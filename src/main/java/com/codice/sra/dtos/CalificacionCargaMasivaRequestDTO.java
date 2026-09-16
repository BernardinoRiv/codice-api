package com.codice.sra.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CalificacionCargaMasivaRequestDTO {
    @NotNull private Long idEvaluacion;
    @NotNull @Valid private List<CalificacionItemDTO> calificaciones;
}
