
package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EmpleadoDatosLaboralesDTO {

    @NotNull(message = "El área es obligatoria")
    @Positive
    private Long idArea;

    @NotNull(message = "El cargo es obligatorio")
    @Positive
    private Long idCargo;

    @NotNull(message = "La sede es obligatoria")
    @Positive
    private Long idSede;

    private LocalDate fechaFin;
}
