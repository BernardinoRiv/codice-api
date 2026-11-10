package com.codice.sra.dtos.DashboardDTO;

import java.math.BigDecimal;

public record DashboardFinancieroDTO(
        String codigoCiclo,
        Long idSede,
        BigDecimal recaudacionEsperada,
        BigDecimal recaudacionReal,
        BigDecimal saldoPendiente,
        BigDecimal saldoVencido,
        BigDecimal porcentajeCumplimiento
) {}
