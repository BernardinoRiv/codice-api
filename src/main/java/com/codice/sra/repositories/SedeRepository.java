package com.codice.sra.repositories;

import com.codice.sra.models.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {
    boolean existsByCodigoSede(String codigoSede);
}