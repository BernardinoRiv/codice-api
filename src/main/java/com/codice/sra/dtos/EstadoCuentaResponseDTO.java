package com.codice.sra.dtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class EstadoCuentaResponseDTO {
    private EstudianteCajaDTO estudiante;
    private List<CargoPendienteDTO> cargosPendientes;
    private BigDecimal totalDeuda; // La suma de todos los montos totales pendientes
}