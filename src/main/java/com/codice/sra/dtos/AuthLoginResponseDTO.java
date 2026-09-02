package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthLoginResponseDTO {

    //Este objeto definirá la estructura de la respuesta exitosa, entregando el token JWT (que configuraremos más adelante) y los datos básicos de la sesión, pero manteniendo oculta toda la información sensible de la entidad Usuario.

    private String token;
    private String nombreCompleto;
    private String rol;
}