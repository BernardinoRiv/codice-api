
package com.codice.sra.utils;

public final class ValidadorDocumentoUtil {

    private ValidadorDocumentoUtil() {}

    private static final String REGEX_DUI = "^\\d{8}-\\d$";
    private static final String REGEX_NIT = "^\\d{4}-\\d{6}-\\d{3}-\\d$";
    private static final String REGEX_PASAPORTE = "^[A-Z][0-9]{7,8}$";

    public static String normalizar(String numeroDocumento) {
        if (numeroDocumento == null) {
            return null;
        }
        return numeroDocumento.trim().toUpperCase();
    }

    public static void validar(Long idTipoDocumento, String numeroDocumento) {

        if (idTipoDocumento == null) {
            throw new IllegalArgumentException(
                    "El tipo de documento es obligatorio."
            );
        }

        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException(
                    "El número de documento es obligatorio."
            );
        }

        String doc = normalizar(numeroDocumento);


        switch (idTipoDocumento.intValue()) {

            case 1 -> validarDui(doc);

            case 2 -> validarPasaporte(doc);

            case 3 -> validarNit(doc);

            default -> throw new IllegalArgumentException(
                    "Tipo de documento no admitido. " +
                            "Solo se permite DUI, PASAPORTE o NIT."
            );
        }

    }

    private static void validarDui(String dui) {

        if (!dui.matches(REGEX_DUI)) {
            throw new IllegalArgumentException(
                    "DUI inválido. Debe tener el formato 00000000-0."
            );
        }

        String digitos = dui.replace("-", "");

        if (digitos.equals("000000000")) {
            throw new IllegalArgumentException(
                    "El DUI no puede contener solamente ceros."
            );
        }

        int suma = 0;

        for (int i = 0; i < 8; i++) {
            int digito = Character.digit(digitos.charAt(i), 10);
            suma += digito * (9 - i);
        }

        int verificadorCalculado = (10 - (suma % 10)) % 10;
        int verificadorIngresado = Character.digit(digitos.charAt(8), 10);

        if (verificadorCalculado != verificadorIngresado) {
            throw new IllegalArgumentException(
                    "DUI inválido. El dígito verificador no coincide."
            );
        }
    }

    private static void validarNit(String nit) {

        if (!nit.matches(REGEX_NIT)) {
            throw new IllegalArgumentException(
                    "NIT inválido. Debe contener exactamente 17 caracteres " +
                            "con el formato 0000-000000-000-0."
            );
        }

        String digitos = nit.replace("-", "");

        if (digitos.equals("00000000000000")) {
            throw new IllegalArgumentException(
                    "El NIT no puede contener solamente ceros."
            );
        }
    }

    private static void validarPasaporte(String pasaporte) {

        if (!pasaporte.matches(REGEX_PASAPORTE)) {
            throw new IllegalArgumentException(
                    "Pasaporte inválido. Debe contener una letra " +
                            "mayúscula seguida de 7 u 8 números. " +
                            "Ejemplo: A12345678."
            );
        }
    }
}
