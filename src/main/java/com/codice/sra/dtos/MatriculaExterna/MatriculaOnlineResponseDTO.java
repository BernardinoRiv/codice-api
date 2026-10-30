package com.codice.sra.dtos.MatriculaExterna;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Confirmación de la pre-matrícula y orden de pago emitida")
public class MatriculaOnlineResponseDTO {

    @Schema(description = "Identificador único de la matrícula en estado PENDIENTE_PAGO", example = "16")
    private Long idMatricula;

    @Schema(description = "Modalidad seleccionada ('EN_LINEA' o 'VENTANILLA')", example = "VENTANILLA")
    private String modalidadPago;

    @Schema(description = "Monto total a cancelar por arancel de matrícula", example = "70.00")
    private BigDecimal totalAPagar;

    @Schema(description = "Fecha límite para realizar el pago antes de que los cupos sean revocados", example = "2026-10-27")
    private LocalDate fechaVencimiento;

    @Schema(description = "Número de Pago Electrónico generado para ventanilla (nulo si es en línea)",
            example = "0981000000160000582026")
    private String npe;

    @Schema(description = "Mensaje de confirmación e instrucciones",
            example = "Pre-matrícula generada. Tienes 48 horas para pagar en Colecturía con tu código NPE.")
    private String mensaje;
}
