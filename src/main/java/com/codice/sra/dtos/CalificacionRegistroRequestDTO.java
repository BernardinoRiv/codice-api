package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class CalificacionRegistroRequestDTO {
    @NotNull private Long idInscripcion;
    @NotNull private Long idEvaluacion;
    @DecimalMin("0.0") @DecimalMax("10.0") private BigDecimal nota;
}
