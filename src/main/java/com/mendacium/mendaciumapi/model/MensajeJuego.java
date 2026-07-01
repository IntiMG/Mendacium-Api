package com.mendacium.mendaciumapi.model;

// Envoltorio de todos los mensajes que viajan por WebSocket.
// tipo  -> qué clase de evento es ("JUGADOR_UNIDO", "PARTIDA_INICIADA", ...)
// payload -> el objeto con los datos
public class MensajeJuego {

    private String tipo;
    private Object payload;

    public MensajeJuego() {}

    public MensajeJuego(String tipo, Object payload) {
        this.tipo = tipo;
        this.payload = payload;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
