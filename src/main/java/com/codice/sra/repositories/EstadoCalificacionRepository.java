package com.codice.sra.repositories;

import com.codice.sra.models.EstadoCalificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCalificacionRepository extends JpaRepository<EstadoCalificacion, Long> {

    Optional<EstadoCalificacion> findByEstadoCalificacion(String estadoCalificacion);
}