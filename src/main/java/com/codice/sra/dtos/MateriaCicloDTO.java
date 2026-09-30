package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MateriaCicloDTO {
    private String codigoCiclo;
    private int anio;
    private int numeroCiclo;
    private String estadoCiclo;
    private List<MateriaHistorialDTO> materias;
}