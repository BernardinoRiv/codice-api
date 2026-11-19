
package com.codice.sra.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record PensumBorradorDTO(
        @NotNull List<@NotNull @Valid MateriaAsignadaDTO> materias
) {
    public record MateriaAsignadaDTO(
            @NotNull @Positive Long idMateria,
            @NotNull @Positive Integer cicloRecomendado,
            @NotNull Boolean obligatoria
    ) {}
}
