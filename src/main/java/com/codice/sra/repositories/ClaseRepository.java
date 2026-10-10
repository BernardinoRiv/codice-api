package com.codice.sra.repositories;

import com.codice.sra.models.Clase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, Long> {

    @Query("SELECT c FROM Clase c WHERE c.grupo.idGrupo = :idGrupo AND c.fechaClase = :fecha")
    Optional<Clase> findByGrupoAndFecha(@Param("idGrupo") Long idGrupo, @Param("fecha") LocalDate fecha);
}