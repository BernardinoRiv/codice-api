package com.codice.sra.utils;

public final class CicloCodigoUtil {

    private CicloCodigoUtil() {
        throw new UnsupportedOperationException("Clase utilitaria: no se permite instanciación.");
    }

    /**
     * Genera el código canónico de ciclo lectivo sin sufijos de letras (ej. '01-2027').
     *
     * @param numeroCiclo Número de ciclo (1 o 2)
     * @param anio        Año lectivo institucional
     * @return Formato estandarizado '0X-AAAA'
     */
    public static String generarCodigoCiclo(int numeroCiclo, int anio) {
        if (numeroCiclo < 1 || numeroCiclo > 2) {
            throw new IllegalArgumentException("El número de ciclo debe ser estrictamente 1 o 2.");
        }
        if (anio < 1970 || anio > 2100) {
            throw new IllegalArgumentException("Año lectivo fuera del rango permitido.");
        }
        // Formato con dos dígitos para el ciclo (ej. 01-2027, 02-2027)
        return String.format("%02d-%d", numeroCiclo, anio);
    }
}