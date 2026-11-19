package com.codice.sra.dtos;

import java.math.BigDecimal;

public record PensumCatalogoMateriaDTO(
        Long idMateria,
        String codigoMateria,
        String nombreMateria,
        BigDecimal unidadesValorativas
) {}