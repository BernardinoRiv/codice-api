package com.codice.sra.dtos;

import java.math.BigDecimal;
import java.util.List;

public record PensumDetalleDTO(PensumListadoDTO pensum, Integer totalMaterias,
                              BigDecimal totalUnidadesValorativas, Integer totalCiclos,
                              List<PensumMateriaDetalleDTO> materias) {}
