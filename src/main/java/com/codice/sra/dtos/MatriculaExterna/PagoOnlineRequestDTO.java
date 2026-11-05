package com.codice.sra.dtos.MatriculaExterna;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Información transaccional para liquidar la matrícula en pasarela simulada")
public class PagoOnlineRequestDTO {

    @NotNull(message = "El ID de la matrícula es obligatorio.")
    @Schema(description = "ID de la matrícula a liquidar", example = "16")
    private Long idMatricula;

    @NotBlank(message = "El número de tarjeta es obligatorio.")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Número de tarjeta bancaria inválido.")
    @Schema(description = "Número de tarjeta sin espacios ni guiones", example = "4000123456789010")
    private String numeroTarjeta;

    @NotBlank(message = "El titular de la tarjeta es obligatorio.")
    @Size(max = 150, message = "El nombre del titular excede el tamaño permitido.")
    @Schema(description = "Nombre impreso en la tarjeta", example = "CARLOS EDUARDO LOPEZ HERNANDEZ")
    private String titularTarjeta;

    @NotBlank(message = "La fecha de vencimiento es obligatoria.")
    @Pattern(regexp = "^(0[1-9]|1[0-2])\\/([0-9]{2})$", message = "Formato de fecha inválido. Utilizar MM/YY.")
    @Schema(description = "Mes y año de expiración (MM/YY)", example = "08/28")
    private String fechaExpiracion;

    @NotBlank(message = "El código CVV es obligatorio.")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "El CVV debe contener 3 o 4 dígitos.")
    @Schema(description = "Código de seguridad al reverso de la tarjeta", example = "123")
    private String cvv;

    @Min(value = 1, message = "Debe pagar al menos la primera cuota.")
    @Max(value = 5, message = "Máximo 5 cuotas mensuales por ciclo.")
    private Integer cuotasAPagar; // Opcional si liquida en línea: permite elegir cuántas liquida en ese instante
}