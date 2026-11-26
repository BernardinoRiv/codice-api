package com.codice.sra.dtos;

import lombok.Data;
import java.util.List;

@Data
public class SolicitudModificacionNotaDTO {
    private Long idGrupo;
    private Long idTipoEvaluacion;
    private String tipoEvaluacion;
    private Integer numeroEvaluacion;
    private List<Long> idsInscripcion;
    private Boolean todosLosAlumnos;
    private String motivo;
}
