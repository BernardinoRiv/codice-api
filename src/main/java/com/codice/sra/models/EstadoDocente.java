package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "estados_docente")
public class EstadoDocente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_docente")
    private Long idEstadoDocente;

    @Column(name = "estado_docente", nullable = false, unique = true)
    private String estadoDocente;
}