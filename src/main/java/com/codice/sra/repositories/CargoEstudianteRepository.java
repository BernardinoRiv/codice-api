package com.codice.sra.repositories;

import com.codice.sra.models.CargoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CargoEstudianteRepository extends JpaRepository<CargoEstudiante, Long> {

    // 1. Restaurado y corregido para que SolvenciaService funcione
    List<CargoEstudiante> findByMatricula_IdMatricula(Long idMatricula);

    // 2. Métodos para FinanzasService
    boolean existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobro(Long idMatricula, Long idConceptoCobro);
    List<CargoEstudiante> findByMatricula_Estudiante_CarnetAndEstadoCargo_EstadoCargoOrderByFechaVencimientoAsc(String carnet, String estado);
    List<CargoEstudiante> findByMatricula_Estudiante_IdEstudianteAndEstadoCargo_EstadoCargoOrderByFechaVencimientoAsc(Long idEstudiante, String estadoCargo);
    boolean existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobroAndNumeroCuota(Long idMatricula, Long idConcepto, Integer numeroCuota);
}