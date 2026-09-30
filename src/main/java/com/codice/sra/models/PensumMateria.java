package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(
        name = "pensum_materias",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_pensum_materia",
                        columnNames = {"id_pensum", "id_materia"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PensumMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pensum_materia", nullable = false, updatable = false)
    private Long idPensumMateria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_pensum",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pensum_materias_pensum")
    )
    private Pensum pensum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_materia",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pensum_materias_materia")
    )
    private Materia materia;

    @Column(name = "ciclo_recomendado", nullable = false)
    private Integer cicloRecomendado;

    @Builder.Default
    @Column(name = "obligatoria", nullable = false)
    private Boolean obligatoria = true;

    // Implementación canónica y segura de equals() y hashCode() para entidades JPA con proxies Hibernate
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hp
                ? hp.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hp
                ? hp.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PensumMateria that = (PensumMateria) o;
        return getIdPensumMateria() != null && Objects.equals(getIdPensumMateria(), that.getIdPensumMateria());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hp
                ? hp.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}