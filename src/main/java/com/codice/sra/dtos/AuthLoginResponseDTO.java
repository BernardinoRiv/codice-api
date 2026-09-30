package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthLoginResponseDTO {

    // Datos de sesión (serán nulos si el login falla)
    private String token;
    private String nombreCompleto;
    private String rol;
    private OffsetDateTime ultimoAcceso;

    // Nuevos campos para intentos y bloqueos
    private Integer intentosFallidos;
    private OffsetDateTime bloqueadoHasta;

    // Campos de control para saber el resultado de la petición
    private String mensaje;
    private boolean exito;
}