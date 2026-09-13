package com.codice.sra.repositories;

import com.codice.sra.models.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    boolean existsByCodigoEmpleado(String codigoEmpleado);
}
