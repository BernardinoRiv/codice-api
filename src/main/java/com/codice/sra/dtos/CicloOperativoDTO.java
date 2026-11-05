package com.codice.sra.dtos;

import java.time.LocalDate;

public record CicloOperativoDTO(
        Long idCiclo,
        String codigoCiclo,
        Integer anio,
        Integer numeroCiclo,
        String estadoCiclo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean esPlanificacion
) {}