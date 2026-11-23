
package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EstudianteDatosPersonalesDTO {

    @NotBlank
    @Size(max = 100)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    private String apellidos;

    @NotNull
    @Past
    private LocalDate fechaNacimiento;

    @NotBlank
    @Pattern(regexp = "^[MFmf]$", message = "El sexo debe ser M o F")
    private String sexo;

    @Pattern(regexp = "^$|^[67][0-9]{3}-?[0-9]{4}$",
            message = "El teléfono debe tener 8 dígitos e iniciar con 6 o 7")
    private String telefono;

    @Email
    @Size(max = 150)
    private String correoPersonal;

    @NotBlank
    private String direccion;

    @NotNull
    private Long idDistrito;
}
