package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "puntos_recaudo")
@Data
public class PuntoRecaudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_punto_recaudo")
    private Long idPuntoRecaudo;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @Column(name = "punto_recaudo", nullable = false, length = 150)
    private String puntoRecaudo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
