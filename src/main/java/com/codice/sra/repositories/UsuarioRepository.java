package com.codice.sra.repositories;

import com.codice.sra.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);
    boolean existsByCorreoInstitucional(String correoInstitucional);

    boolean existsByPersona_IdPersonaAndRol_IdRol(Long idPersona, Long idRol);
    boolean existsByPersona_IdPersonaAndRol_Rol(Long idPersona, String rol);
    boolean existsByPersona_IdPersona(Long idPersona);
}