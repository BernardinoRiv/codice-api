package com.codice.sra.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record InscribirMateriasRequestDTO(
        @NotEmpty(message = "Debe seleccionar al menos un grupo.")
        List<@NotNull(message = "Los identificadores de grupo no pueden ser nulos.") Long> idsGrupos,
        String numeroTarjeta,
        String titularTarjeta,
        String fechaExpiracion,
        String cvv
) {}
