package com.codice.sra.repositories;

import com.codice.sra.models.Persona;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {
    boolean existsByDocumento(String documento);
    boolean existsByCorreoPersonal(String correoPersonal);

    Optional<Persona> findByDocumento(String documento);

    // Bloquea la fila en la BD para evitar concurrencia
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Persona p WHERE p.idPersona = :idPersona")
    Optional<Persona> findByIdForUpdate(@Param("idPersona") Long idPersona);
}