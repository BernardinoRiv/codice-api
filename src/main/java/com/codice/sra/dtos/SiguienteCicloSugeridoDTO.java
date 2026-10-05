package com.codice.sra.dtos;

public record SiguienteCicloSugeridoDTO(
        Integer anio,
        Integer numeroCiclo,
        String codigoSugerido,
        String descripcionSemestre
) {}