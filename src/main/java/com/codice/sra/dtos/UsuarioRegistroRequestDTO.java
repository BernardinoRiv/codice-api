package com.codice.sra.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UsuarioRegistroRequestDTO {

    @NotNull(message = "El identificador de la persona es obligatorio")
    @Positive(message = "El ID de la persona debe ser un número positivo")
    private Long idPersona;

    @NotNull(message = "El rol es obligatorio")
    @Positive(message = "El ID del rol debe ser un número positivo")
    private Long idRol;
}