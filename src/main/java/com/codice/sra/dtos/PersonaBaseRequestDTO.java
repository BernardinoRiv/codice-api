package com.codice.sra.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PersonaBaseRequestDTO implements PersonaInputDTO {

    @NotNull(message = "El tipo de documento es obligatorio")
    private Long idTipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio.")
    @Pattern(
            regexp = "^[0-9A-Za-z-]{7,17}$",
            message = "Formato de documento de identidad inválido."
    )
    @Schema(description = "Número oficial de documento de identidad", example = "06123456-7")
    private String numeroDocumento;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    private String apellidos;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @Size(max = 25, message = "El teléfono no puede exceder 25 caracteres")
    private String telefono;

    @Size(max = 150, message = "El correo personal no puede exceder 150 caracteres")
    @Email(message = "El formato del correo personal no es válido")
    private String correoPersonal;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
    private String direccion;

    @NotBlank(message = "El sexo es obligatorio.")
    @Pattern(regexp = "^[MF]$", message = "El sexo debe ser 'M' (Masculino) o 'F' (Femenino).")
    private String sexo;

    @NotNull(message = "El distrito de residencia es obligatorio.")
    private Long idDistrito;
}