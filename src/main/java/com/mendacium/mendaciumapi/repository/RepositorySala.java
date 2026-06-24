package com.mendacium.mendaciumapi.repository;

import com.mendacium.mendaciumapi.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorySala extends JpaRepository<Sala, UUID> {
    Optional<Sala> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
