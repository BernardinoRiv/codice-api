package com.codice.sra.repositories;

import com.codice.sra.dtos.ResumenCarreraOfertaDTO;
import com.codice.sra.models.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findByDocenteIdDocente(Long idDocente);

    // Validar si ya existe ese código de grupo para la misma materia en el ciclo y sede
    boolean existsByCiclo_IdCicloAndMateria_IdMateriaAndSede_IdSedeAndCodigoGrupoIgnoreCase(
            Long idCiclo, Long idMateria, Long idSede, String codigoGrupo
    );

    // Contar cuántas materias/grupos activos tiene asignados el docente en el ciclo (para validar maximo_materias)
    @Query("SELECT COUNT(g) FROM Grupo g " +
            "WHERE g.docente.idDocente = :idDocente " +
            "AND g.ciclo.idCiclo = :idCiclo " +
            "AND g.estadoGrupo.estadoGrupo != 'CANCELADO'")
    long countGruposActivosPorDocenteYCiclo(@Param("idDocente") Long idDocente,
                                            @Param("idCiclo") Long idCiclo);

    // Obtener la oferta académica configurada para un ciclo y sede específicos
    @Query("SELECT g FROM Grupo g " +
            "JOIN FETCH g.materia m " +
            "JOIN FETCH g.docente d " +
            "JOIN FETCH d.persona p " +
            "JOIN FETCH g.estadoGrupo eg " +
            "WHERE g.ciclo.idCiclo = :idCiclo " +
            "AND g.sede.idSede = :idSede")
    List<Grupo> findOfertaPorCicloYSede(@Param("idCiclo") Long idCiclo,
                                        @Param("idSede") Long idSede);


    Optional<Grupo> findById(Long id);
    List<Grupo> findByDocenteIdDocenteAndCicloIdCiclo(Long idDocente, Long idCiclo);

    // Verifica si existen secciones asociadas al ciclo
    boolean existsByCiclo_IdCiclo(Long idCiclo);

    // Consulta los grupos de un ciclo específico para gestión o limpieza
    List<Grupo> findByCiclo_IdCiclo(Long idCiclo);

    @Query("""
        SELECT new com.codice.sra.dtos.ResumenCarreraOfertaDTO(
            c.idCarrera,
            c.codigoCarrera,
            c.nombreCarrera,
            COUNT(DISTINCT g.idGrupo)
        )
        FROM Grupo g
        JOIN g.materia m
        JOIN PensumMateria pm ON pm.materia.idMateria = m.idMateria
        JOIN pm.pensum p
        JOIN p.estadoPensum ep
        JOIN p.carrera c
        JOIN CarreraSede cs ON cs.carrera.idCarrera = c.idCarrera AND cs.sede.idSede = g.sede.idSede
        WHERE g.ciclo.idCiclo = :idCiclo
          AND UPPER(TRIM(ep.estadoPensum)) = 'VIGENTE'
        GROUP BY c.idCarrera, c.codigoCarrera, c.nombreCarrera
        ORDER BY c.nombreCarrera ASC
    """)
    List<ResumenCarreraOfertaDTO> contarSeccionesPorCarreraEnCiclo(@Param("idCiclo") Long idCiclo);

    @Query("""
        SELECT COUNT(g) FROM Grupo g
        WHERE g.docente.idDocente = :idDocente
          AND g.ciclo.idCiclo = :idCiclo
    """)
    long countByDocenteIdDocenteAndCicloIdCiclo(@Param("idDocente") Long idDocente, @Param("idCiclo") Long idCiclo);


    // Obtener únicamente los IDs para no cargar entidades pesadas a la memoria de Hibernate
    @Query("SELECT g.idGrupo FROM Grupo g WHERE g.ciclo.idCiclo = :idCiclo")
    List<Long> findIdsByCiclo_IdCiclo(@Param("idCiclo") Long idCiclo);

    // Borrado directo en BD sin ensuciar el contexto de persistencia
    @Modifying
    @Query("DELETE FROM Grupo g WHERE g.ciclo.idCiclo = :idCiclo")
    void deleteByCiclo_IdCiclo(@Param("idCiclo") Long idCiclo);
}