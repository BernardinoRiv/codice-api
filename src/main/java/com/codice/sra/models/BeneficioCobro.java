package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "beneficios_cobro")
@Data
public class BeneficioCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_beneficio_cobro")
    private Long idBeneficioCobro;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_beneficio", nullable = false)
    private Beneficio beneficio;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_cobro", nullable = false)
    private TipoCobro tipoCobro;
}
