
package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class EmpleadoListaResponseDTO {

    private Long idEmpleado;
    private Long idUsuario;
    private String codigoEmpleado;
    private String nombres;
    private String apellidos;
    private String numeroDocumento;
    private Long idTipoDocumento;
    private String correoPersonal;
    private String correoInstitucional;
    private String telefono;
    private String direccion;
    private Long idDistrito;
    private String estadoEmpleado;
    private String estadoUsuario;
    private LocalDate fechaIngreso;
    private LocalDate fechaFin;
    private LocalDate fechaNacimiento;
    private String sexo;
    private Long idDepartamento;
    private String nombreDepartamento;
    private String nombreDistrito;
    private Long idArea;
    private String nombreArea;
    private Long idCargo;
    private String nombreCargo;
    private Long idSede;
    private String nombreSede;
}