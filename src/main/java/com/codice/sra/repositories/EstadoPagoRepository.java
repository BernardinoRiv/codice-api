package com.codice.sra.repositories;

import com.codice.sra.models.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoPagoRepository extends JpaRepository<EstadoPago, Long> {
    Optional<EstadoPago> findByEstadoPago(String estadoPago);
}
