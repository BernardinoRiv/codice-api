package com.codice.sra.repositories;

import com.codice.sra.dtos.DocenteSeleccionDTO;
import com.codice.sra.models.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {
    boolean existsByCodigoDocente(String codigoDocente);
    Optional<Docente> findByUsuarioIdUsuario(Long idUsuario);

    //Verificar limite de materias de un docente
    @Query("SELECT d FROM Docente d " +
            "JOIN FETCH d.tipoContratacion tc " +
            "JOIN FETCH d.estadoDocente ed " +
            "WHERE d.idDocente = :idDocente")
    Optional<Docente> findByIdConContratacion(@Param("idDocente") Long idDocente);

    @Query("SELECT new com.codice.sra.dtos.DocenteSeleccionDTO(" +
            "d.idDocente, d.codigoDocente, p.nombres, p.apellidos, tc.tipoContratacion, tc.maximoMaterias) " +
            "FROM Docente d " +
            "JOIN d.persona p " +
            "JOIN d.tipoContratacion tc " +
            "JOIN d.estadoDocente ed " +
            "WHERE UPPER(TRIM(ed.estadoDocente)) = 'ACTIVO' " +
            "ORDER BY p.apellidos ASC, p.nombres ASC")
    List<DocenteSeleccionDTO> findDocentesParaSeleccion();
}