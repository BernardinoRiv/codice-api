package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "estudiantes_carreras")
@Data
public class EstudianteCarrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante_carrera")
    private Long idEstudianteCarrera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carrera_sede", nullable = false)
    private CarreraSede carreraSede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pensum", nullable = false)
    private Pensum pensum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_trayectoria", nullable = false)
    private EstadoTrayectoria estadoTrayectoria;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
}