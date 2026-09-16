package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cargos_estudiante")
@Data
public class CargoEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cargo_estudiante")
    private Long idCargoEstudiante;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_matricula", nullable = false)
    private Matricula matricula;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_concepto_cobro", nullable = false)
    private ConceptoCobro conceptoCobro;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante_beneficio")
    private EstudianteBeneficio estudianteBeneficio;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_cargo", nullable = false)
    private EstadoCargo estadoCargo;

    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota = 1;

    @Column(name = "monto_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoBase;

    @Column(name = "monto_descuento", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    @Column(name = "monto_recargo", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoRecargo = BigDecimal.ZERO;

    @Column(name = "monto_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
}
