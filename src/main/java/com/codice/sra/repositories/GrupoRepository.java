package com.codice.sra.repositories;

import com.codice.sra.models.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}