package com.codice.sra.dtos;

import java.time.LocalTime;
import java.util.List;

public record PlantillaHorarioResponseDTO(
        Long idPlantilla,
        String codigoPlantilla,
        String descripcion,
        List<DetallePlantillaDTO> detalles
) {
    // Record anidado: automáticamente genera constructor, getters (accesores),
    // equals(), hashCode() y toString() sin una sola anotación de Lombok.
    public record DetallePlantillaDTO(
            String dia,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {}
}