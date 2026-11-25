package com.codice.sra.dtos;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocenteDetalleDTO extends DocenteListaResponseDTO {
    private String sexo;
    private LocalDate fechaNacimiento;
    private Long idTipoDocumento;
    private Long idDepartamento;
    private String nombreDepartamento;
    private String nombreDistrito;
    private Long idEstadoDocente;
}
