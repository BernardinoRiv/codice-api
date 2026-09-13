package com.codice.sra.repositories;

import com.codice.sra.models.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {
    boolean existsByCodigoDocente(String codigoDocente);
}