package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recargos")
@Data
public class Recargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recargo")
    private Long idRecargo;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_cobro", nullable = false)
    private TipoCobro tipoCobro;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_recargo", nullable = false)
    private EstadoRecargo estadoRecargo;

    @Column(name = "recargo", nullable = false, precision = 14, scale = 2)
    private BigDecimal recargo;

    @Column(name = "dias_gracia", nullable = false)
    private Integer diasGracia = 0;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
}
