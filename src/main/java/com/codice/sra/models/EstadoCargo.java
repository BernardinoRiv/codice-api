package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_cargo")
@Data
public class EstadoCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_cargo")
    private Long idEstadoCargo;

    @Column(name = "estado_cargo", nullable = false, unique = true, length = 50)
    private String estadoCargo;
}
