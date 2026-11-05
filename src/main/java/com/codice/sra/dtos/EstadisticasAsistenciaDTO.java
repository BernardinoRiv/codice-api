package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasAsistenciaDTO {
    private long totalClases;
    private long presentes;
    private long tardes;
    private long ausentes;
    private double porcentajeAsistencia;
}