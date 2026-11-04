package com.codice.sra.repositories;

import com.codice.sra.models.EstadoFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoFacturaRepository extends JpaRepository<EstadoFactura, Long> {
}