package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_facultad")
@Data
public class EstadoFacultad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_facultad")
    private Long idEstadoFacultad;

    @Column(name = "estado_facultad", nullable = false, unique = true, length = 50)
    private String estadoFacultad;
}