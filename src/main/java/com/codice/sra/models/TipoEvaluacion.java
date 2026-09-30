package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tipos_evaluacion")
@Data
public class TipoEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_evaluacion")
    private Long idTipoEvaluacion;

    @Column(name = "tipo_evaluacion", nullable = false, unique = true, length = 50)
    private String tipoEvaluacion;
}