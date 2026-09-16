package com.codice.sra.repositories;

import com.codice.sra.models.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    @Query("SELECT e FROM Evaluacion e " +
            "JOIN FETCH e.tipoEvaluacion te " +
            "LEFT JOIN FETCH e.periodoEvaluacion pe " +
            "WHERE e.grupo.idGrupo = :idGrupo " +
            "ORDER BY pe.periodo, te.tipoEvaluacion, e.numeroEvaluacion")
    List<Evaluacion> findByGrupoIdGrupo(@Param("idGrupo") Long idGrupo);
}