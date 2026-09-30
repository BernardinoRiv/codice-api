package com.codice.sra.dtos;

public record AulaResponseDTO(
        Long idAula,
        String codigoAula,
        String edificio,
        Integer capacidad
) {}