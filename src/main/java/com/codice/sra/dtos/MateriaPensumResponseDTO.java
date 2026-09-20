package com.codice.sra.dtos;

public record MateriaPensumResponseDTO(
        Long idMateria,
        String codigoMateria,
        String nombreMateria,
        Integer cicloRecomendado,
        String labelVisual
) {
    // Constructor de conveniencia que usará JPQL directamente
    public MateriaPensumResponseDTO(Long idMateria, String codigoMateria, String nombreMateria, Integer cicloRecomendado) {
        this(
                idMateria,
                codigoMateria,
                nombreMateria,
                cicloRecomendado,
                "[Ciclo " + cicloRecomendado + "] " + codigoMateria + " - " + nombreMateria
        );
    }
}