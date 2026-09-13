package com.codice.sra.models;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "estados_empleado")
public class EstadoEmpleado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_empleado")
    private Long idEstadoEmpleado;

    @Column(name = "estado_empleado", nullable = false, unique = true)
    private String estadoEmpleado;
}