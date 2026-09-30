package com.codice.sra.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class CalificacionListaResponseDTO {
    private Long idCalificacion;
    private Long idInscripcion;
    private Long idEvaluacion;
    private BigDecimal nota;
    private String estadoCalificacion;
    private LocalDateTime fechaRegistro;
}
