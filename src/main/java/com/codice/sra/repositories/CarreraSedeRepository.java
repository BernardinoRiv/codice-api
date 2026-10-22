package com.codice.sra.repositories;

import com.codice.sra.models.CarreraSede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarreraSedeRepository extends JpaRepository<CarreraSede, Long> {
}