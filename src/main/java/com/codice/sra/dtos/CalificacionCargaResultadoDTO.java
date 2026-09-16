package com.codice.sra.dtos;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CalificacionCargaResultadoDTO {
    private Integer totalProcesadas;
    private Integer exitosas;
    private Integer fallidas;
    private List<String> errores;
}
