package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotasEstudianteResponseDTO {
    private String ciclo;
    private List<MateriaNotasDTO> materias;
}