package com.codice.sra.repositories;

import com.codice.sra.models.EstadoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoMatriculaRepository extends JpaRepository<EstadoMatricula, Long> {

    Optional<EstadoMatricula> findByEstadoMatricula(String estadoMatricula);
}