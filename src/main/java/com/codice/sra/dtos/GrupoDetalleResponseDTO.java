package com.codice.sra.dtos;

import java.util.List;

public record GrupoDetalleResponseDTO(
        Long idGrupo,
        String codigoGrupo,
        Long idMateria,
        String codigoMateria,
        String nombreMateria,
        String nombreCarrera,
        Long idDocente,
        String nombreDocente,
        Long idSede,
        String nombreSede,
        String modalidad,
        String aulaOEnlace,
        Integer cupoMaximo,
        String estadoGrupo,
        List<FranjaHorariaResponseDTO> horarios
) {}