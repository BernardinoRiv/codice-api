package com.codice.sra.dtos.MatriculaExterna;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta sanitizada de verificación de identidad para aspirantes")
public class VerificacionIdentidadPublicaResponseDTO {

    @Schema(description = "Indica si la persona ya cuenta con registro civil en la institución", example = "true")
    private boolean existePersona;

    @Schema(description = "Indica si ya es un estudiante activo con credenciales asignadas", example = "false")
    private boolean esEstudianteActivo;

    @Schema(description = "Determina si el usuario debe ser redirigido a la pantalla de login institucional", example = "false")
    private boolean requiereLogin;

    @Schema(description = "Mensaje contextual para orientar al aspirante en la interfaz",
            example = "Identidad validada. Puedes continuar con la selección académica.")
    private String mensaje;

    @Schema(description = "Identificador de la persona en el censo (nulo si no existe)", example = "10")
    private Long idPersona;

    @Schema(description = "Nombres de la persona", example = "Carlos Eduardo")
    private String nombres;

    @Schema(description = "Apellidos de la persona", example = "Lopez Hernandez")
    private String apellidos;

    @Schema(description = "Correo personal enmascarado para validación de seguridad", example = "c****z@gmail.com")
    private String correoEnmascarado;

    @Schema(description = "Teléfono móvil enmascarado", example = "****-1234")
    private String telefonoEnmascarado;
}