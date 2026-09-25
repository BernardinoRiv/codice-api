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

    @NotNull(message = "La nota es requerida")
    @DecimalMin(value = "0.0", message = "La nota mínima es 0.0")
    @DecimalMax(value = "10.0", message = "La nota máxima es 10.0")
    private BigDecimal nota;
}