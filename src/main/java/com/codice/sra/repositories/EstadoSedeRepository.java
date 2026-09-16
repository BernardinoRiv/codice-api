package com.codice.sra.repositories;

import com.codice.sra.models.EstadoSede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoSedeRepository extends JpaRepository<EstadoSede, Long> {
}
