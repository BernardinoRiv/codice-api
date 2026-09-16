package com.codice.sra.repositories;

import com.codice.sra.models.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    @Query("SELECT i FROM Inscripcion i " +
            "JOIN FETCH i.matricula m " +
            "JOIN FETCH m.estudiante e " +
            "JOIN FETCH e.persona p " +
            "WHERE i.grupo.idGrupo = :idGrupo " +
            "ORDER BY p.nombres, p.apellidos")
    List<Inscripcion> findByGrupoIdGrupo(@Param("idGrupo") Long idGrupo);
}