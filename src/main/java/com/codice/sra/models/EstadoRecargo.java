package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_recargo")
@Data
public class EstadoRecargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_recargo")
    private Long idEstadoRecargo;

    @Column(name = "estado_recargo", nullable = false, unique = true, length = 50)
    private String estadoRecargo;
}
