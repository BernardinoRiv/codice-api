package com.codice.sra.repositories;

import com.codice.sra.models.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    //Valida si un DOCENTE ya tiene asignada una clase concurrente en el ciclo, día y franja.
    @Query("""
        SELECT COUNT(h) > 0 FROM Horario h
        JOIN h.grupo g
        WHERE g.ciclo.idCiclo = :idCiclo
          AND g.docente.idDocente = :idDocente
          AND h.dia.idDia = :idDia
          AND UPPER(TRIM(g.estadoGrupo.estadoGrupo)) != 'CANCELADO'
          AND (:horaInicio < h.horaFin AND :horaFin > h.horaInicio)
    """)
    boolean existeTraslapeDocente(@Param("idCiclo") Long idCiclo,
                                  @Param("idDocente") Long idDocente,
                                  @Param("idDia") Long idDia,
                                  @Param("horaInicio") LocalTime horaInicio,
                                  @Param("horaFin") LocalTime horaFin);

    //Valida si un AULA FÍSICA ya está ocupada por otra sección en el ciclo, día y franja.
    @Query("""
        SELECT COUNT(h) > 0 FROM Horario h
        JOIN h.grupo g
        WHERE g.ciclo.idCiclo = :idCiclo
          AND h.aula.idAula = :idAula
          AND h.dia.idDia = :idDia
          AND UPPER(TRIM(g.estadoGrupo.estadoGrupo)) != 'CANCELADO'
          AND (:horaInicio < h.horaFin AND :horaFin > h.horaInicio)
    """)
    boolean existeTraslapeAula(@Param("idCiclo") Long idCiclo,
                               @Param("idAula") Long idAula,
                               @Param("idDia") Long idDia,
                               @Param("horaInicio") LocalTime horaInicio,
                               @Param("horaFin") LocalTime horaFin);

    //Valida si el docente colisiona con OTRO grupo, excluyendo el grupo en edición.
    @Query("""
        SELECT COUNT(h) > 0 FROM Horario h
        JOIN h.grupo g
        WHERE g.ciclo.idCiclo = :idCiclo
          AND g.docente.idDocente = :idDocente
          AND h.dia.idDia = :idDia
          AND g.idGrupo <> :idGrupoActual
          AND UPPER(TRIM(g.estadoGrupo.estadoGrupo)) != 'CANCELADO'
          AND (:horaInicio < h.horaFin AND :horaFin > h.horaInicio)
    """)
    boolean existeTraslapeDocenteEnOtroGrupo(@Param("idCiclo") Long idCiclo,
                                             @Param("idDocente") Long idDocente,
                                             @Param("idDia") Long idDia,
                                             @Param("idGrupoActual") Long idGrupoActual,
                                             @Param("horaInicio") LocalTime horaInicio,
                                             @Param("horaFin") LocalTime horaFin);

    //Valida si el aula colisiona con OTRO grupo, excluyendo el grupo en edición.
    @Query("""
        SELECT COUNT(h) > 0 FROM Horario h
        JOIN h.grupo g
        WHERE g.ciclo.idCiclo = :idCiclo
          AND h.aula.idAula = :idAula
          AND h.dia.idDia = :idDia
          AND g.idGrupo <> :idGrupoActual
          AND UPPER(TRIM(g.estadoGrupo.estadoGrupo)) != 'CANCELADO'
          AND (:horaInicio < h.horaFin AND :horaFin > h.horaInicio)
    """)
    boolean existeTraslapeAulaEnOtroGrupo(@Param("idCiclo") Long idCiclo,
                                          @Param("idAula") Long idAula,
                                          @Param("idDia") Long idDia,
                                          @Param("idGrupoActual") Long idGrupoActual,
                                          @Param("horaInicio") LocalTime horaInicio,
                                          @Param("horaFin") LocalTime horaFin);


    //Recupera todas las franjas horarias vinculadas a una sección específica.
    @Query("SELECT h FROM Horario h WHERE h.grupo.idGrupo = :idGrupo")
    List<Horario> findByGrupoIdGrupo(@Param("idGrupo") Long idGrupo);

    //Purga física de franjas horarias para operaciones en cascada.
    @Modifying
    @Query("DELETE FROM Horario h WHERE h.grupo.idGrupo = :idGrupo")
    void deleteByGrupo_IdGrupo(@Param("idGrupo") Long idGrupo);

    @Modifying
    @Query("DELETE FROM Horario h WHERE h.grupo.idGrupo IN :idsGrupos")
    void deleteByGrupo_IdGrupoIn(@Param("idsGrupos") List<Long> idsGrupos);
}