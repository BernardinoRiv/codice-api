package com.codice.sra.repositories;

import com.codice.sra.models.PagoCargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoCargoRepository extends JpaRepository<PagoCargo, Long> {
    List<PagoCargo> findByCargoIdCargoEstudiante(Long idCargoEstudiante);
    // Obtiene el detalle de los pagos aplicados a los cargos de una matrícula específica
    List<PagoCargo> findByCargo_Matricula_IdMatricula(Long idMatricula);
}