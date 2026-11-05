package com.codice.sra.repositories;

import com.codice.sra.models.Pensum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PensumRepository extends JpaRepository<Pensum, Integer> {

    // Busca el pensum activo (ID estado 1) para la carrera asociada a la Sede
    @Query("SELECT p FROM Pensum p WHERE p.carrera.idCarrera = (SELECT cs.carrera.idCarrera FROM CarreraSede cs WHERE cs.idCarreraSede = :idCarreraSede) AND p.estadoPensum.idEstadoPensum = 1")
    Optional<Pensum> findPensumVigenteByCarreraSede(@Param("idCarreraSede") Integer idCarreraSede);

    // Consulta explícita por Long mapeando a la propiedad real 'idPensum'
    @Query("SELECT p FROM Pensum p WHERE p.idPensum = :id")
    Optional<Pensum> findByIdPensumLong(@Param("id") Long id);

}