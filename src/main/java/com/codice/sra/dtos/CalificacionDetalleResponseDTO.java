package com.codice.sra.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class CalificacionDetalleResponseDTO {
    private Long idCalificacion;
    private String carnetEstudiante;
    private String nombreEstudiante;
    private String apellidoEstudiante;
    private String codigoMateria;
    private String nombreMateria;
    private String codigoGrupo;
    private String tipoEvaluacion;
    private Integer numeroEvaluacion;
    private BigDecimal nota;
    private String estadoCalificacion;
    private String nombreDocente;
    private String apellidoDocente;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaModificacion;
}
