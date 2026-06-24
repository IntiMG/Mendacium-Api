package com.mendacium.mendaciumapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name = "jugador")
public class Jugador extends BaseEntity {

    @NotEmpty(message = "El nombre no puede estar vacío")
    private String nombre;

    private boolean esHost;
    private String rol;
    private boolean vivo = true;

    @ManyToOne
    @JoinColumn(name = "sala_id", nullable = false)
    @JsonIgnore
    private Sala sala;

    public Jugador() {}

    public Jugador(String nombre, boolean esHost, Sala sala) {
        this.nombre = nombre;
        this.esHost = esHost;
        this.sala = sala;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isEsHost() {
        return esHost;
    }

    public void setEsHost(boolean esHost) {
        this.esHost = esHost;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isVivo() {
        return vivo;
    }

    public void setVivo(boolean vivo) {
        this.vivo = vivo;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }
}
