package com.codice.sra.dtos;

public record ResumenCarreraOfertaDTO(
        Long idCarrera,
        String codigoCarrera,
        String nombreCarrera,
        Long totalSecciones
) {}