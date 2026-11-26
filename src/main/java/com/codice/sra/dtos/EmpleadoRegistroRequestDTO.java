package com.codice.sra.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    static final String FORMATO_TELEFONO = "[267][0-9]{3}-?[0-9]{4}";
    static final String FORMATO_DIRECCION = "(?s)(?=.*[\\p{L}\\p{N}])[\\p{L}\\p{M}\\p{N}\\s#.,;:'’\"()&°ºª/+-]+";

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
    @Pattern(regexp = EmpleadoDatosPersonalesDTO.FORMATO_NOMBRE, message = "Escribe los nombres con letras. Puedes usar espacios, guiones o apóstrofos.")
    public String getNombres() {
        return super.getNombres();
    }

    @Override
    @Pattern(regexp = EmpleadoDatosPersonalesDTO.FORMATO_NOMBRE, message = "Escribe los apellidos con letras. Puedes usar espacios, guiones o apóstrofos.")
    public String getApellidos() {
        return super.getApellidos();
    }

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