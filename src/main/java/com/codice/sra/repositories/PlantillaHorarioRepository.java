package com.codice.sra.repositories;

import com.codice.sra.models.PlantillaHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaHorarioRepository extends JpaRepository<PlantillaHorario, Long> {

    List<PlantillaHorario> findByActivaTrueOrderByCodigoPlantillaAsc();

    Optional<PlantillaHorario> findByCodigoPlantillaIgnoreCase(String codigoPlantilla);

    @Query("SELECT p FROM PlantillaHorario p LEFT JOIN FETCH p.detalles d WHERE p.idPlantilla = :idPlantilla AND p.activa = true")
    Optional<PlantillaHorario> findByIdConDetalles(@Param("idPlantilla") Long idPlantilla);

    /**
     * Recupera todas las plantillas activas cargando en un único SELECT
     * la colección de detalles temporales y los días asociados para evitar el problema N+1.
     */
    @Query("SELECT DISTINCT p FROM PlantillaHorario p " +
            "LEFT JOIN FETCH p.detalles d " +
            "LEFT JOIN FETCH d.dia " +
            "WHERE p.activa = true " +
            "ORDER BY p.codigoPlantilla ASC")
    List<PlantillaHorario> findAllActivasConDetalles();
}