package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteCarreraListaDTO {
    private Long idEstudianteCarrera;
    private String nombreCarrera;
    private String nombreSede;
    private String estadoTrayectoria;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer versionPensum;
    private Integer totalMaterias;
    private Integer materiasAprobadas;
    private Integer materiasReprobadas;
    private Integer materiasCursando;
    private Integer materiasRetiradas;
    private Integer ciclosMatriculados;
    private Integer ciclosCursados;
    private Integer totalCiclosPensum;
    private BigDecimal porcentajeAvance;
    private Integer anioFinalizacionEstimado;

    public EstudianteCarreraListaDTO(Long idEstudianteCarrera, String nombreCarrera, String nombreSede,
                                     String estadoTrayectoria, LocalDate fechaInicio, LocalDate fechaFin) {
        this.idEstudianteCarrera = idEstudianteCarrera;
        this.nombreCarrera = nombreCarrera;
        this.nombreSede = nombreSede;
        this.estadoTrayectoria = estadoTrayectoria;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }
}
