package com.codice.sra.models;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "areas")
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_area")
    private Long idArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area_padre")
    private Area areaPadre;

    @Column(name = "area", nullable = false, unique = true)
    private String area;
}