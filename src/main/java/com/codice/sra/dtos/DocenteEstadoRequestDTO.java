package com.codice.sra.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contrato formal para la transición de estado operativo de un docente")
public class DocenteEstadoRequestDTO {

    @Schema(description = "ID del estado de destino (Catálogo EstadoDocente)", example = "2")
    @NotNull(message = "El identificador del estado es obligatorio")
    @Positive(message = "El ID del estado debe ser positivo")
    private Long idEstadoDocente;

    @Schema(description = "Motivo administrativo del cambio de estado", example = "Suspensión administrativa por finalización de ciclo")
    @NotBlank(message = "Debe proporcionar una justificación o motivo del cambio de estado")
    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String motivo;
}