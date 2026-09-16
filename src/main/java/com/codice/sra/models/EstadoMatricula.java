package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_matricula")
@Data
public class EstadoMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_matricula")
    private Long idEstadoMatricula;

    @Column(name = "estado_matricula", nullable = false, unique = true, length = 50)
    private String estadoMatricula;
}