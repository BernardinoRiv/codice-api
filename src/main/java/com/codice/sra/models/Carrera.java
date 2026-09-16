package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "carreras")
@Data
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Long idCarrera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_facultad", nullable = false)
    private Facultad facultad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel", nullable = false)
    private NivelAcademico nivel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_carrera", nullable = false)
    private EstadoCarrera estadoCarrera;

    @Column(name = "codigo_carrera", nullable = false, unique = true, length = 20)
    private String codigoCarrera;

    @Column(name = "nombre_carrera", nullable = false, unique = true, length = 150)
    private String nombreCarrera;
}