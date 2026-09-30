package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // <--- Esto genera el constructor de 6 parámetros automáticamente
@NoArgsConstructor
public class PersonaResponseDTO {
    private Long idPersona;
    private String numeroDocumento; // Cambiado de 'documento' a 'numeroDocumento'
    private String nombres;
    private String apellidos;
    private String estadoRegistro;  // Nuevo campo para mostrar el estado
    private String mensaje;
}