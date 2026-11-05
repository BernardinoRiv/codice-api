package com.codice.sra.repositories;

import com.codice.sra.models.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("SELECT a FROM Auditoria a " +
            "LEFT JOIN FETCH a.usuario " +
            "WHERE a.tabla = :tabla AND a.idRegistro = :idRegistro " +
            "ORDER BY a.fechaHora DESC")
    List<Auditoria> findHistorialPorEntidad(@Param("tabla") String tabla, @Param("idRegistro") Long idRegistro);
}