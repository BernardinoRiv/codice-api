package com.codice.sra.repositories;

import com.codice.sra.models.TipoContratacionDocente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoContratacionDocenteRepository extends JpaRepository<TipoContratacionDocente, Long> {
}