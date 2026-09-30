package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "modalidades")
@Data
public class Modalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modalidad")
    private Long idModalidad;

    @Column(name = "modalidad", nullable = false, unique = true, length = 50)
    private String modalidad;
}