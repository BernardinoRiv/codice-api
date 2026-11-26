package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class NuevoIngresoRequestDTO {

    // 1. Datos de Identidad y Contacto
    @Positive
    private Long idPersona;
    @NotNull @Positive
    private Long idTipoDocumento;
    @NotBlank @Pattern(regexp = "^[0-9A-Za-z-]{7,17}$", message = "Formato de documento inválido.")
    private String numeroDocumento;
    @NotBlank @Size(max = 100) @Pattern(regexp = EmpleadoDatosPersonalesDTO.FORMATO_NOMBRE, message = "Escribe los nombres con letras válidas.")
    private String nombres;
    @NotBlank @Size(max = 100) @Pattern(regexp = EmpleadoDatosPersonalesDTO.FORMATO_NOMBRE, message = "Escribe los apellidos con letras válidas.")
    private String apellidos;
    @NotNull @Past
    private LocalDate fechaNacimiento;
    @NotBlank @Pattern(regexp = "[MF]", message = "Selecciona un sexo válido.")
    private String sexo;
    @NotBlank @Pattern(regexp = EmpleadoRegistroRequestDTO.FORMATO_TELEFONO, message = "El teléfono debe tener ocho dígitos y comenzar con 2, 6 o 7.")
    private String telefono;
    @NotBlank @Email @Size(max = 150)
    private String correoPersonal;
    @NotNull @Positive
    private Long idDistrito;
    @NotBlank @Size(max = 255)
    @Pattern(regexp = EmpleadoRegistroRequestDTO.FORMATO_DIRECCION, message = "Escribe una dirección válida.")
    private String direccion;

    // 2. Datos Institucionales y Académicos
    @NotNull @Positive
    private Long idSede;
    @NotNull @Positive
    private Long idCarreraSede;
    @NotNull @Positive
    private Long idPensum;

    // 3. Selección de Horarios
    @NotEmpty
    private List<@NotNull @Positive Long> idGruposSeleccionados;

    // 4. Modalidad Financiera
    // 1 = Ninguna/Regular, 2 = Franja Becaria, 3 = Beca Completa U, 4 = Beca Terceros Directo
    @Positive
    private Integer tipoBeca;
}