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
@Schema(description = "Contrato de entrada para el alta de una nueva carrera universitaria")
public class CarreraCreacionRequestDTO {

    @Schema(description = "Nombre oficial de la carrera", example = "Licenciatura en Ciberseguridad")
    @NotBlank(message = "El nombre de la carrera es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe contener entre 3 y 150 caracteres")
    private String nombreCarrera;

    @Schema(description = "Identificador de la facultad asignada", example = "1")
    @NotNull(message = "La facultad es obligatoria")
    @Positive(message = "El ID de la facultad debe ser un número positivo")
    private Long idFacultad;

    @Schema(description = "Identificador del nivel académico (1: Técnico, 2: Licenciatura, 3: Maestría)", example = "2")
    @NotNull(message = "El nivel académico es obligatorio")
    @Positive(message = "El ID del nivel académico debe ser un número positivo")
    private Long idNivel;
}