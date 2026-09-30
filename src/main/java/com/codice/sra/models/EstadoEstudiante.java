package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_estudiante")
@Data
public class EstadoEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_estudiante")
    private Long idEstadoEstudiante;

    @Column(name = "estado_estudiante", nullable = false, unique = true, length = 50)
    private String estadoEstudiante;
}