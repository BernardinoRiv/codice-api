package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "periodos_evaluacion")
@Data
public class PeriodoEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periodo_evaluacion")
    private Long idPeriodoEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ciclo", nullable = false)
    private Ciclo ciclo;

    @Column(name = "periodo", nullable = false)
    private Integer periodo;

    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;
}