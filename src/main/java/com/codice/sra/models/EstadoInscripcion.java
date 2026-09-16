package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_inscripcion")
@Data
public class EstadoInscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_inscripcion")
    private Long idEstadoInscripcion;

    @Column(name = "estado_inscripcion", nullable = false, unique = true, length = 50)
    private String estadoInscripcion;
}