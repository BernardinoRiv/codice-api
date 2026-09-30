package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_edificio")
@Data
public class EstadoEdificio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_edificio")
    private Long idEstadoEdificio;

    @Column(name = "estado_edificio", nullable = false, unique = true, length = 50)
    private String estadoEdificio;
}