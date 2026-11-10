package com.codice.sra.dtos;

import lombok.Data;
import java.time.LocalTime;

@Data
public class HorarioDTO {
    private String dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String aula;
    private String modalidad;
}