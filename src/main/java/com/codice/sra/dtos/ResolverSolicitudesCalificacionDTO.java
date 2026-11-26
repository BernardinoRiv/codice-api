package com.codice.sra.dtos;

import lombok.Data;
import java.util.List;

@Data
public class ResolverSolicitudesCalificacionDTO {
    private List<Long> idsSolicitud;
    private Boolean aprobar;
    private String respuesta;
}
