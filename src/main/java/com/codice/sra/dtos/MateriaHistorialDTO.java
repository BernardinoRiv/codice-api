package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MateriaHistorialDTO {
    private String codigoMateria;
    private String nombreMateria;
    private int uv;
    private BigDecimal notaFinal;
    private String estado;
}