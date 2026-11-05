package com.codice.sra.repositories;

import com.codice.sra.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByReferencia(String referencia);

    Optional<Pago> findByReferencia(String referencia);
}