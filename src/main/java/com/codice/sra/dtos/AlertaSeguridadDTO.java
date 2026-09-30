package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaSeguridadDTO {
    private String nombreDestinatario;
    private String correoDestinatario;
    private LocalDateTime fechaEvento;
    private String direccionIpSospechosa;
    private String dispositivoSospechoso;
    private String tipoAlerta; // Ej: "NUEVO_DISPOSITIVO", "NUEVA_UBICACION"
}