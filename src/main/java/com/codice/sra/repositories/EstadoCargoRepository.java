package com.codice.sra.repositories;

import com.codice.sra.models.EstadoCargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCargoRepository extends JpaRepository<EstadoCargo, Long> {

    // Método necesario para buscar el estado "PENDIENTE" o "PAGADO"
    Optional<EstadoCargo> findByEstadoCargo(String estadoCargo);

}