
package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EmpleadoEstadoRequestDTO {

    @NotNull(message = "El identificador del estado es obligatorio")
    @Positive(message = "El ID del estado debe ser positivo")
    private Long idEstadoEmpleado;

    @NotBlank(message = "Debe proporcionar un motivo para el cambio de estado")
    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    private String motivo;
}

