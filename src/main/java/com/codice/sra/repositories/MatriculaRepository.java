package com.codice.sra.repositories;

import com.codice.sra.models.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    Optional<Matricula> findByEstudianteIdEstudianteAndCicloIdCiclo(Long idEstudiante, Long idCiclo);
    Optional<Matricula> findFirstByEstudiante_IdEstudianteOrderByFechaMatriculaDesc(Long idEstudiante);
}