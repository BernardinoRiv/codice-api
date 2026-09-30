package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InscripcionResponseDTO {
    private Long idInscripcion;
    private String carnet;
    private String nombresEstudiante;
    private String apellidosEstudiante;
}