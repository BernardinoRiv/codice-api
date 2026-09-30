package com.codice.sra.repositories;

import com.codice.sra.models.EstadoDocente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoDocenteRepository extends JpaRepository<EstadoDocente, Long> {
    Optional<EstadoDocente> findByEstadoDocente(String estadoDocente);
}