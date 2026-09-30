package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "beneficios")
@Data
public class Beneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_beneficio")
    private Long idBeneficio;

    @Column(name = "beneficio", nullable = false, unique = true, length = 150)
    private String beneficio;

    @Column(name = "porcentaje", precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(name = "monto", precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "cum_minimo", precision = 5, scale = 2)
    private BigDecimal cumMinimo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_beneficio", nullable = false)
    private EstadoBeneficio estadoBeneficio;
}
