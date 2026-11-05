package com.codice.sra.dtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ComprobantePagoDTO {
    private String numeroFactura;
    private LocalDateTime fechaEmision;
    private String nombreCompleto;
    private String carnetEstudiante;
    private String correoEstudiante;
    private BigDecimal totalCobrado;
    private String cajeroResponsable;
    private List<DetalleComprobanteDTO> detalles;

    @Data
    @Builder
    public static class DetalleComprobanteDTO {
        private String concepto;
        private BigDecimal montoTotal;
    }
}