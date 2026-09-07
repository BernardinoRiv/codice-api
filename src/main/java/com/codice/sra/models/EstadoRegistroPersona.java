package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "estados_registro_persona")
public class EstadoRegistroPersona {

    @Id
    @Column(name = "id_estado_registro")
    private Long idEstadoRegistro;

    @Column(name = "estado_registro", nullable = false, unique = true, length = 50)
    private String estadoRegistro;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_creacion", updatable = false)
    private OffsetDateTime fechaCreacion;
}