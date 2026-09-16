package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tipos_aula")
@Data
public class TipoAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_aula")
    private Long idTipoAula;

    @Column(name = "tipo_aula", nullable = false, unique = true, length = 50)
    private String tipoAula;
}