package com.codice.sra.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionUsuarioDTO {

    private Long idSesion;
    private String nombreUsuario;
    private String rol;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String direccionIp;
    private String dispositivo;
    private Boolean exitosa;
    private Boolean esAnomalia;
}