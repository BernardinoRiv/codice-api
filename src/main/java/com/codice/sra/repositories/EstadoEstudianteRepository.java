package com.codice.sra.repositories;

import com.codice.sra.models.EstadoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoEstudianteRepository extends JpaRepository<EstadoEstudiante, Long> {
}