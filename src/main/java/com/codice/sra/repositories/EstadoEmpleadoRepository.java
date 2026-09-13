package com.codice.sra.repositories;

import com.codice.sra.models.EstadoEmpleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoEmpleadoRepository extends JpaRepository<EstadoEmpleado, Long> {
    Optional<EstadoEmpleado> findByEstadoEmpleado(String estadoEmpleado);

}
