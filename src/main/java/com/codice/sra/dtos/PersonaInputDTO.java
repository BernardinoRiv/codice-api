package com.codice.sra.dtos;

import java.time.LocalDate;

public interface PersonaInputDTO {
    Long getIdTipoDocumento();
    String getNumeroDocumento();
    String getNombres();
    String getApellidos();
    LocalDate getFechaNacimiento();
    String getTelefono();
    String getCorreoPersonal();
    String getDireccion();
    String getSexo();
    Long getIdDistrito();
}