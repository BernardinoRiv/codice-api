package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_carrera")
@Data
public class EstadoCarrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_carrera")
    private Long idEstadoCarrera;

    @Column(name = "estado_carrera", nullable = false, unique = true, length = 50)
    private String estadoCarrera;

    @Column(nullable = false)
    private Boolean activo = true;
}