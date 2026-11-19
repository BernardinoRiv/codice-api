
package com.codice.sra.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PensumCrearDTO(
        @NotNull Long idCarrera,
        @NotNull @Min(1) Integer version,
        @NotNull @Min(1) Integer anioInicio,
        @NotNull @Min(1) Integer anioFin
) {}
