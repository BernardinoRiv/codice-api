package com.codice.sra.repositories;

import com.codice.sra.models.CargoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CargoEstudianteRepository extends JpaRepository<CargoEstudiante, Long> {

    // 1. Restaurado y corregido para que SolvenciaService funcione
    List<CargoEstudiante> findByMatricula_IdMatricula(Long idMatricula);

    // 2. Métodos para FinanzasService
    boolean existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobro(Long idMatricula, Long idConceptoCobro);
    List<CargoEstudiante> findByMatricula_Estudiante_CarnetAndEstadoCargo_EstadoCargoOrderByFechaVencimientoAsc(String carnet, String estado);
    List<CargoEstudiante> findByMatricula_Estudiante_IdEstudianteAndEstadoCargo_EstadoCargoOrderByFechaVencimientoAsc(Long idEstudiante, String estadoCargo);
    boolean existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobroAndNumeroCuota(Long idMatricula, Long idConcepto, Integer numeroCuota);

    @Query("SELECT c FROM CargoEstudiante c " +
            "WHERE c.estadoCargo.estadoCargo = :estado " +
            "AND MONTH(c.fechaVencimiento) = :mes " +
            "AND YEAR(c.fechaVencimiento) = :anio " +
            "AND c.matricula.ciclo.estadoCiclo.estadoCiclo = 'ACTIVO' " +
            "AND c.matricula.estudiante.estadoEstudiante.estadoEstudiante = 'ACTIVO'")
    List<CargoEstudiante> findCargosPendientesPorMesYAnio(
            @Param("estado") String estado,
            @Param("mes") int mes,
            @Param("anio") int anio);

    // Obtiene todos los cargos (pagados y pendientes) de un estudiante en un ciclo específico
    List<CargoEstudiante> findByMatricula_Estudiante_CarnetAndMatricula_Ciclo_IdCiclo(String carnet, Long idCiclo);
    // 3. Sumar cuánto dinero debe en el ciclo activo actual
    @Query("SELECT COALESCE(SUM(c.montoTotal), 0) FROM CargoEstudiante c WHERE c.matricula.estudiante.carnet = :carnet AND c.matricula.ciclo.estadoCiclo.estadoCiclo = 'ACTIVO' AND c.estadoCargo.estadoCargo = 'PENDIENTE'")
    java.math.BigDecimal calcularDeudaPendienteCicloActivo(@Param("carnet") String carnet);


    List<CargoEstudiante> findByMatricula_Estudiante_IdEstudianteAndMatricula_Ciclo_IdCiclo(Long idEstudiante, Long idCiclo);


    // Localiza el cargo de matrícula (tipo de cobro 1) ligado a la matrícula
    Optional<CargoEstudiante> findByMatricula_IdMatriculaAndConceptoCobro_TipoCobro_IdTipoCobro(
            Long idMatricula, Long idTipoCobro);


    // Firma requerida para listar los cargos pendientes asociados a la matrícula
    List<CargoEstudiante> findByMatricula_IdMatriculaAndEstadoCargo_EstadoCargo(Long idMatricula, String estadoCargo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CargoEstudiante c WHERE c.matricula.idMatricula = :idMatricula")
    void deleteByMatriculaId(@Param("idMatricula") Long idMatricula);

    List<CargoEstudiante> findByMatricula_IdMatriculaAndEstadoCargo_EstadoCargoOrderByNumeroCuotaAsc(
            Long idMatricula,
            String estadoCargo
    );

}