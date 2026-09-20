package com.codice.sra.repositories;

import com.codice.sra.dtos.MateriaPensumResponseDTO;
import com.codice.sra.models.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long> {

    Optional<Materia> findByCodigoMateriaIgnoreCase(String codigoMateria);

    /**
     * Obtiene las materias del pensum vigente o activo de la carrera,
     * ordenadas por ciclo recomendado y nombre de materia.
     */
    @Query("SELECT DISTINCT new com.codice.sra.dtos.MateriaPensumResponseDTO(" +
            "m.idMateria, m.codigoMateria, m.nombreMateria, pm.cicloRecomendado) " +
            "FROM PensumMateria pm " +
            "JOIN pm.materia m " +
            "JOIN pm.pensum p " +
            "WHERE p.carrera.idCarrera = :idCarrera " +
            "AND UPPER(TRIM(p.estadoPensum.estadoPensum)) IN ('VIGENTE', 'ACTIVO') " +
            "AND m.estadoMateria = true " +
            "ORDER BY pm.cicloRecomendado ASC, m.nombreMateria ASC")
    List<MateriaPensumResponseDTO> findMateriasActivasPorCarrera(@Param("idCarrera") Long idCarrera);
}