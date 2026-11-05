package com.codice.sra.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RestablecerClaveDTO {
    @NotBlank(message = "El correo es obligatorio")
    private String correoInstitucional;

    @NotBlank(message = "El código de recuperación es obligatorio")
    private String codigo;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String nuevaContrasena;

    @NotBlank(message = "Debe confirmar la nueva contraseña")
    private String confirmarContrasena;
}