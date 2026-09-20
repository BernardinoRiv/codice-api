package com.codice.sra.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GrupoException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public GrupoException(HttpStatus status, String error, String mensaje) {
        super(mensaje);
        this.status = status;
        this.error = error;
    }

    // Factory methods semánticos para uso limpio en servicios
    public static GrupoException conflicto(String mensaje) {
        return new GrupoException(HttpStatus.CONFLICT, "Conflicto de Horario", mensaje);
    }

    public static GrupoException reglaNegocio(String mensaje) {
        return new GrupoException(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de Negocio de Grupo", mensaje);
    }

    public static GrupoException noEncontrado(String mensaje) {
        return new GrupoException(HttpStatus.NOT_FOUND, "Recurso No Encontrado", mensaje);
    }
}