package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "dias_semana")
@Data
public class DiaSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dia")
    private Long idDia;

    @Column(name = "dia", nullable = false, unique = true, length = 20)
    private String dia;
}