package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrupoResponseDTO {

    private Long idGrupo;
    private String codigoGrupo;
    private String nombreMateria;
    private String codigoMateria;
    private String nombreCiclo;
    private String nombreSede;
    private String nombreDocente;
    private String apellidoDocente;
}