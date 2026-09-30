package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "canales_pago")
@Data
public class CanalPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_canal_pago")
    private Long idCanalPago;

    @Column(name = "canal_pago", nullable = false, unique = true, length = 80)
    private String canalPago;
}
