package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolvenciaEstudianteDTO {

    private Long idInscripcion;
    private String carnet;
    private String nombreEstudiante;
    private String estadoMatricula;
    private BigDecimal totalCargos;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    private Boolean esSolvente;
}