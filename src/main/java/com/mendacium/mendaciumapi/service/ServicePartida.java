package com.mendacium.mendaciumapi.service;

import com.mendacium.mendaciumapi.model.Jugador;
import com.mendacium.mendaciumapi.model.MensajeJuego;
import com.mendacium.mendaciumapi.model.Sala;
import com.mendacium.mendaciumapi.repository.RepositoryJugador;
import com.mendacium.mendaciumapi.repository.RepositorySala;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lógica del juego del lado servidor para el modo EN LÍNEA.
 * Es el espejo de MotorResolucion.kt del cliente Android, pero aquí es la fuente de verdad:
 * resuelve la noche y la votación, y notifica a los teléfonos vía WebSocket (STOMP).
 */
@Service
public class ServicePartida {

    // Roles (en texto, sin acentos para evitar problemas de codificación en la red)
    private static final String IMPOSTOR = "IMPOSTOR";
    private static final String MEDICO = "MEDICO";
    private static final String VIDENTE = "VIDENTE";
    private static final String ALDEANO = "ALDEANO";

    // Tipos de acción nocturna
    private static final String ATACAR = "ATACAR";
    private static final String PROTEGER = "PROTEGER";
    private static final String INVESTIGAR = "INVESTIGAR";

    private final RepositorySala repoSala;
    private final RepositoryJugador repoJugador;
    private final SimpMessagingTemplate messaging;
    private final Random random = new Random();

    public ServicePartida(RepositorySala repoSala, RepositoryJugador repoJugador,
                          SimpMessagingTemplate messaging) {
        this.repoSala = repoSala;
        this.repoJugador = repoJugador;
        this.messaging = messaging;
    }

    // ───────────────────────────── Inicio de partida ─────────────────────────────

    @Transactional
    public void asignarRoles(String codigo, int impostorCount, int doctorCount, int seerCount) {
        Sala sala = obtenerSala(codigo);
        List<Jugador> jugadores = repoJugador.findBySalaCodigo(codigo);

        // Construye la bolsa de roles según la configuración y rellena con aldeanos
        List<String> roles = new ArrayList<>();
        for (int i = 0; i < impostorCount; i++) roles.add(IMPOSTOR);
        for (int i = 0; i < doctorCount; i++) roles.add(MEDICO);
        for (int i = 0; i < seerCount; i++) roles.add(VIDENTE);
        while (roles.size() < jugadores.size()) roles.add(ALDEANO);
        // Si pidieron más roles especiales que jugadores, recorta
        roles = new ArrayList<>(roles.subList(0, jugadores.size()));
        Collections.shuffle(roles, random);

        for (int i = 0; i < jugadores.size(); i++) {
            Jugador j = jugadores.get(i);
            j.setRol(roles.get(i));
            j.setVivo(true);
            repoJugador.save(j);
        }

        sala.setFaseActual("NOCHE");
        sala.setNumeroDia(1);
        sala.getAccionesNoche().clear();
        sala.getVotosDia().clear();
        sala.setEstadoSala("EN_JUEGO");
        repoSala.save(sala);

        // Recarga con roles ya asignados
        jugadores = repoJugador.findBySalaCodigo(codigo);
        List<String> nombres = nombresDe(jugadores);
        List<String> impostores = jugadores.stream()
                .filter(j -> IMPOSTOR.equals(j.getRol()))
                .map(Jugador::getNombre)
                .collect(Collectors.toList());

        // Cada jugador recibe SU propio rol (mensaje privado)
        for (Jugador j : jugadores) {
            List<String> aliados = IMPOSTOR.equals(j.getRol())
                    ? impostores.stream().filter(n -> !n.equals(j.getNombre())).collect(Collectors.toList())
                    : List.of();

            Map<String, Object> payload = new HashMap<>();
            payload.put("rol", j.getRol());
            payload.put("jugadores", nombres);
            payload.put("codigosAliados", aliados);
            enviarAJugador(j.getNombre(), new MensajeJuego("PARTIDA_INICIADA", payload));
        }
    }

    // ───────────────────────────── Fase de noche ─────────────────────────────

    @Transactional
    public void registrarAccionNoche(String codigo, String jugador, String tipo, String objetivo) {
        Sala sala = obtenerSala(codigo);
        sala.getAccionesNoche().put(tipo, objetivo);
        repoSala.save(sala);

        enviarAJugador(jugador, new MensajeJuego("ACCION_NOCHE_RECIBIDA", Map.of("ok", true)));

        // La vidente recibe el resultado de su investigación en privado
        if (INVESTIGAR.equals(tipo) && objetivo != null) {
            Optional<Jugador> investigado = repoJugador.findBySalaCodigoAndNombre(codigo, objetivo);
            boolean esMalo = investigado.map(j -> IMPOSTOR.equals(j.getRol())).orElse(false);
            Map<String, Object> p = new HashMap<>();
            p.put("investigado", objetivo);
            p.put("esMalo", esMalo);
            enviarAJugador(jugador, new MensajeJuego("RESULTADO_INVESTIGACION", p));
        }

        if (sala.getAccionesNoche().size() >= accionesEsperadas(codigo)) {
            resolverNoche(codigo);
        }
    }

    @Transactional
    public void resolverNoche(String codigo) {
        Sala sala = obtenerSala(codigo);
        Map<String, String> acciones = sala.getAccionesNoche();

        String victima = acciones.get(ATACAR);
        String protegido = acciones.get(PROTEGER);
        // Hay muerte solo si el impostor atacó y el médico no protegió a esa misma persona
        String eliminado = (victima != null && !victima.equals(protegido)) ? victima : null;

        if (eliminado != null) {
            repoJugador.findBySalaCodigoAndNombre(codigo, eliminado).ifPresent(j -> {
                j.setVivo(false);
                repoJugador.save(j);
            });
        }

        acciones.clear();
        repoSala.save(sala);

        List<String> vivos = nombresVivos(codigo);
        Map<String, Object> payload = new HashMap<>();
        payload.put("eliminado", eliminado);
        payload.put("nadieMurio", eliminado == null);
        payload.put("jugadoresVivos", vivos);
        enviarATopic(codigo, new MensajeJuego("RESULTADO_NOCHE", payload));

        verificarFin(codigo);
    }

    // ───────────────────────────── Avances controlados por el host ─────────────────────────────

    @Transactional
    public void continuarDia(String codigo) {
        Sala sala = obtenerSala(codigo);
        sala.setFaseActual("DIA");
        repoSala.save(sala);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroDia", sala.getNumeroDia());
        payload.put("jugadoresVivos", nombresVivos(codigo));
        enviarATopic(codigo, new MensajeJuego("FASE_DIA", payload));
    }

    @Transactional
    public void iniciarVotacion(String codigo) {
        Sala sala = obtenerSala(codigo);
        sala.setFaseActual("VOTACION");
        sala.getVotosDia().clear();
        repoSala.save(sala);

        Map<String, Object> payload = new HashMap<>();
        payload.put("jugadoresVivos", nombresVivos(codigo));
        enviarATopic(codigo, new MensajeJuego("FASE_VOTACION", payload));
    }

    @Transactional
    public void siguienteNoche(String codigo) {
        Sala sala = obtenerSala(codigo);
        sala.setFaseActual("NOCHE");
        sala.setNumeroDia(sala.getNumeroDia() + 1);
        sala.getAccionesNoche().clear();
        sala.getVotosDia().clear();
        repoSala.save(sala);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroDia", sala.getNumeroDia());
        payload.put("jugadoresVivos", nombresVivos(codigo));
        enviarATopic(codigo, new MensajeJuego("FASE_NOCHE", payload));
    }

    // ───────────────────────────── Fase de día / votación ─────────────────────────────

    @Transactional
    public void registrarVoto(String codigo, String jugador, String votado) {
        Sala sala = obtenerSala(codigo);

        Set<String> vivos = repoJugador.findBySalaCodigo(codigo).stream()
                .filter(Jugador::isVivo)
                .map(Jugador::getNombre)
                .collect(Collectors.toSet());

        // Solo los jugadores vivos cuentan para la votación (los muertos no votan)
        if (!vivos.contains(jugador)) {
            return;
        }

        sala.getVotosDia().put(jugador, votado == null ? "ABSTENCION" : votado);
        repoSala.save(sala);

        // Resuelve cuando TODOS los vivos ya votaron
        long votosDeVivos = sala.getVotosDia().keySet().stream().filter(vivos::contains).count();
        if (votosDeVivos >= vivos.size()) {
            resolverLinchamiento(codigo);
        }
    }

    @Transactional
    public void resolverLinchamiento(String codigo) {
        Sala sala = obtenerSala(codigo);

        // Cuenta los votos (ignorando abstenciones)
        Map<String, Long> conteo = sala.getVotosDia().values().stream()
                .filter(v -> !"ABSTENCION".equals(v))
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        String linchado = elegirMasVotado(conteo);

        boolean eraImpostor = false;
        if (linchado != null) {
            Optional<Jugador> j = repoJugador.findBySalaCodigoAndNombre(codigo, linchado);
            if (j.isPresent()) {
                j.get().setVivo(false);
                repoJugador.save(j.get());
                eraImpostor = IMPOSTOR.equals(j.get().getRol());
            }
        }

        sala.getVotosDia().clear();
        repoSala.save(sala);

        Map<String, Object> payload = new HashMap<>();
        payload.put("linchado", linchado);
        payload.put("eraImpostor", eraImpostor);
        payload.put("jugadoresVivos", nombresVivos(codigo));
        enviarATopic(codigo, new MensajeJuego("RESULTADO_VOTACION", payload));

        verificarFin(codigo);
    }

    // ───────────────────────────── Victoria ─────────────────────────────

    public String evaluarVictoria(String codigo) {
        List<Jugador> vivos = repoJugador.findBySalaCodigo(codigo).stream()
                .filter(Jugador::isVivo)
                .collect(Collectors.toList());
        long impostoresVivos = vivos.stream().filter(j -> IMPOSTOR.equals(j.getRol())).count();
        long buenosVivos = vivos.size() - impostoresVivos;

        if (impostoresVivos == 0) return "BUENOS";
        if (impostoresVivos >= buenosVivos) return "IMPOSTORES";
        return "NINGUNO";
    }

    private void verificarFin(String codigo) {
        String ganador = evaluarVictoria(codigo);
        if (!"NINGUNO".equals(ganador)) {
            Sala sala = obtenerSala(codigo);
            sala.setFaseActual("TERMINADA");
            sala.setEstadoSala("TERMINADA");
            repoSala.save(sala);

            List<Map<String, String>> roles = repoJugador.findBySalaCodigo(codigo).stream()
                    .map(j -> {
                        Map<String, String> m = new HashMap<>();
                        m.put("nombre", j.getNombre());
                        m.put("rol", j.getRol());
                        return m;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> payload = new HashMap<>();
            payload.put("ganador", ganador);
            payload.put("rolesRevelados", roles);
            enviarATopic(codigo, new MensajeJuego("FIN_PARTIDA", payload));
        }
    }

    // ───────────────────────────── Helpers ─────────────────────────────

    private int accionesEsperadas(String codigo) {
        List<Jugador> vivos = repoJugador.findBySalaCodigo(codigo).stream()
                .filter(Jugador::isVivo)
                .collect(Collectors.toList());
        int esperadas = 0;
        if (vivos.stream().anyMatch(j -> IMPOSTOR.equals(j.getRol()))) esperadas++;
        if (vivos.stream().anyMatch(j -> MEDICO.equals(j.getRol()))) esperadas++;
        if (vivos.stream().anyMatch(j -> VIDENTE.equals(j.getRol()))) esperadas++;
        return esperadas;
    }

    private String elegirMasVotado(Map<String, Long> conteo) {
        if (conteo.isEmpty()) return null;
        long max = Collections.max(conteo.values());
        List<String> empatados = conteo.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        // Si hay empate, nadie es linchado
        return empatados.size() == 1 ? empatados.get(0) : null;
    }

    private List<String> nombresDe(List<Jugador> jugadores) {
        return jugadores.stream().map(Jugador::getNombre).collect(Collectors.toList());
    }

    private List<String> nombresVivos(String codigo) {
        return repoJugador.findBySalaCodigo(codigo).stream()
                .filter(Jugador::isVivo)
                .map(Jugador::getNombre)
                .collect(Collectors.toList());
    }

    private Sala obtenerSala(String codigo) {
        return repoSala.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("No existe la sala con el código: " + codigo));
    }

    private void enviarATopic(String codigo, MensajeJuego mensaje) {
        messaging.convertAndSend("/topic/sala/" + codigo, mensaje);
    }

    private void enviarAJugador(String nombre, MensajeJuego mensaje) {
        messaging.convertAndSend("/queue/jugador/" + nombre, mensaje);
    }
}
