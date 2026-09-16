package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "pagos_cargos")
@Data
public class PagoCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_cargo")
    private Long idPagoCargo;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", nullable = false)
    private Pago pago;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cargo", nullable = false)
    private CargoEstudiante cargo;

    @Column(name = "monto_aplicado", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoAplicado;
}
