package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_aula")
@Data
public class EstadoAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_aula")
    private Long idEstadoAula;

    @Column(name = "estado_aula", nullable = false, unique = true, length = 50)
    private String estadoAula;
}