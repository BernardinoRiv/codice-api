package com.codice.sra.repositories;

import com.codice.sra.models.CanalPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CanalPagoRepository extends JpaRepository<CanalPago, Long> {
    Optional<CanalPago> findByCanalPago(String canalPago);
}
