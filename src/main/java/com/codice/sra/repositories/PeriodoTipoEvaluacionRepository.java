package com.codice.sra.repositories;

import com.codice.sra.models.PeriodoTipoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PeriodoTipoEvaluacionRepository extends JpaRepository<PeriodoTipoEvaluacion, Long> {
    List<PeriodoTipoEvaluacion> findByPeriodoEvaluacionIdPeriodoEvaluacion(Long idPeriodoEvaluacion);
}