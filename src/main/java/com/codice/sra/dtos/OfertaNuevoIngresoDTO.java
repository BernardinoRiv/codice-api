package com.codice.sra.dtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class OfertaNuevoIngresoDTO {

    private Long idMateria;
    private String codigoMateria;
    private String nombreMateria;
    private int unidadesValorativas;
    private List<GrupoDisponibleDTO> gruposDisponibles;

    @Data
    @Builder
    public static class GrupoDisponibleDTO {
        private Long idGrupo;
        private String codigoGrupo;
        private String nombreDocente;
        private int cupoMaximo;
        private int cuposDisponibles; // Calculado en tiempo real
        private List<HorarioDTO> horarios;
    }

    @Data
    @Builder
    public static class HorarioDTO {
        private String diaSemana;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private String modalidad;
        private String aula; // Puede ser null si es virtual
        private String enlaceVirtual;
    }
}