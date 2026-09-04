package com.codice.sra.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginRequestDTO {

    //Este objeto será el encargado de recibir de forma segura las credenciales del usuario.
    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "El formato del correo no es válido")
    private String correoInstitucional;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}