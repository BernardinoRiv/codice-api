package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class CalificacionRegistroRequestDTO {

    @NotNull(message = "El ID de la inscripción es requerido")
    private Long idInscripcion;

    @NotNull(message = "El ID de la evaluación es requerido")
    private Long idEvaluacion;

    @DecimalMin(value = "0.00", message = "La nota mínima es 0.00")
    @DecimalMax(value = "10.00", message = "La nota máxima es 10.00")
    private BigDecimal nota;
}