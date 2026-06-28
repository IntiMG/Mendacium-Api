package com.mendacium.mendaciumapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "sala")
public class Sala extends BaseEntity {

    @NotEmpty(message = "El código no puede estar vacío")
    @Column(unique = true, nullable = false, length = 5)
    private String codigo;

    @NotEmpty(message = "El estado no puede estar vacío")
    private String estadoSala;

    // Fase de la partida en línea: LOBBY | NOCHE | DIA | VOTACION | TERMINADA
    private String faseActual = "LOBBY";

    private int numeroDia = 0;

    // Acciones nocturnas acumuladas. key = "ATACAR"|"PROTEGER"|"INVESTIGAR", value = nombre objetivo
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "acciones_noche")
    private Map<String, String> accionesNoche = new HashMap<>();

    // Votos del día. key = nombre del votante, value = nombre votado (o "ABSTENCION")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "votos_dia")
    private Map<String, String> votosDia = new HashMap<>();

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

    public String getFaseActual() {
        return faseActual;
    }

    public void setFaseActual(String faseActual) {
        this.faseActual = faseActual;
    }

    public int getNumeroDia() {
        return numeroDia;
    }

    public void setNumeroDia(int numeroDia) {
        this.numeroDia = numeroDia;
    }

    public Map<String, String> getAccionesNoche() {
        return accionesNoche;
    }

    public void setAccionesNoche(Map<String, String> accionesNoche) {
        this.accionesNoche = accionesNoche;
    }

    public Map<String, String> getVotosDia() {
        return votosDia;
    }

    public void setVotosDia(Map<String, String> votosDia) {
        this.votosDia = votosDia;
    }

    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<Jugador> jugadores) {
        this.jugadores = jugadores;
    }
}
