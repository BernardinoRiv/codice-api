
package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EmpleadoBloqueoUsuarioDTO {

    @NotBlank
    @Size(min = 5, max = 100)
    private String motivo;
}
