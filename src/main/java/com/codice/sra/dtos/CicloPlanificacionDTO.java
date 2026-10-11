package com.codice.sra.dtos;

import java.time.LocalDate;

public record CicloPlanificacionDTO(
        Long idCiclo,
        String codigoCiclo,
        Integer anio,
        Integer numeroCiclo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estadoCiclo,
        Long totalSeccionesConfiguradas
) {}