package com.codice.sra.repositories;

import com.codice.sra.models.PeriodoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PeriodoEvaluacionRepository extends JpaRepository<PeriodoEvaluacion, Long> {
    List<PeriodoEvaluacion> findByCicloIdCiclo(Long idCiclo);
}