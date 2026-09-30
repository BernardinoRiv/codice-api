package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_pago")
@Data
public class EstadoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_pago")
    private Long idEstadoPago;

    @Column(name = "estado_pago", nullable = false, unique = true, length = 50)
    private String estadoPago;
}
