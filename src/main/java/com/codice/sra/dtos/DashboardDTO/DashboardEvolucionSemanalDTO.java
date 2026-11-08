package com.codice.sra.dtos.DashboardDTO;

import java.math.BigDecimal;

public record DashboardEvolucionSemanalDTO(
        String semana,
        BigDecimal recaudacionEsperada,
        BigDecimal recaudacionReal
) {
}