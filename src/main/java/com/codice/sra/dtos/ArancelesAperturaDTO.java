package com.codice.sra.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Resumen de aranceles oficiales clonados para la apertura y relevo del ciclo")
public record ArancelesAperturaDTO(
        Long idCiclo,
        // Pregrado
        Long idMatriculaPregrado,
        BigDecimal montoMatriculaPregrado,
        Long idCuotaPregrado,
        BigDecimal montoCuotaPregrado,

        // Maestría / Posgrado (opcionales)
        Long idMatriculaMaestria,
        BigDecimal montoMatriculaMaestria,
        Long idCuotaMaestria,
        BigDecimal montoCuotaMaestria,

        boolean arancelesPregradoCompletos
) {}