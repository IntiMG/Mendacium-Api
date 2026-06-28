package com.mendacium.mendaciumapi.controller;

import com.mendacium.mendaciumapi.service.ServicePartida;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Recibe los mensajes STOMP que envían los teléfonos.
 * Los clientes envían a destinos /app/sala/{codigo}/...
 */
@Controller
public class ControllerPartida {

    private final ServicePartida service;

    public ControllerPartida(ServicePartida service) {
        this.service = service;
    }

    // El host inicia la partida con la configuración de roles
    @MessageMapping("/sala/{codigo}/iniciar")
    public void iniciar(@DestinationVariable String codigo, @Payload Map<String, Object> body) {
        service.asignarRoles(
                codigo,
                asInt(body.get("impostorCount")),
                asInt(body.get("doctorCount")),
                asInt(body.get("seerCount"))
        );
    }

    // Un rol nocturno envía su acción: { jugador, tipo: ATACAR|PROTEGER|INVESTIGAR, objetivo }
    @MessageMapping("/sala/{codigo}/accion")
    public void accion(@DestinationVariable String codigo, @Payload Map<String, Object> body) {
        service.registrarAccionNoche(
                codigo,
                String.valueOf(body.get("jugador")),
                String.valueOf(body.get("tipo")),
                body.get("objetivo") == null ? null : String.valueOf(body.get("objetivo"))
        );
    }

    // Host: del resumen de la noche a la discusión
    @MessageMapping("/sala/{codigo}/continuarDia")
    public void continuarDia(@DestinationVariable String codigo) {
        service.continuarDia(codigo);
    }

    // Host: de la discusión a la votación
    @MessageMapping("/sala/{codigo}/iniciarVotacion")
    public void iniciarVotacion(@DestinationVariable String codigo) {
        service.iniciarVotacion(codigo);
    }

    // Un jugador vota: { jugador, votado } (votado nulo = abstención)
    @MessageMapping("/sala/{codigo}/votar")
    public void votar(@DestinationVariable String codigo, @Payload Map<String, Object> body) {
        service.registrarVoto(
                codigo,
                String.valueOf(body.get("jugador")),
                body.get("votado") == null ? null : String.valueOf(body.get("votado"))
        );
    }

    // Host: del veredicto a la siguiente noche
    @MessageMapping("/sala/{codigo}/siguienteNoche")
    public void siguienteNoche(@DestinationVariable String codigo) {
        service.siguienteNoche(codigo);
    }

    private int asInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        return Integer.parseInt(String.valueOf(o));
    }
}
