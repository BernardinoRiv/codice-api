package com.codice.sra.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Immutable // <-- BUENA PRÁCTICA: Evita que JPA intente hacer INSERT/UPDATE/DELETE
@Table(name = "mv_dashboard_gerencial")
public class MvDashboardGerencial {

    @Id // JPA requiere un ID lógico, aunque sea una vista.
    @Column(name = "id_ciclo")
    private Long idCiclo;

    @Column(name = "codigo_ciclo")
    private String codigoCiclo;

    @Column(name = "total_matricula")
    private Integer totalMatricula;

    @Column(name = "total_docentes")
    private Integer totalDocentes;

    @Column(name = "total_secciones")
    private Integer totalSecciones;

    @Column(name = "capacidad_promedio")
    private Double capacidadPromedio;

    @Column(name = "total_facturado")
    private BigDecimal totalFacturado;

    @Column(name = "total_recaudado")
    private BigDecimal totalRecaudado;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;
}