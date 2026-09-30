package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "estados_sede")
public class EstadoSede {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_sede")
    private Long idEstadoSede;

    @Column(name = "estado_sede", nullable = false, unique = true)
    private String estadoSede;
}