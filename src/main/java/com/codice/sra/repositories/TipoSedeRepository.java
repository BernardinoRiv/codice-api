package com.codice.sra.repositories;

import com.codice.sra.models.TipoSede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoSedeRepository extends JpaRepository<TipoSede, Long> {
}