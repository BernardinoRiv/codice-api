package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipos_sede")
public class TipoSede {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_sede")
    private Long idTipoSede;

    @Column(name = "tipo_sede", nullable = false, unique = true)
    private String tipoSede;
}