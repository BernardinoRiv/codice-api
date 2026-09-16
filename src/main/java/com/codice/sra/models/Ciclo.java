package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "ciclos")
@Data
public class Ciclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ciclo")
    private Long idCiclo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_ciclo", nullable = false)
    private EstadoCiclo estadoCiclo;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "numero_ciclo", nullable = false)
    private Integer numeroCiclo;

    @Column(name = "codigo_ciclo", nullable = false, unique = true, length = 20)
    private String codigoCiclo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;
}