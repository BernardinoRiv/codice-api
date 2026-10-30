package com.codice.sra.repositories;

import com.codice.sra.models.EstadoMateriaEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoMateriaEstudianteRepository extends JpaRepository<EstadoMateriaEstudiante, Long> {
    Optional<EstadoMateriaEstudiante> findByEstadoMateria(String estadoMateria);
}