package com.codice.sra.repositories;

import com.codice.sra.models.EstadoBeneficioEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoBeneficioEstudianteRepository extends JpaRepository<EstadoBeneficioEstudiante, Long> {
    Optional<EstadoBeneficioEstudiante> findByEstadoBeneficioEstudiante(String estadoBeneficioEstudiante);
    List<EstadoBeneficioEstudiante> findByActivoTrue();
}