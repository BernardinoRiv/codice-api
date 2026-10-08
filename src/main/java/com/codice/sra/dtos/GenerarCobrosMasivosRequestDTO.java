package com.codice.sra.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerarCobrosMasivosRequestDTO {

    @NotNull(message = "El ID del ciclo es obligatorio.")
    private Long idCiclo;

    // --- Aranceles vigentes para PREGRADO / CARRERAS ---
    private Long idMatriculaCarrera;
    private Long idCuotaCarrera;

    // --- Aranceles vigentes para MAESTRÍAS ---
    private Long idMatriculaMaestria;
    private Long idCuotaMaestria;

    // --- Aranceles vigentes para TESIS / PREESPECIALIZACIÓN ---
    private Long idMatriculaTesis;
    private Long idCuotaTesis;
}