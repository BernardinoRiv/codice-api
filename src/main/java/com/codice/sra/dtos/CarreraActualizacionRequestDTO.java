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
@Schema(description = "Contrato formal para la actualización de datos generales de una carrera (HU-ADM-05)")
public class CarreraActualizacionRequestDTO {

    @Schema(description = "Nombre oficial corregido o actualizado", example = "Licenciatura en Informática y Redes")
    @NotBlank(message = "El nombre de la carrera es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe contener entre 3 y 150 caracteres")
    private String nombreCarrera;

    @Schema(description = "ID de la facultad a la que se adscribe la carrera", example = "1")
    @NotNull(message = "La facultad es obligatoria")
    @Positive(message = "El ID de la facultad debe ser positivo")
    private Long idFacultad;

    @Schema(description = "ID del nivel académico correspondiente", example = "2")
    @NotNull(message = "El nivel académico es obligatorio")
    @Positive(message = "El ID del nivel académico debe ser positivo")
    private Long idNivel;
}