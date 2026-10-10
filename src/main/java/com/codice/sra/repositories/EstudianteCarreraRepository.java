package com.codice.sra.repositories;

import com.codice.sra.models.EstudianteCarrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstudianteCarreraRepository extends JpaRepository<EstudianteCarrera, Long> {

    @Query("SELECT c.nombreCarrera FROM EstudianteCarrera ec " +
            "JOIN ec.carreraSede cs " +
            "JOIN cs.carrera c " +
            "WHERE ec.estudiante.idEstudiante = :idEstudiante")
    Optional<String> findNombreCarreraByEstudianteId(@Param("idEstudiante") Long idEstudiante);
}