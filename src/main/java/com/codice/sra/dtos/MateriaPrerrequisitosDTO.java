package com.codice.sra.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MateriaPrerrequisitosDTO(
        @NotNull List<@NotNull @Positive Long> idsPrerrequisitos
) {}
