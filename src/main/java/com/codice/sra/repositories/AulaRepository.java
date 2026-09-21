package com.codice.sra.repositories;

import com.codice.sra.models.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AulaRepository extends JpaRepository<Aula, Long> {

    Optional<Aula> findByCodigoAulaIgnoreCase(String codigoAula);

    // Consulta optimizada para la validación territorial de GrupoService (Carga Eagerly requerida)
    @Query("SELECT a FROM Aula a " +
            "JOIN FETCH a.edificio e " +
            "JOIN FETCH e.sede s " +
            "JOIN FETCH a.tipoAula ta " +
            "JOIN FETCH a.estadoAula ea " +
            "WHERE a.idAula = :idAula")
    Optional<Aula> findByIdConEdificioYSede(@Param("idAula") Long idAula);

    // Listar aulas disponibles por sede para alimentar los selectores del frontend en un solo SELECT (Sin N+1)
    @Query("SELECT a FROM Aula a " +
            "JOIN FETCH a.edificio e " +
            "JOIN a.estadoAula ea " +
            "WHERE e.sede.idSede = :idSede " +
            "AND UPPER(ea.estadoAula) = 'DISPONIBLE' " +
            "ORDER BY e.nombreEdificio ASC, a.codigoAula ASC")
    List<Aula> findAulasDisponiblesPorSede(@Param("idSede") Long idSede);
}