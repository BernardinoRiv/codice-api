package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "edificios")
@Data
public class Edificio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_edificio")
    private Long idEdificio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_edificio", nullable = false)
    private EstadoEdificio estadoEdificio;

    @Column(name = "codigo_edificio", nullable = false, unique = true, length = 20)
    private String codigoEdificio;

    @Column(name = "nombre_edificio", nullable = false, length = 100)
    private String nombreEdificio;

    @Column(name = "direccion", length = 200)
    private String direccion;
}