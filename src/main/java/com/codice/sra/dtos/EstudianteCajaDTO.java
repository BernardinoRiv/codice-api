package com.codice.sra.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstudianteCajaDTO {
    private Long idEstudiante;
    private String carnet;
    private String nombreCompleto;
    private String programaAcademico; // Ej: "Licenciatura en Informática"
    private String nivelAcademico; // Ej: "PREGRADO", "MAESTRÍA", "PRE-ESPECIALIZACIÓN"
    private boolean esBecado; // Bandera visual rápida para el cajero
}