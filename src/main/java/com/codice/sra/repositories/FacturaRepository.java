package com.codice.sra.repositories;

import com.codice.sra.models.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    // Firma requerida para recuperar la factura ligada a un pago específico
    Optional<Factura> findByPago_IdPago(Long idPago);

    Optional<Factura> findByNumeroFactura(String numeroFactura);
}