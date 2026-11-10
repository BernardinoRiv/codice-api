package com.codice.sra.dtos;

public record CarreraResponseDTO(
        Long idCarrera,
        String codigoCarrera,
        String nombreCarrera,
        String facultad,
        Long idPensumVigente,
        Integer versionPensum,

        Long idFacultad,
        Long idNivel,
        Long idEstadoCarrera,
        String estadoCarrera
) {}