package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "docentes")
public class Docente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_docente")
    private Long idDocente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_contratacion", nullable = false)
    private TipoContratacionDocente tipoContratacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_docente", nullable = false)
    private EstadoDocente estadoDocente;

    @Column(name = "codigo_docente", nullable = false, unique = true)
    private String codigoDocente;

    @Column(name = "especialidad")
    private String especialidad;
}
