package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class CalificacionItemDTO {
    @NotNull private Long idInscripcion;
    @DecimalMin("0.0") @DecimalMax("10.0") private BigDecimal nota;
}
