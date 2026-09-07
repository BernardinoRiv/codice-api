package com.codice.sra.repositories;

import com.codice.sra.models.EstadoRegistroPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoRegistroPersonaRepository extends JpaRepository<EstadoRegistroPersona, Long> {

    // Busca el estado por su nombre (ej: "PENDIENTE", "COMPLETADA")
    Optional<EstadoRegistroPersona> findByEstadoRegistro(String estadoRegistro);
}