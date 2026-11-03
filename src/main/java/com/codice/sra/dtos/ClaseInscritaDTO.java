package com.codice.sra.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ClaseInscritaDTO {
    private String codigoMateria;
    private String nombreMateria;
    private String grupo;
    private BigDecimal uvs;
    private String nombreDocente;
    private List<HorarioDTO> horarios;
}