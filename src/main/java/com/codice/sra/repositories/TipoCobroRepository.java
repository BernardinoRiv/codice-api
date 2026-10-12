package com.codice.sra.repositories;

import com.codice.sra.models.TipoCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoCobroRepository extends JpaRepository<TipoCobro, Long> {
    Optional<TipoCobro> findByTipoCobroIgnoreCase(String tipoCobro);
}