package com.codice.sra.repositories;

import com.codice.sra.models.TipoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoEvaluacionRepository extends JpaRepository<TipoEvaluacion, Long> {
    Optional<TipoEvaluacion> findByTipoEvaluacionIgnoreCase(String tipoEvaluacion);
}
