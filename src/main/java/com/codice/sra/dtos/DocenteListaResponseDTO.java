package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocenteListaResponseDTO {

    private Long idDocente;
    private String codigoDocente;
    private String nombres;
    private String apellidos;
    private String correoInstitucional;
    private String especialidad;
    private String nombreSede;
    private String tipoContratacion;
    private String estadoDocente;
    private String telefono;
    private String correoPersonal;
    private String direccion;
    private Long idDistrito;
    private Long idSede;
    private Long idTipoContratacion;
    private String numeroDocumento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}