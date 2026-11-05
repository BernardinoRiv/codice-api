package com.codice.sra.dtos.MatriculaExterna;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprobante final tras la acreditación del pago y aprovisionamiento de usuario")
public class ConfirmacionMatriculaOnlineDTO {

    @Schema(description = "Número oficial de carnet estudiantil asignado", example = "LH260016")
    private String carnet;

    @Schema(description = "Correo institucional generado automáticamente por el sistema", example = "ce.lopez@estudiante.uma.edu.sv")
    private String correoInstitucional;

    @Schema(description = "Número de factura legal emitida en el sistema", example = "FAC-WEB-1791500000000")
    private String numeroFactura;

    @Schema(description = "Número de autorización emitido por la pasarela de pago", example = "AUTH-48A9C2D1")
    private String numeroAutorizacion;

    @Schema(description = "Monto cancelado", example = "70.00")
    private BigDecimal montoPagado;

    @Schema(description = "Fecha y hora del procesamiento del pago", example = "2026-10-25T14:30:00")
    private LocalDateTime fechaPago;

    @Schema(description = "Mensaje informativo sobre el despacho de accesos",
            example = "Matrícula completada con éxito. Se ha enviado la contraseña de acceso institucional a tu correo personal.")
    private String mensaje;
}