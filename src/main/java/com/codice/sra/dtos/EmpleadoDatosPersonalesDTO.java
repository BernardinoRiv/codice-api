
package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EmpleadoDatosPersonalesDTO {

    static final String FORMATO_NOMBRE = "^ *\\p{L}[\\p{L}\\p{M}]*(?:[ ’'-]\\p{L}[\\p{L}\\p{M}]*)* *$";

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = FORMATO_NOMBRE, message = "Escribe los nombres con letras. Puedes usar espacios, guiones o apóstrofos.")
    private String nombres;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = FORMATO_NOMBRE, message = "Escribe los apellidos con letras. Puedes usar espacios, guiones o apóstrofos.")
    private String apellidos;

    @NotNull
    @Past
    private LocalDate fechaNacimiento;

    @NotBlank
    @Pattern(regexp = "^[MFmf]$", message = "El sexo debe ser M o F")
    private String sexo;

    @Pattern(regexp = "^$|^[267][0-9]{3}-?[0-9]{4}$",
            message = "El teléfono debe tener 8 dígitos e iniciar con 2, 6 o 7")
    private String telefono;

    @Email
    @Size(max = 150)
    private String correoPersonal;

    @NotBlank
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres.")
    private String direccion;

    @NotNull
    @Positive
    private Long idDistrito;
}