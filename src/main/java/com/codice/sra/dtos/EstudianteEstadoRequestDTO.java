package com.codice.sra.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EstudianteEstadoRequestDTO {

    @NotNull(message = "El identificador del estado es obligatorio")
    @Positive(message = "El ID del estado debe ser positivo")
    private Long idEstadoEstudiante;

    @NotBlank(message = "Debe proporcionar un motivo para el cambio de estado")
    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String motivo;
}
