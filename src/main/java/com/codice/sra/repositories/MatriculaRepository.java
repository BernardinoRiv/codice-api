package com.codice.sra.repositories;

import com.codice.sra.models.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    Optional<Matricula> findByEstudianteIdEstudianteAndCicloIdCiclo(Long idEstudiante, Long idCiclo);
    Optional<Matricula> findFirstByEstudiante_IdEstudianteOrderByFechaMatriculaDesc(Long idEstudiante);

    // Firma requerida para obtener las matrículas asociadas al expediente
    List<Matricula> findByEstudiante_IdEstudiante(Long idEstudiante);


    // Método requerido para control de idempotencia y prevención de doble matrícula en el ciclo
    boolean existsByEstudiante_IdEstudianteAndCiclo_IdCiclo(Long idEstudiante, Long idCiclo);

}