package com.codice.sra.dtos.DashboardDTO;

import java.math.BigDecimal;

public interface DashboardEvolucionSemanalProjection {

    Integer getNumeroSemana();

    BigDecimal getRecaudacionEsperada();

    BigDecimal getRecaudacionReal();
}