package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "aulas")
@Data
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aula")
    private Long idAula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_edificio", nullable = false)
    private Edificio edificio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_aula", nullable = false)
    private TipoAula tipoAula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_aula", nullable = false)
    private EstadoAula estadoAula;

    @Column(name = "codigo_aula", nullable = false, unique = true, length = 20)
    private String codigoAula;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;
}