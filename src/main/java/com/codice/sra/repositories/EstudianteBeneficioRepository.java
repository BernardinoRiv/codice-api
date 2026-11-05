package com.codice.sra.repositories;

import com.codice.sra.models.EstudianteBeneficio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstudianteBeneficioRepository extends JpaRepository<EstudianteBeneficio, Long> {

    // Busca navegando: EstudianteBeneficio -> EstadoBeneficioEstudiante -> estadoBeneficioEstudiante
    Optional<EstudianteBeneficio> findByEstudiante_IdEstudianteAndEstadoBeneficioEstudiante_EstadoBeneficioEstudiante(Long idEstudiante, String estado);

    boolean existsByEstudiante_IdEstudianteAndEstadoBeneficioEstudiante_EstadoBeneficioEstudiante(Long idEstudiante, String estado);
}