package com.codice.sra.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocenteRegistroRequestDTO extends PersonaBaseRequestDTO {

    @NotNull(message = "La sede es obligatoria")
    @Positive
    private Long idSede;

    @NotNull(message = "El tipo de contratación es obligatorio")
    @Positive
    private Long idTipoContratacion;

    @Size(max = 100, message = "La especialidad no puede exceder 100 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{M} .,'’()&/+0-9-]*$", message = "La especialidad contiene caracteres inválidos.")
    private String especialidad;

    @Override
    @Pattern(regexp = EmpleadoRegistroRequestDTO.FORMATO_TELEFONO, message = "El teléfono debe tener ocho dígitos y comenzar con 2, 6 o 7.")
    public String getTelefono() {
        return super.getTelefono();
    }

    @Override
    @Size(max = 255, message = "La dirección no puede superar 255 caracteres.")
    @Pattern(regexp = EmpleadoRegistroRequestDTO.FORMATO_DIRECCION, message = "Escribe una dirección válida.")
    public String getDireccion() {
        return super.getDireccion();
    }

    @Override
    @Pattern(regexp = EmpleadoDatosPersonalesDTO.FORMATO_NOMBRE, message = "Escribe los nombres con letras válidas.")
    public String getNombres() {
        return super.getNombres();
    }

    @Override
    @Pattern(regexp = EmpleadoDatosPersonalesDTO.FORMATO_NOMBRE, message = "Escribe los apellidos con letras válidas.")
    public String getApellidos() {
        return super.getApellidos();
    }

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