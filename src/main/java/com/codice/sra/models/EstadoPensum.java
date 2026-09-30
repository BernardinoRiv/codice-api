package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_pensum")
@Data
public class EstadoPensum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_pensum")
    private Long idEstadoPensum;

    @Column(name = "estado_pensum", nullable = false, unique = true, length = 50)
    private String estadoPensum;
}