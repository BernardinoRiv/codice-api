package com.codice.sra.dtos;

import java.util.List;
import java.math.BigDecimal;

public record InscripcionContinuaResponseDTO(Long idMatricula, String codigoCiclo,
                                               List<MateriaInscritaDTO> materiasInscritas,
                                               boolean pagoRealizado, Long idPago,
                                               String numeroFactura, String referenciaPago,
                                               BigDecimal montoPagado, String mensaje) {
    public record MateriaInscritaDTO(Long idInscripcion, Long idGrupo,
                                     String codigoGrupo, String codigoMateria,
                                     String nombreMateria, String estado) {}
}
