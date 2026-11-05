package com.codice.sra.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmpleadoRegistroRequestDTO extends PersonaBaseRequestDTO {

    @NotNull(message = "El área es obligatoria")
    private Long idArea;

    @NotNull(message = "El identificador de la sede regional es obligatorio.")
    @Positive(message = "El identificador de la sede debe ser un valor positivo.")
    private Long idSede;

    @NotNull(message = "El cargo es obligatorio")
    private Long idCargo;

    @FutureOrPresent(message = "La fecha de fin de contrato no puede estar en el pasado")
    private LocalDate fechaFin; // Opcional (null = contrato indefinido)

    @AssertTrue(message = "La fecha de fin de contrato no puede ser anterior a la fecha de ingreso actual")
    public boolean isFechasValidas() {
        if (fechaFin == null) {
            return true;
        }
        return !fechaFin.isBefore(LocalDate.now());
    }
}