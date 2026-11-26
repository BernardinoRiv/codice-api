package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record DocenteDatosLaboralesDTO(
        @Size(max = 255) String especialidad,
        @NotNull @Positive Long idSede,
        @NotNull @Positive Long idTipoContratacion,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin) {}
