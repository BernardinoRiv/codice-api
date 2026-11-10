package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MateriaNotasDTO {
    private String codigo;
    private String nombre;
    private NotasEvaluacionesDTO notas;
    private BigDecimal promedioSinRedondear;
    private BigDecimal promedioOficial;
    private String estado;
    private List<HorarioDTO> horarios;
}