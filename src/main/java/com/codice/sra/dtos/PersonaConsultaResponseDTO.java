package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaConsultaResponseDTO {
    private Long idPersona;
    private Long idTipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String correoPersonal;
    private String direccion;
    private List<String> rolesActivos; // Ej: ["ESTUDIANTE"]

    private Long idSede;
    private String nombreSede;
    private Long idTipoContratacion;
    private String tipoContratacion;
    private Long idEspecialidad;
    private String especialidad;
}