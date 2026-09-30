package com.codice.sra.repositories;

import com.codice.sra.models.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    // Busca al estudiante por el ID de su usuario asociado
    Optional<Estudiante> findByUsuarioIdUsuario(Long idUsuario);
}