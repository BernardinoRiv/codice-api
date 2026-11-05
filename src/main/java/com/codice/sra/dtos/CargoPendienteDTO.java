package com.codice.sra.dtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CargoPendienteDTO {
    private Long idCargo;
    private String ciclo; // Ej: "01-2027"
    private String concepto; // Ej: "Cuota 1 - Pregrado" o "Matrícula Maestría"
    private Integer numeroCuota;

    // Desglose financiero
    private BigDecimal montoBase;      // Ej: $70.00
    private BigDecimal montoDescuento; // Ej: $17.00 (Si tiene beca activa)
    private BigDecimal montoRecargo;   // Ej: $5.00 (Calculado al vuelo si pasó el día 10)
    private BigDecimal montoTotal;     // Ej: $58.00 (Base - Descuento + Recargo)

    private LocalDate fechaVencimiento;
    private boolean aplicaMora; // Indica si se le sumaron los $5.00 al vuelo
}