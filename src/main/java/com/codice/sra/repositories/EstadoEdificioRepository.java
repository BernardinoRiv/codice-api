package com.codice.sra.repositories;

import com.codice.sra.models.EstadoEdificio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoEdificioRepository extends JpaRepository<EstadoEdificio, Long> {
    Optional<EstadoEdificio> findByEstadoEdificio(String estadoEdificio);
    List<EstadoEdificio> findByActivoTrue();
}