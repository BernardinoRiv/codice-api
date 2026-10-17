package com.codice.sra.repositories;

import com.codice.sra.models.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultadRepository extends JpaRepository<Facultad, Long> {

    Optional<Facultad> findByCodigoFacultadIgnoreCase(String codigoFacultad);

    Optional<Facultad> findByNombreFacultadIgnoreCase(String nombreFacultad);

    boolean existsByCodigoFacultadIgnoreCase(String codigoFacultad);

    boolean existsByNombreFacultadIgnoreCase(String nombreFacultad);

    // Consulta optimizada para cargar la facultad junto con su estado evitando N+1
    @Query("SELECT f FROM Facultad f JOIN FETCH f.estadoFacultad WHERE f.idFacultad = :idFacultad")
    Optional<Facultad> findByIdConEstado(@Param("idFacultad") Long idFacultad);

    // Listar facultades activas (id_estado_facultad = 1)
    @Query("SELECT f FROM Facultad f JOIN FETCH f.estadoFacultad ef WHERE ef.idEstadoFacultad = 1 ORDER BY f.nombreFacultad ASC")
    List<Facultad> findAllActivas();
}