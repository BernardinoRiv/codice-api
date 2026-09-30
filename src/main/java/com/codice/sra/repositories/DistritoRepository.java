package com.codice.sra.repositories;

import com.codice.sra.models.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistritoRepository extends JpaRepository<Distrito, Long> {
    List<Distrito> findByDepartamento_IdDepartamentoOrderByNombreAsc(Long idDepartamento);
}