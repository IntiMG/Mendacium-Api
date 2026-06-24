package com.mendacium.mendaciumapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sala")
public class Sala extends BaseEntity {

    @NotEmpty(message = "El código no puede estar vacío")
    @Column(unique = true, nullable = false, length = 5)
    private String codigo;

    @NotEmpty(message = "El estado no puede estar vacío")
    private String estadoSala;

    @OneToMany(mappedBy = "sala", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Jugador> jugadores = new ArrayList<>();

    public Sala() {}

    public Sala(String codigo) {
        this.codigo = codigo;
        this.estadoSala = "ESPERANDO";
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getEstadoSala() {
        return estadoSala;
    }

    public void setEstadoSala(String estadoSala) {
        this.estadoSala = estadoSala;
    }

    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<Jugador> jugadores) {
        this.jugadores = jugadores;
    }
}
