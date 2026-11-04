package com.codice.sra.repositories;

import com.codice.sra.models.EstadoFacultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoFacultadRepository extends JpaRepository<EstadoFacultad, Long> {
}