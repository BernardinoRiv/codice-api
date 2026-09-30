package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocenteRegistroResponseDTO {
    private Long idDocente;
    private String codigoDocente;
    private String nombres;
    private String apellidos;
    private String correoInstitucional;
    private String mensaje;
}