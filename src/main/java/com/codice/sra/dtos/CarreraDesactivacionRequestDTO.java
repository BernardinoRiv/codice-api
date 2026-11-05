package com.codice.sra.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contrato de entrada para la desactivación o baja lógica de una carrera académica")
public class CarreraDesactivacionRequestDTO {

    @Schema(
            description = "Motivo administrativo o justificación formal del cierre/suspensión de la carrera",
            example = "Cierre programado por actualización de oferta académica institucional y migración a nuevo plan."
    )
    @NotBlank(message = "El motivo de la desactivación es obligatorio")
    @Size(min = 10, max = 250, message = "El motivo debe contener entre 10 y 250 caracteres")
    private String motivo;
}