package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "ciclos",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ciclos_codigo", columnNames = {"codigo_ciclo"}),
                @UniqueConstraint(name = "uq_ciclos_anio_numero", columnNames = {"anio", "numero_ciclo"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ciclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ciclo", nullable = false)
    private Long idCiclo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_estado_ciclo",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ciclos_estado")
    )
    private EstadoCiclo estadoCiclo;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "numero_ciclo", nullable = false)
    private Integer numeroCiclo;

    @Column(name = "codigo_ciclo", nullable = false, unique = true, length = 20)
    private String codigoCiclo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Builder.Default
    @OneToMany(mappedBy = "ciclo", fetch = FetchType.LAZY)
    private List<Grupo> grupos = new ArrayList<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Ciclo ciclo = (Ciclo) o;
        return getIdCiclo() != null && Objects.equals(getIdCiclo(), ciclo.getIdCiclo());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}