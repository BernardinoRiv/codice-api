package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FranjaHorariaResponseDTO {
    private Long idHorario;
    private String dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String modalidad;
    private String aula;
    private String enlaceVirtual;
}