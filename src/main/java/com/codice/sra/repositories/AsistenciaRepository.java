package com.codice.sra.repositories;

import com.codice.sra.models.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    // Para que el docente vea la lista de una clase específica
    @Query("SELECT a FROM Asistencia a " +
            "JOIN FETCH a.clase c " +
            "JOIN FETCH a.inscripcion i " +
            "JOIN FETCH i.matricula m " +
            "JOIN FETCH m.estudiante e " +
            "JOIN FETCH e.persona p " +
            "JOIN FETCH a.estadoAsistencia " +
            "WHERE c.idClase = :idClase")
    List<Asistencia> findByClaseIdClase(@Param("idClase") Long idClase);

    // Para que el estudiante vea su historial de asistencias en un grupo específico
    @Query("SELECT a FROM Asistencia a " +
            "JOIN FETCH a.clase c " +
            "JOIN FETCH c.grupo g " +
            "JOIN FETCH a.inscripcion i " +
            "JOIN FETCH i.matricula m " +
            "JOIN FETCH m.estudiante e " +
            "JOIN FETCH e.usuario u " +
            "JOIN FETCH a.estadoAsistencia " +
            "WHERE u.idUsuario = :idUsuario " +
            "AND g.idGrupo = :idGrupo")
    List<Asistencia> findByEstudianteAndGrupo(@Param("idUsuario") Long idUsuario, @Param("idGrupo") Long idGrupo);

    @Query("SELECT a FROM Asistencia a " +
            "JOIN FETCH a.clase c " +
            "JOIN FETCH c.grupo g " +
            "JOIN FETCH a.inscripcion i " +
            "JOIN FETCH i.matricula m " +
            "JOIN FETCH m.estudiante e " +
            "JOIN FETCH e.persona p " +
            "JOIN FETCH a.estadoAsistencia " +
            "WHERE g.idGrupo = :idGrupo " +
            "ORDER BY c.fechaClase DESC, p.nombres")
    List<Asistencia> findByGrupoIdGrupo(@Param("idGrupo") Long idGrupo);

    @Query("SELECT a FROM Asistencia a " +
            "JOIN FETCH a.clase c " +
            "JOIN FETCH c.grupo g " +
            "JOIN FETCH a.inscripcion i " +
            "JOIN FETCH i.matricula m " +
            "JOIN FETCH m.estudiante e " +
            "JOIN FETCH a.estadoAsistencia " +
            "WHERE g.idGrupo = :idGrupo " +
            "AND e.idEstudiante = :idEstudiante " +
            "ORDER BY c.fechaClase DESC")
    List<Asistencia> findByGrupoAndEstudiante(@Param("idGrupo") Long idGrupo, @Param("idEstudiante") Long idEstudiante);
    Optional<Asistencia> findByClaseIdClaseAndInscripcionIdInscripcion(Long idClase, Long idInscripcion);

    @Query("SELECT a FROM Asistencia a " +
            "JOIN FETCH a.clase c " +
            "JOIN FETCH c.grupo g " +
            "JOIN FETCH a.inscripcion i " +
            "JOIN FETCH i.matricula m " +
            "JOIN FETCH m.estudiante e " +
            "JOIN FETCH e.persona p " +
            "JOIN FETCH a.estadoAsistencia " +
            "WHERE g.idGrupo = :idGrupo " +
            "AND c.fechaClase = :fecha " +
            "ORDER BY p.nombres")
    List<Asistencia> findByGrupoIdGrupoAndFecha(@Param("idGrupo") Long idGrupo, @Param("fecha") LocalDate fecha);
}