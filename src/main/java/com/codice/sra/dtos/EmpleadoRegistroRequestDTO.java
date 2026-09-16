package com.codice.sra.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmpleadoRegistroRequestDTO extends PersonaBaseRequestDTO {

    @NotNull(message = "El área es obligatoria")
    private Long idArea;

    @NotNull(message = "El cargo es obligatorio")
    private Long idCargo;
}