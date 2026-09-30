package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CambiarContrasenaRequestDTO {

    @NotBlank(message = "La contraseña actual es requerida")
    private String contrasenaActual;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String nuevaContrasena;

    @NotBlank(message = "La confirmación de contraseña es requerida")
    private String confirmarNuevaContrasena;
}