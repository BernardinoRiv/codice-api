
package com.codice.sra.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EstudianteCambioDocumentoDTO {

    @NotBlank
    private String documentoActual;

    @NotNull
    private Long idTipoDocumento;

    @NotBlank
    private String numeroDocumento;

    @NotBlank
    @Size(min = 10, max = 500)
    private String motivo;
}
