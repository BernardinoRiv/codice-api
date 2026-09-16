package com.codice.sra.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalificacionResponseDTO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long idCalificacion;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long idInscripcion;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long idEvaluacion;

    private BigDecimal nota;
    private String estadoCalificacion;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaModificacion;
}