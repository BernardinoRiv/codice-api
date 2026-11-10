package com.codice.sra.dtos.DashboardDTO;

public interface SeccionDetalleProjection {
    Long getIdGrupo();
    String getCodigoGrupo();
    Long getIdMateria();
    String getMateria();
    String getDocente();
    String getJornada();
    Long getInscritos();
    Long getCupoMaximo();
}
