package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotasEvaluacionesDTO {
    private BigDecimal lab1;
    private BigDecimal par1;
    private BigDecimal lab2;
    private BigDecimal par2;
    private BigDecimal lab3;
    private BigDecimal par3;
}