package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluacionResponseDTO {
    private Long idEvaluacion;
    private String tipoEvaluacion;
    private Integer numeroEvaluacion;
    private LocalDateTime fechaInicio;
    private Integer periodo; // Del PeriodoEvaluacion
}