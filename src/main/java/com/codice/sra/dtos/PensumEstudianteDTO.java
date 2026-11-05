package com.codice.sra.dtos;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PensumEstudianteDTO {
    private Integer materiasAprobadas;
    private Integer materiasCursando;
    private Integer materiasPendientes;
    private Map<Integer, List<MateriaPensumDTO>> materiasPorCiclo;
}