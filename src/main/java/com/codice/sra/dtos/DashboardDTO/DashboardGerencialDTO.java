package com.codice.sra.dtos.DashboardDTO;

import com.codice.sra.dtos.DashboardDTO.DistribucionProjection;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter // Necesario para poder inyectar las listas desde el servicio
public class DashboardGerencialDTO {
    private String cicloActivo;
    private int matriculaActiva;
    private int docentesActivos;
    private int seccionesAbiertas;
    private double capacidadPromedio;
    private BigDecimal totalFacturado;
    private BigDecimal totalRecaudado;
    private double porcentajeCobro;

    // Buena Práctica: Inicializar para garantizar que el JSON envíe "[]" en lugar de "null"
    private List<DistribucionProjection> distribucionFacultades = new ArrayList<>();
    private List<DistribucionProjection> distribucionContratos = new ArrayList<>();
    private List<AsistenciaSemanalProjection> asistenciaSemanal = new ArrayList<>();

    public DashboardGerencialDTO(String cicloActivo, int matricula, int docentes, int secciones, double capacidad, BigDecimal facturado, BigDecimal recaudado) {
        this.cicloActivo = cicloActivo;
        this.matriculaActiva = matricula;
        this.docentesActivos = docentes;
        this.seccionesAbiertas = secciones;
        this.capacidadPromedio = capacidad;

        this.totalFacturado = facturado != null ? facturado : BigDecimal.ZERO;
        this.totalRecaudado = recaudado != null ? recaudado : BigDecimal.ZERO;

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