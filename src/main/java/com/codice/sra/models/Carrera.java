package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "carreras",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_carreras_codigo", columnNames = {"codigo_carrera"}),
                @UniqueConstraint(name = "uq_carreras_nombre", columnNames = {"nombre_carrera"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Long idCarrera;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_facultad", nullable = false, foreignKey = @ForeignKey(name = "fk_carreras_facultad"))
    private Facultad facultad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_nivel", nullable = false, foreignKey = @ForeignKey(name = "fk_carreras_nivel"))
    private NivelAcademico nivel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado_carrera", nullable = false, foreignKey = @ForeignKey(name = "fk_carreras_estado"))
    private EstadoCarrera estadoCarrera;

    @Column(name = "codigo_carrera", nullable = false, unique = true, length = 20)
    private String codigoCarrera;

    @Column(name = "nombre_carrera", nullable = false, unique = true, length = 150)
    private String nombreCarrera;

    // Relación bidireccional indispensable para CarreraRepository (c.carrerasSedes)
    @Builder.Default
    @OneToMany(mappedBy = "carrera", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarreraSede> carrerasSedes = new ArrayList<>();

    // Relación bidireccional indispensable para CarreraRepository (c.pensums)
    @Builder.Default
    @OneToMany(mappedBy = "carrera", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pensum> pensums = new ArrayList<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Carrera carrera = (Carrera) o;
        return getIdCarrera() != null && Objects.equals(getIdCarrera(), carrera.getIdCarrera());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}