package com.codice.sra.repositories;

import com.codice.sra.models.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {
    boolean existsByDocumento(String documento);
    boolean existsByCorreoPersonal(String correoPersonal);
}