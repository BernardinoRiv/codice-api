package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_materia_estudiante")
@Data
public class EstadoMateriaEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_materia")
    private Long idEstadoMateria;

    @Column(name = "estado_materia", nullable = false, unique = true, length = 50)
    private String estadoMateria;
}