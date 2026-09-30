package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeccionResponseDTO {

    //Entrega la confirmación de apertura y la oferta consolidada de la sección.
    private Long idGrupo;
    private String codigoGrupo;
    private String materia;
    private String docente;
    private String sede;
    private String estado;
    private Integer cupoMaximo;
    private List<FranjaHorariaResponseDTO> horarios;
}