package com.codice.sra.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plantillas_horarios", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla")
    private Long idPlantilla;

    @Column(name = "codigo_plantilla", nullable = false, unique = true, length = 50)
    private String codigoPlantilla;

    @Column(name = "descripcion")
    private String descripcion;

    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Builder.Default
    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlantillaHorarioDetalle> detalles = new ArrayList<>();
}