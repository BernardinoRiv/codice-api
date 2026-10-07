package com.codice.sra.repositories;

import com.codice.sra.models.RecuperacionClave;
import com.codice.sra.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecuperacionClaveRepository extends JpaRepository<RecuperacionClave, Long> {

    // Busca los tokens de un usuario que no hayan sido usados y que aún no expiren
    List<RecuperacionClave> findByUsuarioAndUsadoFalseAndFechaExpiracionAfter(Usuario usuario, LocalDateTime fechaActual);
}