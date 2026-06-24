package com.mendacium.mendaciumapi.repository;

import com.mendacium.mendaciumapi.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepositoryJugador extends JpaRepository<Jugador, UUID> {
}
