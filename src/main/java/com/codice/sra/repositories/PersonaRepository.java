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

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByCorreoPersonal(String correoPersonal);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Persona p WHERE p.idPersona = :idPersona")
    Optional<Persona> findByIdForUpdate(@Param("idPersona") Long idPersona);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Persona p WHERE p.numeroDocumento = :numeroDocumento")
    Optional<Persona> findByNumeroDocumentoForUpdate(@Param("numeroDocumento") String numeroDocumento);

    @Query("SELECT p FROM Persona p " +
            "LEFT JOIN FETCH p.tipoDocumento " +
            "LEFT JOIN FETCH p.distrito d " +
            "LEFT JOIN FETCH d.departamento " +
            "WHERE p.numeroDocumento = :numeroDocumento")
    Optional<Persona> findByNumeroDocumentoConUbicacion(@Param("numeroDocumento") String numeroDocumento);
}