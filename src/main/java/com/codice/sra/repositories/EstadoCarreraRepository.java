package com.codice.sra.repositories;

import com.codice.sra.models.EstadoCarrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCarreraRepository extends JpaRepository<EstadoCarrera, Long> {
    Optional<EstadoCarrera> findByEstadoCarreraIgnoreCase(String estadoCarrera);
}