package com.codice.sra.repositories;

import com.codice.sra.models.EstadoGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoGrupoRepository extends JpaRepository<EstadoGrupo, Long> {

    Optional<EstadoGrupo> findByEstadoGrupoIgnoreCase(String estadoGrupo);
}