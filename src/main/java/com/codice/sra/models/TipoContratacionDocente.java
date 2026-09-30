package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipos_contratacion_docente")
public class TipoContratacionDocente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_contratacion")
    private Long idTipoContratacion;

    @Column(name = "tipo_contratacion", nullable = false, unique = true)
    private String tipoContratacion;

    @Column(name = "maximo_materias", nullable = false)
    private Integer maximoMaterias;
}