package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "materias")
@Data
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_materia")
    private Long idMateria;

    @Column(name = "codigo_materia", nullable = false, unique = true, length = 20)
    private String codigoMateria;

    @Column(name = "nombre_materia", nullable = false, unique = true, length = 150)
    private String nombreMateria;

    @Column(name = "unidades_valorativas", nullable = false, precision = 4, scale = 2)
    private BigDecimal unidadesValorativas;

    @Column(name = "estado_materia", nullable = false)
    private Boolean estadoMateria = true;

    @OneToMany(mappedBy = "materia", fetch = FetchType.LAZY)
    private List<PensumMateria> pensumMaterias = new ArrayList<>();
}