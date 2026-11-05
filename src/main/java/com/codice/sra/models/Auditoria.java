package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "auditoria", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria", updatable = false)
    private Long idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", updatable = false)
    private Usuario usuario;

    @Column(name = "accion", nullable = false, updatable = false, length = 50)
    private String accion; // Ej: "CAMBIO_ESTADO_DOCENTE", "ACTUALIZACION_DOCENTE"

    @Column(name = "tabla", updatable = false, length = 50)
    private String tabla; // "docentes"

    @Column(name = "id_registro", updatable = false)
    private Long idRegistro; // idDocente

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private OffsetDateTime fechaHora;

    // Mapeo transparente del tipo nativo inet de PostgreSQL en Hibernate 6
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "direccion_ip", updatable = false, columnDefinition = "inet")
    private String direccionIp;

    @Column(name = "descripcion", columnDefinition = "text", updatable = false)
    private String descripcion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaHora == null) {
            this.fechaHora = OffsetDateTime.now();
        }
    }
}