package com.codice.sra.dtos;

import lombok.Data;
import java.util.List;

@Data
public class DocenteDashboardResponseDTO {
    private int clasesHoy;
    private int evaluacionesPendientes;
    private String proximaEvaluacionTitulo;
    private String proximaEvaluacionFecha;
    private List<GrupoAsignadoDTO> gruposAsignados;

    @Data
    public static class GrupoAsignadoDTO {
        private Long idGrupo;
        private String codigoGrupo;
        private String materia;
        private long inscritos;
        private List<HorarioGrupoDTO> horarios;
    }

    @Data
    public static class HorarioGrupoDTO {
        private String dia;
        private String horaInicio;
        private String horaFin;
        private String aula;
    }
}