package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_beneficio")
@Data
public class EstadoBeneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_beneficio")
    private Long idEstadoBeneficio;

    @Column(name = "estado_beneficio", nullable = false, unique = true, length = 50)
    private String estadoBeneficio;
}
