package com.codice.sra.repositories;

import com.codice.sra.models.EstadoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoInscripcionRepository extends JpaRepository<EstadoInscripcion, Long> {
    Optional<EstadoInscripcion> findByEstadoInscripcion(String estadoInscripcion);
}