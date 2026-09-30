package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialAcademicoDTO {

    private BigDecimal cumAcumulado;
    private int uvAcumuladas;
    private int totalMateriasAprobadas;
    private int totalMateriasReprobadas;
    private List<MateriaCicloDTO> materiasPorCiclo;
}