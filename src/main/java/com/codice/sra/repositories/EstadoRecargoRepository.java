package com.codice.sra.repositories;

import com.codice.sra.models.EstadoRecargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoRecargoRepository extends JpaRepository<EstadoRecargo, Long> {
}