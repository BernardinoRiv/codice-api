package com.codice.sra.repositories;

import com.codice.sra.models.EstadoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoEstudianteRepository extends JpaRepository<EstadoEstudiante, Long> {
    Optional<EstadoEstudiante> findByEstadoEstudiante(String estadoEstudiante);
}