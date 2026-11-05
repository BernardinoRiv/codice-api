package com.codice.sra.dtos;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class NuevoIngresoRequestDTO {

    // 1. Datos de Identidad y Contacto (Si idPersona es null, se crea. Si trae ID, se actualiza)
    private Long idPersona;
    private Long idTipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String telefono;
    private String correoPersonal;
    private Long idDistrito;
    private String direccion;

    // 2. Datos Institucionales y Académicos
    private Long idSede;
    private Long idCarreraSede;
    private Long idPensum;

    // 3. Selección de Horarios
    // Lista de los IDs de los grupos que el estudiante eligió para su ciclo 1
    private List<Long> idGruposSeleccionados;
}