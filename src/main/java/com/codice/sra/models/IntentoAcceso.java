package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "intentos_acceso")
@Data
public class IntentoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento")
    private Long idIntento;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso = false;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;
}
