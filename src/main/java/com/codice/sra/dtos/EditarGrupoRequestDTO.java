package com.codice.sra.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EditarGrupoRequestDTO(
        @NotNull(message = "El docente titular es obligatorio.")
        Long idDocente,

        Long idAula, // Null si es modalidad virtual

        String enlaceVirtual, // Null si es modalidad presencial

        @NotNull(message = "El cupo máximo es obligatorio.")
        @Min(value = 1, message = "El cupo debe ser mayor a cero.")
        Integer cupoMaximo
) {}