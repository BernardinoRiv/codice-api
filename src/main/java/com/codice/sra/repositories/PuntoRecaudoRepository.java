package com.codice.sra.repositories;

import com.codice.sra.models.PuntoRecaudo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PuntoRecaudoRepository extends JpaRepository<PuntoRecaudo, Long> {
    // Busca la primera caja que esté activa para la sede de la cajera
    Optional<PuntoRecaudo> findFirstBySede_IdSedeAndActivoTrue(Long idSede);
}