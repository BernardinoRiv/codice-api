package com.codice.sra.repositories;

import com.codice.sra.models.EstadoCiclo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCicloRepository extends JpaRepository<EstadoCiclo, Long> {

    /**
     * Recupera un estado de ciclo ignorando mayúsculas, minúsculas y espacios en blanco.
     * Permite resolver estados del dominio como 'PLANIFICACION', 'ACTIVO' o 'FINALIZADO'.
     */
    @Query("SELECT ec FROM EstadoCiclo ec WHERE UPPER(TRIM(ec.estadoCiclo)) = UPPER(TRIM(:estadoCiclo))")
    Optional<EstadoCiclo> findByEstadoCicloIgnoreCase(@Param("estadoCiclo") String estadoCiclo);

    /**
     * Verificación de existencia para validaciones defensivas previas.
     */
    @Query("SELECT CASE WHEN COUNT(ec) > 0 THEN true ELSE false END " +
            "FROM EstadoCiclo ec WHERE UPPER(TRIM(ec.estadoCiclo)) = UPPER(TRIM(:estadoCiclo))")
    boolean existsByEstadoCicloIgnoreCase(@Param("estadoCiclo") String estadoCiclo);
}