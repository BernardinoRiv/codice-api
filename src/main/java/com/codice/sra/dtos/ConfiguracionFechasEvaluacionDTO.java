package com.codice.sra.dtos;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ConfiguracionFechasEvaluacionDTO {
    private Map<String, RangoFechas> fechas;

    @Data
    public static class RangoFechas {
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
    }
}
