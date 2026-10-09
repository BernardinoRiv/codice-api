package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocenteEdicionRequestDTO extends PersonaContactoEdicionDTO {
    // --- Datos Contractuales/Laborales Editables ---
    @NotNull(message = "La sede institucional es obligatoria")
    @Positive(message = "El ID de sede debe ser positivo")
    private Long idSede;

    @NotNull(message = "El tipo de contratación es obligatorio")
    @Positive(message = "El ID de tipo de contratación debe ser positivo")
    private Long idTipoContratacion;
}