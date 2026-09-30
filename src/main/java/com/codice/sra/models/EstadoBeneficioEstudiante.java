package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estados_beneficio_estudiante")
@Data
public class EstadoBeneficioEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_beneficio_estudiante")
    private Long idEstadoBeneficioEstudiante;

    @Column(name = "estado_beneficio_estudiante", nullable = false, unique = true, length = 50)
    private String estadoBeneficioEstudiante;
}
