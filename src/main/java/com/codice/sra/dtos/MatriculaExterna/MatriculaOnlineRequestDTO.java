package com.codice.sra.dtos.MatriculaExterna;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
        import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Payload para la solicitud de registro y pre-matrícula en línea")
public class MatriculaOnlineRequestDTO {

    @Schema(description = "ID de la persona si ya fue recuperada en la verificación civil. Nulo si es aspirante nuevo.", example = "null")
    private Long idPersona;

    @NotNull(message = "El tipo de documento es obligatorio.")
    @Schema(description = "ID del catálogo tipos_documento (1=DUI, 2=Pasaporte)", example = "1")
    private Long idTipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio.")
    @Pattern(regexp = "^[0-9A-Za-z-]{7,15}$", message = "Formato de documento de identidad inválido.")
    @Schema(description = "Número oficial de documento de identidad", example = "06123456-7")
    private String numeroDocumento;

    @NotBlank(message = "Los nombres son obligatorios.")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres.")
    @Schema(description = "Nombres del aspirante", example = "Carlos Eduardo")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios.")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres.")
    @Schema(description = "Apellidos del aspirante", example = "Lopez Hernandez")
    private String apellidos;

    @NotNull(message = "La fecha de nacimiento es obligatoria.")
    @Past(message = "La fecha de nacimiento debe pertenecer al pasado.")
    @Schema(description = "Fecha de nacimiento del aspirante", example = "2003-07-22")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El sexo es obligatorio.")
    @Pattern(regexp = "^(M|F)$", message = "El sexo debe ser 'M' o 'F'.")
    @Schema(description = "Sexo registrado (M o F)", example = "M")
    private String sexo;

    @NotBlank(message = "El teléfono móvil es obligatorio.")
    @Pattern(regexp = "^[0-9-]{8,15}$", message = "Formato de teléfono no válido.")
    @Schema(description = "Teléfono de contacto", example = "7890-1234")
    private String telefono;

    @NotBlank(message = "El correo electrónico personal es obligatorio.")
    @Email(message = "Formato de correo electrónico inválido.")
    @Size(max = 150, message = "El correo no puede exceder 150 caracteres.")
    @Schema(description = "Correo personal donde se remitirán credenciales y comprobantes", example = "carlos.lopez@gmail.com")
    private String correoPersonal;

    @NotNull(message = "El distrito de residencia es obligatorio.")
    @Schema(description = "ID del distrito territorial (fk hacia public.distritos)", example = "22")
    private Long idDistrito;

    @NotBlank(message = "La dirección de residencia es obligatoria.")
    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres.")
    @Schema(description = "Dirección domiciliar exacta", example = "Colonia El Palmar, Calle Principal #14, Santa Ana")
    private String direccion;

    // --- Oferta Académica ---

    @NotNull(message = "La sede es obligatoria.")
    @Schema(description = "ID de la sede universitaria seleccionada", example = "2")
    private Long idSede;

    @NotNull(message = "La carrera es obligatoria.")
    @Schema(description = "ID de la relación carrera_sede", example = "4")
    private Long idCarreraSede;

    @NotNull(message = "El pensum es obligatorio.")
    @Schema(description = "ID del pensum académico vigente", example = "7")
    private Long idPensum;

    @NotEmpty(message = "Debe seleccionar al menos un grupo de asignaturas.")
    @Schema(description = "Arreglo con los IDs de grupos a reservar para el Ciclo 1", example = "[10, 19, 20]")
    private List<Long> idGruposSeleccionados;

    // --- Modalidad de Pago ---

    @NotBlank(message = "Debe indicar la modalidad de pago.")
    @Pattern(regexp = "^(EN_LINEA|VENTANILLA)$", message = "La modalidad debe ser 'EN_LINEA' o 'VENTANILLA'.")
    @Schema(description = "Modo de liquidación elegido por el aspirante", example = "EN_LINEA")
    private String modalidadPago;

}