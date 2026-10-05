package com.codice.sra.repositories;

import com.codice.sra.models.Ciclo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CicloRepository extends JpaRepository<Ciclo, Long> {

    Optional<Ciclo> findByCodigoCicloIgnoreCase(String codigoCiclo);

    // Listar ciclos activos para poblar el dropdown de contexto
    @Query("SELECT c FROM Ciclo c WHERE UPPER(c.estadoCiclo.estadoCiclo) = 'ACTIVO' ORDER BY c.anio DESC, c.numeroCiclo DESC")
    List<Ciclo> findCiclosActivos();

    // Consulta con join fetch del estado para evitar LazyInitializationException
    @Query("SELECT c FROM Ciclo c JOIN FETCH c.estadoCiclo WHERE c.idCiclo = :idCiclo")
    Optional<Ciclo> findByIdConEstado(@Param("idCiclo") Long idCiclo);

    @Query("SELECT c FROM Ciclo c " +
            "WHERE c.fechaInicio <= CURRENT_DATE " +
            "AND c.fechaFin >= CURRENT_DATE")
    Optional<Ciclo> findCicloActivo();


    //Recupera el ciclo institucional en estado formal PLANIFICACION.

    @Query("SELECT c FROM Ciclo c JOIN FETCH c.estadoCiclo ec " +
            "WHERE UPPER(TRIM(ec.estadoCiclo)) = 'PLANIFICACION'")
    Optional<Ciclo> findCicloEnPlanificacion();

    //Lista todos los ciclos en preparación para la pantalla de relevo y activación.
    @Query("SELECT c FROM Ciclo c JOIN FETCH c.estadoCiclo ec " +
            "WHERE UPPER(TRIM(ec.estadoCiclo)) = 'PLANIFICACION' " +
            "ORDER BY c.anio ASC, c.numeroCiclo ASC")
    List<Ciclo> findAllEnPlanificacion();

    //Verifica la existencia de un semestre específico para impedir registros duplicados.
    boolean existsByAnioAndNumeroCiclo(Integer anio, Integer numeroCiclo);

    /**
     * Obtiene el ciclo más reciente cronológicamente para calcular de forma
     * determinista la progresión del siguiente semestre.
     */
    @Query("SELECT c FROM Ciclo c " +
            "ORDER BY c.anio DESC, c.numeroCiclo DESC " +
            "LIMIT 1")
    Optional<Ciclo> findUltimoCicloRegistrado();

}