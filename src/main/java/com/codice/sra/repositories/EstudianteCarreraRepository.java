package com.codice.sra.repositories;

import com.codice.sra.models.EstudianteCarrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteCarreraRepository extends JpaRepository<EstudianteCarrera, Long> {

    @Query("SELECT c.nombreCarrera FROM EstudianteCarrera ec " +
            "JOIN ec.carreraSede cs " +
            "JOIN cs.carrera c " +
            "WHERE ec.estudiante.idEstudiante = :idEstudiante")
    Optional<String> findNombreCarreraByEstudianteId(@Param("idEstudiante") Long idEstudiante);

    Optional<EstudianteCarrera> findFirstByEstudiante_IdEstudianteOrderByFechaInicioDesc(Long idEstudiante);

    // Firma requerida para desvincular la trayectoria durante la purga o cancelación
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM EstudianteCarrera ec WHERE ec.estudiante.idEstudiante = :idEstudiante")
    void deleteByEstudiante_IdEstudiante(@Param("idEstudiante") Long idEstudiante);



    // Método requerido para recuperar o asociar la carrera-sede activa del estudiante
    Optional<EstudianteCarrera> findByEstudiante_IdEstudianteAndCarreraSede_IdCarreraSede(
            Long idEstudiante,
            Long idCarreraSede
    );

    @Query("""
    SELECT ec FROM EstudianteCarrera ec
    JOIN FETCH ec.carreraSede cs
    JOIN FETCH cs.carrera
    JOIN FETCH cs.sede
    JOIN FETCH ec.estadoTrayectoria
    JOIN FETCH ec.pensum
    ORDER BY ec.estudiante.idEstudiante, ec.fechaInicio DESC
    """)
    List<EstudianteCarrera> findAllConCarreraSedeYEstado();

    @Query("""
        SELECT ec FROM EstudianteCarrera ec
        JOIN FETCH ec.pensum
        WHERE ec.estudiante.idEstudiante = :idEstudiante
        """)
    List<EstudianteCarrera> findByEstudianteIdConPensum(@Param("idEstudiante") Long idEstudiante);
}