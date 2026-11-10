package com.codice.sra.dtos.DashboardDTO;

import java.math.BigDecimal;

public interface DashboardFinancieroProjection {
    BigDecimal getRecaudacionEsperada();
    BigDecimal getRecaudacionReal();
    BigDecimal getSaldoPendiente();
    BigDecimal getSaldoVencido();
}
