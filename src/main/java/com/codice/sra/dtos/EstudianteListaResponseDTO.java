package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class EstudianteListaResponseDTO {

    private Long idEstudiante;
    private Long idUsuario;
    private String carnet;
    private String nombres;
    private String apellidos;
    private String numeroDocumento;
    private String correoPersonal;
    private String correoInstitucional;
    private String telefono;
    private String direccion;
    private Long idDistrito;
    private String estadoEstudiante;
    private String estadoUsuario;
    private LocalDate fechaIngreso;
    private List<EstudianteCarreraListaDTO> carreras;
    private LocalDate fechaNacimiento;
    private String sexo;
    private Long idDepartamento;
    private String nombreDepartamento;
    private String nombreDistrito;
}