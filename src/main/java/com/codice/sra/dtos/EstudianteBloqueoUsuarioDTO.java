
package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EstudianteBloqueoUsuarioDTO {

    @NotBlank
    @Size(min = 5, max = 500)
    private String motivo;
}
