package com.codice.sra.repositories;

import com.codice.sra.models.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    boolean existsByCodigoEmpleado(String codigoEmpleado);
    @Query("SELECT e FROM Empleado e " +
            "LEFT JOIN FETCH e.sede " +
            "WHERE e.persona.idPersona = :idPersona")
    Optional<Empleado> findByPersonaIdConRelaciones(@Param("idPersona") Long idPersona);
}
