package com.codice.sra.repositories;

import com.codice.sra.models.EstadoAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EstadoAsistenciaRepository extends JpaRepository<EstadoAsistencia, Long> {
    Optional<EstadoAsistencia> findByEstadoAsistencia(String estadoAsistencia);
}