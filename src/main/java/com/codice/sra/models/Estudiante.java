package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "estudiantes")
@Data
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante")
    private Long idEstudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_estudiante", nullable = false)
    private EstadoEstudiante estadoEstudiante;

    @Column(name = "carnet", nullable = false, unique = true, length = 20)
    private String carnet;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;
}