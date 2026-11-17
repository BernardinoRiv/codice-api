package com.codice.sra.dtos;

import java.math.BigDecimal;
import java.util.List;

public record PensumMateriaDetalleDTO(Long idMateria, String codigoMateria, String nombreMateria,
                                     BigDecimal unidadesValorativas, Integer cicloRecomendado,
                                     Boolean obligatoria, List<PrerrequisitoDTO> prerrequisitos) {
    public record PrerrequisitoDTO(Long idMateria, String codigoMateria, String nombreMateria) {}
}
