package com.codice.sra.repositories;

import com.codice.sra.models.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    /**
     * Valida si un DOCENTE ya tiene asignada una clase que se traslape en el mismo ciclo, día y rango horario.
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Horario h " +
            "JOIN h.grupo g " +
            "WHERE g.ciclo.idCiclo = :idCiclo " +
            "AND g.docente.idDocente = :idDocente " +
            "AND h.dia.idDia = :idDia " +
            "AND g.estadoGrupo.estadoGrupo != 'CANCELADO' " +
            "AND (:horaInicio < h.horaFin AND :horaFin > h.horaInicio)")
    boolean existeTraslapeDocente(@Param("idCiclo") Long idCiclo,
                                  @Param("idDocente") Long idDocente,
                                  @Param("idDia") Long idDia,
                                  @Param("horaInicio") LocalTime horaInicio,
                                  @Param("horaFin") LocalTime horaFin);

    /**
     * Valida si un AULA FÍSICA ya está ocupada por otra sección en el mismo ciclo, día y rango horario.
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Horario h " +
            "JOIN h.grupo g " +
            "WHERE g.ciclo.idCiclo = :idCiclo " +
            "AND h.aula.idAula = :idAula " +
            "AND h.dia.idDia = :idDia " +
            "AND g.estadoGrupo.estadoGrupo != 'CANCELADO' " +
            "AND (:horaInicio < h.horaFin AND :horaFin > h.horaInicio)")
    boolean existeTraslapeAula(@Param("idCiclo") Long idCiclo,
                               @Param("idAula") Long idAula,
                               @Param("idDia") Long idDia,
                               @Param("horaInicio") LocalTime horaInicio,
                               @Param("horaFin") LocalTime horaFin);

    // Recuperar todos los bloques de horario asignados a un grupo
    List<Horario> findByGrupo_IdGrupo(Long idGrupo);
}