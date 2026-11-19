package com.codice.sra.repositories;

import com.codice.sra.models.EstadoPensum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoPensumRepository extends JpaRepository<EstadoPensum, Long> {
    Optional<EstadoPensum> findByEstadoPensum(String estadoPensum);
}