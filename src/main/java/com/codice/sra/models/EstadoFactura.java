package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_factura")
@Data
public class EstadoFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_factura")
    private Long idEstadoFactura;

    @Column(name = "estado_factura", nullable = false, unique = true, length = 50)
    private String estadoFactura;
}
