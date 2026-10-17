package com.codice.sra.repositories;

import com.codice.sra.models.EstadoAula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoAulaRepository extends JpaRepository<EstadoAula, Long> {
    Optional<EstadoAula> findByEstadoAula(String estadoAula);
    List<EstadoAula> findByActivoTrue();
}