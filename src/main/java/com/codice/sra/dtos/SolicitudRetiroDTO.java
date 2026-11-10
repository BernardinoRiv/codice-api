package com.codice.sra.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class SolicitudRetiroDTO {
    @NotNull(message = "El ID del grupo es requerido para identificar la inscripción.")
    private Long idGrupo;
}