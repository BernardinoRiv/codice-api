package com.codice.sra.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estructura estándar para opciones de catálogos y listas desplegables")
public record CatalogoDTO(
        @Schema(description = "Identificador único del registro en base de datos")
        Long id,

        @Schema(description = "Nombre o descripción visible de la opción")
        String nombre
) {}