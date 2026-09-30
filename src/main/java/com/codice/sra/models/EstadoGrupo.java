package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_grupo")
@Data
public class EstadoGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_grupo")
    private Long idEstadoGrupo;

    @Column(name = "estado_grupo", nullable = false, unique = true, length = 50)
    private String estadoGrupo;
}