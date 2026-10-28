package com.codice.sra.repositories;

import com.codice.sra.models.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    @Query("SELECT i FROM Inscripcion i " +
            "JOIN FETCH i.matricula m " +
            "JOIN FETCH m.estudiante e " +
            "JOIN FETCH e.persona p " +
            "WHERE i.grupo.idGrupo = :idGrupo " +
            "ORDER BY p.nombres, p.apellidos")
    List<Inscripcion> findByGrupoIdGrupo(@Param("idGrupo") Long idGrupo);

    @Query("SELECT i FROM Inscripcion i " +
            "JOIN i.matricula m " +
            "JOIN m.estudiante e " +
            "JOIN i.grupo g " +
            "WHERE e.idEstudiante = :idEstudiante " +
            "AND g.ciclo.idCiclo = :idCiclo")
    List<Inscripcion> findByEstudianteAndCiclo(
            @Param("idEstudiante") Long idEstudiante,
            @Param("idCiclo") Long idCiclo
    );

    @Query("SELECT i FROM Inscripcion i JOIN i.matricula m JOIN m.estudiante e WHERE e.carnet = :carnet AND i.grupo.idGrupo = :idGrupo")
    Optional<Inscripcion> findByCarnetAndGrupo(@Param("carnet") String carnet, @Param("idGrupo") Long idGrupo);

    @Query("SELECT i FROM Inscripcion i " +
            "JOIN FETCH i.matricula m " +
            "JOIN FETCH m.estudiante e " +
            "JOIN FETCH i.grupo g " +
            "JOIN FETCH g.materia ma " +
            "JOIN FETCH g.ciclo c " +
            "WHERE e.idEstudiante = :idEstudiante")
    List<Inscripcion> findAllByEstudianteId(@Param("idEstudiante") Long idEstudiante);

    long countByGrupoIdGrupo(Long idGrupo);

    boolean existsByGrupoIdGrupo(Long idGrupo);

    // 1. Contar cuántas materias tiene inscritas en el ciclo activo
    @Query("SELECT COUNT(i) FROM Inscripcion i WHERE i.matricula.estudiante.carnet = :carnet AND i.matricula.ciclo.estadoCiclo.estadoCiclo = 'ACTIVO'")
    int countMateriasInscritasEnCicloActivo(@Param("carnet") String carnet);

    // Cuenta cuántos inscritos reales tiene un grupo para calcular el cupo disponible
    @Query("SELECT COUNT(i) FROM Inscripcion i WHERE i.grupo.idGrupo = :idGrupo AND i.estadoInscripcion.estadoInscripcion IN ('INSCRITA', 'RESERVADA')")
    int countInscritosPorGrupo(@Param("idGrupo") Long idGrupo);

    @Query("SELECT COUNT(i) FROM Inscripcion i " +
            "WHERE i.grupo.idGrupo = :idGrupo " +
            "AND UPPER(i.estadoInscripcion.estadoInscripcion) NOT IN ('RETIRADO', 'CANCELADO', 'REPROBADO')")
    int countInscripcionesActivasPorGrupo(@Param("idGrupo") Long idGrupo);


    // Conteo de inscritos para verificar contra grupo.cupoMaximo
    long countByGrupo_IdGrupo(Long idGrupo);

    List<Inscripcion> findByMatricula_IdMatricula(Long idMatricula);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Inscripcion i WHERE i.matricula.idMatricula = :idMatricula")
    void deleteByMatriculaId(@Param("idMatricula") Long idMatricula);
}