package com.codice.sra.dtos;

import java.math.BigDecimal;
import java.util.List;

public record MateriaDetalleDTO(
        Long idMateria,
        String codigoMateria,
        String nombreMateria,
        BigDecimal unidadesValorativas,
        Boolean estadoMateria,
        List<PrerrequisitoDTO> prerrequisitos,
        List<PensumUsoDTO> pensums,
        List<String> codigosDependientesActivos
) {
    public record PrerrequisitoDTO(
            Long idMateria,
            String codigoMateria,
            String nombreMateria,
            Boolean estadoMateria
    ) {}

    public record PensumUsoDTO(
            Long idPensum,
            String nombreCarrera,
            Integer version,
            String estadoPensum
    ) {}
}
