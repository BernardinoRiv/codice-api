package com.codice.sra.dtos;

public record ActivacionCicloResponseDTO(
        boolean exito,
        String mensaje,
        Long idCiclo,
        int cobrosNuevosGenerados
) {}