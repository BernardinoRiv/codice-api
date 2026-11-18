package com.codice.sra.dtos;

public record PensumListadoDTO(Long idPensum, Long idCarrera, String codigoCarrera, String nombreCarrera,
                               Integer version, Integer anioInicio, Integer anioFin,
                               Long idEstadoPensum, String estadoPensum) {}
