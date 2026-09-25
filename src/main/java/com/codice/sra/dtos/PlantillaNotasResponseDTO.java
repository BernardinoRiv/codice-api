package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlantillaNotasResponseDTO {
    private String nombreArchivo;
    private byte[] contenido;
    private String tipoContenido;
}