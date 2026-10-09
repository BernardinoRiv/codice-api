package com.codice.sra.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contrato base con datos civiles y de contacto editables de una persona")
public class PersonaContactoEdicionDTO {

    @Schema(description = "Teléfono de contacto en El Salvador", example = "7890-1234")
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(
            regexp = "^[267][0-9]{3}-?[0-9]{4}$",
            message = "El formato de teléfono debe ser salvadoreño válido (ej: 7890-1234 o 78901234)"
    )
    private String telefono;

    @Schema(description = "Correo electrónico personal", example = "contacto.personal@gmail.com")
    @NotBlank(message = "El correo personal es obligatorio")
    @Size(max = 150, message = "El correo personal no puede exceder 150 caracteres")
    @Email(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "El formato del correo personal no es válido")
    private String correoPersonal;

    @Schema(description = "Dirección de residencia", example = "Colonia San José #45, Santa Ana")
    @NotBlank(message = "La dirección de residencia es obligatoria")
    @Size(max = 250, message = "La dirección no puede exceder 250 caracteres")
    private String direccion;

    @Schema(description = "Identificador del distrito de residencia", example = "1")
    @NotNull(message = "El distrito es obligatorio")
    @Positive(message = "El ID de distrito debe ser positivo")
    private Long idDistrito;
}