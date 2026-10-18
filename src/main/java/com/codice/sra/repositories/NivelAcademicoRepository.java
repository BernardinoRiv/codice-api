package com.codice.sra.repositories;

import com.codice.sra.models.NivelAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NivelAcademicoRepository extends JpaRepository<NivelAcademico, Long> {

    Optional<NivelAcademico> findByNivelIgnoreCase(String nivel);

    boolean existsByNivelIgnoreCase(String nivel);
}