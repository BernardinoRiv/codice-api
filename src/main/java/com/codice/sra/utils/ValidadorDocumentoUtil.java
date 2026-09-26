package com.codice.sra.utils;

public final class ValidadorDocumentoUtil {

    private ValidadorDocumentoUtil() {}

    // Expresiones regulares estrictas
    private static final String REGEX_DUI = "^\\d{8}-\\d$";
    private static final String REGEX_MENORIDAD = "^\\d{7,9}$";
    private static final String REGEX_PASAPORTE = "^[A-Z][0-9]{7,8}$";
    private static final String REGEX_RESIDENCIA = "^[A-Z0-9]{8,12}$";

    public static String normalizar(String numeroDocumento) {
        if (numeroDocumento == null) {
            return null;
        }
        return numeroDocumento.trim().toUpperCase();
    }

    public static void validar(Long idTipoDocumento, String numeroDocumento) {
        if (idTipoDocumento == null) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio.");
        }
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("El número de documento es obligatorio.");
        }

        String doc = normalizar(numeroDocumento);
        int longitud = doc.length();

        switch (idTipoDocumento.intValue()) {
            case 1 -> { // DUI (Longitud fija: 10 caracteres exactos)
                if (longitud != 10) {
                    throw new IllegalArgumentException(
                            "Longitud de DUI inválida. Debe tener exactamente 10 caracteres (8 dígitos, guion y 1 dígito verificador). Longitud recibida: " + longitud
                    );
                }
                if (!doc.matches(REGEX_DUI)) {
                    throw new IllegalArgumentException(
                            "Formato de DUI inválido. Debe cumplir el patrón 00000000-0 (ej: 05123456-7)."
                    );
                }
            }
            case 2 -> { // CARNET DE MINORIDAD (7 a 9 caracteres)
                if (longitud < 7 || longitud > 9) {
                    throw new IllegalArgumentException(
                            "Longitud de Carnet de Minoridad inválida. Debe tener entre 7 y 9 dígitos. Longitud recibida: " + longitud
                    );
                }
                if (!doc.matches(REGEX_MENORIDAD)) {
                    throw new IllegalArgumentException(
                            "Formato de Carnet de Minoridad inválido. Solo se admiten dígitos numéricos sin guiones ni espacios."
                    );
                }
            }
            case 3 -> { // PASAPORTE (8 a 9 caracteres)
                if (longitud < 8 || longitud > 9) {
                    throw new IllegalArgumentException(
                            "Longitud de Pasaporte inválida. Debe tener 8 o 9 caracteres. Longitud recibida: " + longitud
                    );
                }
                if (!doc.matches(REGEX_PASAPORTE)) {
                    throw new IllegalArgumentException(
                            "Formato de Pasaporte inválido. Debe iniciar con una letra mayúscula seguida de 7 u 8 números (ej: A12345678)."
                    );
                }
            }
            case 4 -> { // CARNET DE RESIDENCIA (8 a 12 caracteres)
                if (longitud < 8 || longitud > 12) {
                    throw new IllegalArgumentException(
                            "Longitud de Carnet de Residencia inválida. Debe tener entre 8 y 12 caracteres. Longitud recibida: " + longitud
                    );
                }
                if (!doc.matches(REGEX_RESIDENCIA)) {
                    throw new IllegalArgumentException(
                            "Formato de Carnet de Residencia inválido. Solo se admiten caracteres alfanuméricos sin espacios ni guiones."
                    );
                }
            }
            default -> throw new IllegalArgumentException(
                    "Tipo de documento no admitido. Solo se permite: 1 (DUI), 2 (Minoridad), 3 (Pasaporte) o 4 (Residencia)."
            );
        }
    }
}