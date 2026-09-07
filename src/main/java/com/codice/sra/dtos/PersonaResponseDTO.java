package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonaResponseDTO {
    private Long idPersona;
    private String documento;
    private String nombres;
    private String apellidos;
    private String mensaje;
}