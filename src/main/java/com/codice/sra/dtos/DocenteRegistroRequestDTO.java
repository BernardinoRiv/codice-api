package com.codice.sra.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocenteRegistroRequestDTO extends PersonaBaseRequestDTO {

    @NotNull(message = "La sede es obligatoria")
    private Long idSede;

    @NotNull(message = "El tipo de contratación es obligatorio")
    private Long idTipoContratacion;

    @Size(max = 100, message = "La especialidad no puede exceder 100 caracteres")
    private String especialidad;
}