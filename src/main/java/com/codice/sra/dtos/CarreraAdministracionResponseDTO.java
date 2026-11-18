package com.codice.sra.dtos;

import java.util.List;

public record CarreraAdministracionResponseDTO(
        Long idCarrera,
        String codigoCarrera,
        String nombreCarrera,
        Long idFacultad,
        String facultad,
        Long idNivel,
        Long idEstadoCarrera,
        String estadoCarrera,
        List<SedeAsignadaDTO> sedes
) {
    public record SedeAsignadaDTO(
            Long idSede,
            String nombreSede
    ) {}
}