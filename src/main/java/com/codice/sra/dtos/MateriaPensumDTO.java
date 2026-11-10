package com.codice.sra.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MateriaPensumDTO {
    private String codigo;
    private String nombre;
    private BigDecimal uvs;
    private Integer cicloRecomendado;
    private String estado;
    private List<String> prerrequisitos;
}