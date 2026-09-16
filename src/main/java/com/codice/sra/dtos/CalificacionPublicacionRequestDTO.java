package com.codice.sra.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CalificacionPublicacionRequestDTO {
    @NotNull private Long idEvaluacion;
}
