package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(
        name = "estados_ciclo",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_estados_ciclo_nombre", columnNames = {"estado_ciclo"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoCiclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_ciclo", nullable = false)
    private Long idEstadoCiclo;

    @Column(name = "estado_ciclo", nullable = false, unique = true, length = 50)
    private String estadoCiclo;

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
        EstadoCiclo that = (EstadoCiclo) o;
        return getIdEstadoCiclo() != null && Objects.equals(getIdEstadoCiclo(), that.getIdEstadoCiclo());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}