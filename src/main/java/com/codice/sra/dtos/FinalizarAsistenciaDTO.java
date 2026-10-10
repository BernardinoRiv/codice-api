package com.codice.sra.dtos;

import lombok.Data;
import java.util.List;

@Data
public class FinalizarAsistenciaDTO {
    private List<ActualizacionManualDTO> manuales;

    @Data
    public static class ActualizacionManualDTO {
        private Long idInscripcion; // Clave única garantizada
        private String estado;
    }
}