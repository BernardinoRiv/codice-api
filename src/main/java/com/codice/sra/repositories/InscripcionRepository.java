package com.codice.sra.repositories;

import com.codice.sra.models.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
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


}