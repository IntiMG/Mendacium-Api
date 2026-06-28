package com.mendacium.mendaciumapi.service;

import com.mendacium.mendaciumapi.model.Jugador;
import com.mendacium.mendaciumapi.model.MensajeJuego;
import com.mendacium.mendaciumapi.model.Sala;
import com.mendacium.mendaciumapi.repository.RepositoryJugador;
import com.mendacium.mendaciumapi.repository.RepositorySala;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ServiceSala {

    private final RepositorySala repo;
    private final RepositoryJugador repoJugador;
    private final SimpMessagingTemplate messaging;
    private final Random random = new Random();

    public ServiceSala(RepositorySala repo, RepositoryJugador repoJugador,
                       SimpMessagingTemplate messaging) {
        this.repo = repo;
        this.repoJugador = repoJugador;
        this.messaging = messaging;
    }

    public List<Sala> findAll() {
        return repo.findAll();
    }

    public Sala findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe la sala con el id: " + id));
    }

    public Sala findByCodigo(String codigo) {
        return repo.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("No existe la sala con el código: " + codigo));
    }

    public Sala crearSala(String hostNombre) {
        String codigo = generarCodigoUnico();
        Sala sala = new Sala(codigo);
        repo.save(sala);

        Jugador host = new Jugador(hostNombre, true, sala);
        repoJugador.save(host);
        sala.getJugadores().add(host);

        return sala;
    }

    public Jugador unirseASala(String codigo, String nombre) {
        Sala sala = repo.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("No existe la sala con el código: " + codigo));

        if (!sala.getEstadoSala().equals("ESPERANDO")) {
            throw new RuntimeException("La sala ya no acepta jugadores");
        }

        Jugador jugador = new Jugador(nombre, false, sala);
        Jugador guardado = repoJugador.save(jugador);

        // Avisa por WebSocket a todos los que estén en el lobby de esta sala.
        // El cliente reacciona refrescando la lista (vía REST), así no dependemos
        // de construir la lista completa aquí.
        messaging.convertAndSend("/topic/sala/" + codigo,
                new MensajeJuego("JUGADOR_UNIDO", Map.of("nombre", nombre)));

        return guardado;
    }

    private String generarCodigoUnico() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String codigo;
        do {
            codigo = IntStream.range(0, 5)
                    .mapToObj(i -> String.valueOf(chars.charAt(random.nextInt(chars.length()))))
                    .collect(Collectors.joining());
        } while (repo.existsByCodigo(codigo));
        return codigo;
    }
}
