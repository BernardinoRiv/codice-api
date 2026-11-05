package com.codice.sra.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EstadoCuentaCicloDTO {

    private EstudianteInfo estudiante;
    private ResumenFinanciero resumen;
    private List<CargoDetalle> cargosPendientes;
    private List<PagoDetalle> historialPagos;

    @Data
    @Builder
    public static class EstudianteInfo {
        private Long idEstudiante;
        private String carnet;
        private String nombreCompleto;
        private String carrera;
    }

    @Data
    @Builder
    public static class ResumenFinanciero {
        private BigDecimal deudaTotal;    // Todo lo facturado en el ciclo
        private BigDecimal totalPagado;   // Todo lo que ya se canceló
        private BigDecimal saldoPendiente;// Lo que falta por pagar
    }

    @Data
    @Builder
    public static class CargoDetalle {
        private Long idCargo;
        private String concepto;
        private String mes;
        private LocalDate fechaVencimiento;
        private BigDecimal monto;
        private BigDecimal montoBase;    // <--- AGREGAR ESTO (Cuota sin mora)
        private BigDecimal montoRecargo;
    }

    @Data
    @Builder
    public static class PagoDetalle {
        private Long idPago;
        private String numeroRecibo;
        private LocalDate fechaPago;
        private String concepto;
        private String metodoPago;
        private BigDecimal montoPagado;
    }
}