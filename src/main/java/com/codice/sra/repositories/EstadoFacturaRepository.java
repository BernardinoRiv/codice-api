package com.codice.sra.repositories;

import com.codice.sra.models.EstadoFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoFacturaRepository extends JpaRepository<EstadoFactura, Long> {
    Optional<EstadoFactura> findByEstadoFactura(String estadoFactura);
}
