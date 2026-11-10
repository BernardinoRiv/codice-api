
package com.codice.sra.dtos.DashboardDTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DashboardGerencialDTO {

    // KPIs principales
    private Long matriculaActiva;
    private Long matriculadosConMaterias = 0L;
    private Long matriculadosSinMaterias = 0L;

    private Long docentesActivos;
    private Long seccionesAbiertas;
    private double capacidadPromedio;

    private BigDecimal totalFacturado;
    private BigDecimal totalRecaudado;
    private double porcentajeCobro;

    // Distribuciones de matrícula
    private List<DistribucionProjection> distribucionMatriculaFacultad = new ArrayList<>();
    private List<CarreraConMateriasDTO> distribucionMatriculaCarrera = new ArrayList<>();
    private List<HistoricoMatriculaProjection> historicoMatricula = new ArrayList<>();
    private List<DistribucionProjection> distribucionMatriculaMateria = new ArrayList<>();

    @Getter
    @Setter
    public static class CarreraConMateriasDTO {
        private Long idCarrera;
        private String nombre;
        private Long cantidad;
        private List<MateriaDTO> materias = new ArrayList<>();

        public CarreraConMateriasDTO(Long idCarrera, String nombre, Long cantidad) {
            this.idCarrera = idCarrera;
            this.nombre = nombre;
            this.cantidad = cantidad;
        }
    }

    @Getter
    @Setter
    public static class MateriaDTO {
        private Long idMateria;
        private String nombre;
        private Long cantidad;

        public MateriaDTO(Long idMateria, String nombre, Long cantidad) {
            this.idMateria = idMateria;
            this.nombre = nombre;
            this.cantidad = cantidad;
        }
    }

    // Distribuciones de docentes
    private List<DistribucionProjection> distribucionFacultades = new ArrayList<>();
    private List<DistribucionProjection> distribucionContratos = new ArrayList<>();

    // Asistencia
    private List<AsistenciaSemanalProjection> asistenciaSemanal = new ArrayList<>();

    public DashboardGerencialDTO(
            Long matricula,
            Long docentes,
            Long secciones,
            double capacidad,
            BigDecimal facturado,
            BigDecimal recaudado
    ) {
        this.matriculaActiva = matricula != null ? matricula : 0L;
        this.docentesActivos = docentes != null ? docentes : 0L;
        this.seccionesAbiertas = secciones != null ? secciones : 0L;
        this.capacidadPromedio = capacidad;

        this.totalFacturado = facturado != null
                ? facturado : BigDecimal.ZERO;

        this.totalRecaudado = recaudado != null
                ? recaudado : BigDecimal.ZERO;

        if (this.totalFacturado.compareTo(BigDecimal.ZERO) > 0) {
            this.porcentajeCobro = this.totalRecaudado
                    .divide(this.totalFacturado, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        } else {
            this.porcentajeCobro = 0.0;
        }
    }
}
