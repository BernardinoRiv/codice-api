package com.codice.sra.dtos;

/**
 * DTO inmutable de proyección para selectores y apertura de oferta académica.
 * Desacoplado de los flujos de auditoría y registro de personal.
 */
public record DocenteSeleccionDTO(
        Long idDocente,
        String codigoDocente,
        String nombres,
        String apellidos,
        String tipoContratacion,
        Integer maximoMaterias,
        String label
) {
    /**
     * Constructor compacto utilizado por la consulta JPQL en DocenteRepository.
     * Genera la etiqueta visual enriquecida para los desplegables de coordinación.
     */
    public DocenteSeleccionDTO(
            Long idDocente,
            String codigoDocente,
            String nombres,
            String apellidos,
            String tipoContratacion,
            Integer maximoMaterias
    ) {
        this(
                idDocente,
                codigoDocente,
                nombres,
                apellidos,
                tipoContratacion,
                maximoMaterias,
                apellidos + ", " + nombres + " (" + tipoContratacion + " - Máx: " + maximoMaterias + " mat.)"
        );
    }
}