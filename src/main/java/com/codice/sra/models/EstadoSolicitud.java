package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_solicitud")
@Data
public class EstadoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_solicitud")
    private Long idEstadoSolicitud;

    @Column(name = "estado_solicitud", nullable = false, unique = true, length = 50)
    private String estadoSolicitud;
}
