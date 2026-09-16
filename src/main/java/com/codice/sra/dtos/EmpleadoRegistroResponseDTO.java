package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmpleadoRegistroResponseDTO {
    private Long idEmpleado;
    private String codigoEmpleado;
    private String nombres;
    private String apellidos;
    private String correoInstitucional;
    private String mensaje;
}