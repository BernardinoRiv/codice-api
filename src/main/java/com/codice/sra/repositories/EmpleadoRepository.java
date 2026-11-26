package com.codice.sra.repositories;

import com.codice.sra.models.Empleado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    @EntityGraph(attributePaths = {"persona", "persona.tipoDocumento", "persona.distrito", "persona.distrito.departamento", "usuario", "usuario.estadoUsuario", "area", "cargo", "estadoEmpleado", "sede"})
    List<Empleado> findAllBy();

    @EntityGraph(attributePaths = {"persona", "persona.tipoDocumento", "persona.distrito", "persona.distrito.departamento", "usuario", "usuario.estadoUsuario", "area", "cargo", "estadoEmpleado", "sede"})
    List<Empleado> findAllBySede_IdSede(Long idSede);

    Optional<Empleado> findByIdEmpleadoAndSede_IdSede(Long idEmpleado, Long idSede);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Empleado> findForUpdateByIdEmpleado(Long idEmpleado);

    boolean existsByCodigoEmpleado(String codigoEmpleado);

    @Query("SELECT e FROM Empleado e LEFT JOIN FETCH e.sede WHERE e.persona.idPersona = :idPersona")
    Optional<Empleado> findByPersonaIdConRelaciones(@Param("idPersona") Long idPersona);

    Optional<Empleado> findByUsuarioIdUsuario(Long idUsuario);
}