package com.codice.sra.dtos;

import java.math.BigDecimal;

public record MateriaListadoDTO(
        Long idMateria,
        String codigoMateria,
        String nombreMateria,
        BigDecimal unidadesValorativas,
        Boolean estadoMateria,
        Integer cantidadPrerrequisitos
) {}
