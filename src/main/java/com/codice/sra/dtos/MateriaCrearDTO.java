package com.codice.sra.dtos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record MateriaCrearDTO(
        @NotBlank @Size(max = 20) String codigoMateria,
        @NotBlank @Size(max = 150) String nombreMateria,
        @NotNull @Positive @Digits(integer = 2, fraction = 2) BigDecimal unidadesValorativas,
        List<@NotNull @Positive Long> idsPrerrequisitos
) {}
