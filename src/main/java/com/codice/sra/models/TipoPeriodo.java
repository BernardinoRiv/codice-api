package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tipos_periodo")
@Data
public class TipoPeriodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_periodo")
    private Long idTipoPeriodo;

    @Column(name = "tipo_periodo", nullable = false, unique = true, length = 50)
    private String tipoPeriodo;
}