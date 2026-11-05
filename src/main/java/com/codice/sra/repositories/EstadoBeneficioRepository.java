package com.codice.sra.repositories;

import com.codice.sra.models.EstadoBeneficio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoBeneficioRepository extends JpaRepository<EstadoBeneficio, Long> {
    Optional<EstadoBeneficio> findByEstadoBeneficio(String estadoBeneficio);
    List<EstadoBeneficio> findByActivoTrue();
}