package com.codice.sra.repositories;

import com.codice.sra.models.Materia;
import com.codice.sra.models.PensumMateria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PensumMateriaRepository extends JpaRepository<PensumMateria, Long> {

    @Query("SELECT pm.materia FROM PensumMateria pm WHERE pm.pensum.idPensum = :idPensum AND pm.cicloRecomendado = 1 AND pm.obligatoria = true")
    List<Materia> findMateriasPrimerCicloByPensum(@Param("idPensum") Integer idPensum);
}