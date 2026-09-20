package com.codice.sra.repositories;

import com.codice.sra.models.Modalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModalidadRepository extends JpaRepository<Modalidad, Long> {

    Optional<Modalidad> findByModalidadIgnoreCase(String modalidad);
}