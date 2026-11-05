package com.codice.sra.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProcesarPagoRequestDTO {
    @NotNull(message = "El ID del estudiante es obligatorio")
    private Long idEstudiante;

    // ATENCIÓN: Asegúrate de quitarle el @NotEmpty o @NotNull si lo tenía
    private List<Long> idsCargosAPagar = new ArrayList<>();

    // NUEVA LISTA: Recibe los IDs de los aranceles extras que mandamos de React
    private List<Long> arancelesAdicionales = new ArrayList<>();

    @NotNull(message = "El monto recibido es obligatorio")
    private BigDecimal montoRecibido;

    @NotNull(message = "El método de pago es obligatorio")
    private Integer idMetodoPago;

    private String numeroReferencia;
}