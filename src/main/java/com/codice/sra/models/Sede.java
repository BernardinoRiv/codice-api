package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sedes")
public class Sede {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    private Long idSede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_sede", nullable = false)
    private TipoSede tipoSede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_sede", nullable = false)
    private EstadoSede estadoSede;

    @Column(name = "codigo_sede", nullable = false, unique = true)
    private String codigoSede;

    @Column(name = "nombre_sede", nullable = false, unique = true)
    private String nombreSede;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "correo")
    private String correo;
}
