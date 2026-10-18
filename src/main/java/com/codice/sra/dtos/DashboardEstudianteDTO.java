package com.codice.sra.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardEstudianteDTO {
    private double cum;
    private int materiasInscritas;
    private String estadoFinanciero;
    private boolean tieneMora;
}