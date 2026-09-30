package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "facultades")
@Data
public class Facultad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_facultad")
    private Long idFacultad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_facultad", nullable = false)
    private EstadoFacultad estadoFacultad;

    @Column(name = "codigo_facultad", nullable = false, unique = true, length = 20)
    private String codigoFacultad;

    @Column(name = "nombre_facultad", nullable = false, unique = true, length = 150)
    private String nombreFacultad;
}