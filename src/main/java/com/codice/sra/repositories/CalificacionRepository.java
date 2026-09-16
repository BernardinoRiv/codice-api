package com.codice.sra.repositories;

import com.codice.sra.models.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    // Este método funciona correctamente - usa derivación de nombres de Spring Data
    Optional<Calificacion> findByInscripcionIdInscripcionAndEvaluacionIdEvaluacion(
            Long idInscripcion,
            Long idEvaluacion
    );

    // Necesario para el servicio de EvaluacionService
    List<Calificacion> findByEvaluacionIdEvaluacion(Long idEvaluacion);

    // Consulta personalizada con JOIN
    @Query("SELECT c FROM Calificacion c WHERE c.evaluacion.grupo.idGrupo = :idGrupo")
    List<Calificacion> findByEvaluacionGrupoIdGrupo(@Param("idGrupo") Long idGrupo);
}