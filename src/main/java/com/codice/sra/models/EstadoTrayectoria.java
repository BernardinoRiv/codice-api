package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_trayectoria")
@Data
public class EstadoTrayectoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_trayectoria")
    private Long idEstadoTrayectoria;

    @Column(name = "estado_trayectoria", nullable = false, unique = true, length = 50)
    private String estadoTrayectoria;
}