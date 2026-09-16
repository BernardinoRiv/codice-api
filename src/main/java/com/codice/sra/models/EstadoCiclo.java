package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_ciclo")
@Data
public class EstadoCiclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_ciclo")
    private Long idEstadoCiclo;

    @Column(name = "estado_ciclo", nullable = false, unique = true, length = 50)
    private String estadoCiclo;
}