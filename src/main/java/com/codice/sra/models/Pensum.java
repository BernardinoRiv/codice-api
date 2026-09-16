package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "pensum", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_carrera", "version"})
})
@Data
public class Pensum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pensum")
    private Long idPensum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_pensum", nullable = false)
    private EstadoPensum estadoPensum;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "anio_inicio", nullable = false)
    private Integer anioInicio;

    @Column(name = "anio_fin")
    private Integer anioFin;
}