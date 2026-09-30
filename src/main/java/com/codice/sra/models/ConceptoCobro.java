package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "conceptos_cobro")
@Data
public class ConceptoCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_concepto_cobro")
    private Long idConceptoCobro;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ciclo", nullable = false)
    private Ciclo ciclo;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_cobro", nullable = false)
    private TipoCobro tipoCobro;

    @Column(name = "monto_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoBase;
}
