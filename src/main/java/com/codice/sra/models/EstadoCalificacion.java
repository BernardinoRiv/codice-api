package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_calificacion")
@Data
public class EstadoCalificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_calificacion")
    private Long idEstadoCalificacion;

    @Column(name = "estado_calificacion", nullable = false, unique = true, length = 50)
    private String estadoCalificacion;
}