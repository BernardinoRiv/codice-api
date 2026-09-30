package com.codice.sra.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocenteRegistroRequestDTO extends PersonaBaseRequestDTO {

    @NotNull(message = "La sede es obligatoria")
    private Long idSede;

    @NotNull(message = "El tipo de contratación es obligatorio")
    private Long idTipoContratacion;

    @Size(max = 100, message = "La especialidad no puede exceder 100 caracteres")
    private String especialidad;

    @NotNull(message = "La fecha de inicio de contrato es obligatoria.")
    private LocalDate fechaInicio;

    private LocalDate fechaFin; // Opcional (null indica contrato indefinido)

    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio.")
    public boolean isFechasValidas() {
        if (fechaInicio == null || fechaFin == null) {
            return true;
        }
        return !fechaFin.isBefore(fechaInicio);
    }
}