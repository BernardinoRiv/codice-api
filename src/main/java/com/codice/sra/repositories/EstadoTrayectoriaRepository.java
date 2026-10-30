package com.codice.sra.repositories;

import com.codice.sra.models.EstadoTrayectoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoTrayectoriaRepository extends JpaRepository<EstadoTrayectoria, Long> {

    Optional<EstadoTrayectoria> findByEstadoTrayectoria(String trayectoria);
}