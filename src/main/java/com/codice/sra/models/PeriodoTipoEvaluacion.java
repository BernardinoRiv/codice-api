package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.math.BigDecimal;

@Entity
@Table(name = "periodos_tipos_evaluacion")
@Data
public class PeriodoTipoEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periodo_tipo_evaluacion")
    private Long idPeriodoTipoEvaluacion;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_periodo_evaluacion", nullable = false)
    private PeriodoEvaluacion periodoEvaluacion;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evaluacion", nullable = false)
    private TipoEvaluacion tipoEvaluacion;

    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;
}