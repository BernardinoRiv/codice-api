package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_asistencia")
@Data
public class EstadoAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_asistencia")
    private Long idEstadoAsistencia;

    @Column(name = "estado_asistencia", nullable = false, unique = true, length = 50)
    private String estadoAsistencia;
}
