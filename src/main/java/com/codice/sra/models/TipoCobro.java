package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tipos_cobro")
@Data
public class TipoCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_cobro")
    private Long idTipoCobro;

    @Column(name = "tipo_cobro", nullable = false, unique = true, length = 80)
    private String tipoCobro;
}
