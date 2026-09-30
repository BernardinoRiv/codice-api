package com.codice.sra.repositories;

import com.codice.sra.models.CargoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CargoEstudianteRepository extends JpaRepository<CargoEstudiante, Long> {
    List<CargoEstudiante> findByMatriculaIdMatricula(Long idMatricula);
}