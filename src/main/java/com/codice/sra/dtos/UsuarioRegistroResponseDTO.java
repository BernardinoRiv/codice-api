package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioRegistroResponseDTO {
    private Long idUsuario;
    private String nombreCompleto;
    private String correoInstitucional;
    private String rol;
    private String mensaje;
}