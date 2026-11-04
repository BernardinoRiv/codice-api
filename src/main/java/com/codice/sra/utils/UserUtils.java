package com.codice.sra.utils;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.function.Function;

public class UserUtils {

    private static final String CARACTERES_PASSWORD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DOMINIO_INSTITUCIONAL = "uma.edu.sv";

    /**
     * Genera el correo base para Docentes, Empleados y Administradores.
     * Formato Institucional: primerNombre.primerApellido@uma.edu.sv
     */
    public static String generarCorreoInstitucional(String nombres, String apellidos) {
        String primerNombre = limpiarCadena(nombres.trim().split("\\s+")[0]);
        String primerApellido = limpiarCadena(apellidos.trim().split("\\s+")[0]);

        return String.format("%s.%s@%s", primerNombre, primerApellido, DOMINIO_INSTITUCIONAL);
    }

    /**
     * Sobrecarga de seguridad: Mantiene la compatibilidad con servicios existentes
     * que envían el "rol", pero el dominio se fuerza a unificarse en @uma.edu.sv.
     */
    public static String generarCorreoInstitucional(String nombres, String apellidos, String rol) {
        return generarCorreoInstitucional(nombres, apellidos);
    }

    /**
     * Genera el correo exclusivo para ESTUDIANTES basado en el carnet.
     * Formato: carnet@uma.edu.sv
     */
    public static String generarCorreoEstudiante(String carnet) {
        return String.format("%s@%s", carnet.trim().toLowerCase(), DOMINIO_INSTITUCIONAL);
    }

    /**
     * Genera un correo único añadiendo un correlativo numérico si la base ya existe.
     * Ej: jose.castaneda@uma.edu.sv -> jose.castaneda1@uma.edu.sv
     */
    public static String generarCorreoUnico(String nombres, String apellidos, Function<String, Boolean> checker) {
        String base = generarCorreoInstitucional(nombres, apellidos);
        String localPart = base.substring(0, base.indexOf('@'));
        String domain = base.substring(base.indexOf('@') + 1);

        int contador = 1;
        String candidato = base; // Primera iteración evalúa el correo base limpio

        while (checker.apply(candidato)) {
            candidato = String.format("%s%d@%s", localPart, contador, domain);
            contador++;
        }

        return candidato;
    }

    /**
     * Sobrecarga de seguridad para compatibilidad con servicios que inyectaban el rol.
     */
    public static String generarCorreoUnico(String nombres, String apellidos, String rol, Function<String, Boolean> checker) {
        return generarCorreoUnico(nombres, apellidos, checker);
    }

    /**
     * Genera una contraseña aleatoria segura para cuentas administrativas/operativas.
     */
    public static String generarPasswordAleatorio(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(CARACTERES_PASSWORD.charAt(RANDOM.nextInt(CARACTERES_PASSWORD.length())));
        }
        return sb.toString();
    }

    /**
     * Extrae las iniciales de los nombres (Mantenido por si otro servicio lo requiere).
     */
    public static String extraerIniciales(String nombres) {
        String[] partes = nombres.trim().split("\\s+");
        StringBuilder iniciales = new StringBuilder();
        for (String parte : partes) {
            if (!parte.isEmpty()) {
                iniciales.append(Character.toLowerCase(parte.charAt(0)));
            }
        }
        return iniciales.toString();
    }

    /**
     * Genera un código único para registro de Docentes o Empleados.
     */
    public static String generarCodigoUnico(String prefijo) {
        int year = java.time.OffsetDateTime.now().getYear();
        int random = RANDOM.nextInt(9000) + 1000; // Asegura siempre 4 dígitos
        return String.format("%s-%d-%d", prefijo, year, random);
    }

    /**
     * Helper de Sanitización: Convierte caracteres latinos a formato seguro para correo electrónico.
     */
    private static String limpiarCadena(String texto) {
        if (texto == null || texto.isBlank()) return "";

        // Reemplazo explícito de la 'ñ' por 'n' antes de normalizar
        String sinEnies = texto.replace("ñ", "n").replace("Ñ", "N");

        // Descompone caracteres acentuados (ej. á -> a + ´)
        String normalizado = Normalizer.normalize(sinEnies, Normalizer.Form.NFD);

        return normalizado
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // Remueve las tildes
                .replaceAll("[^a-zA-Z0-9]", "") // Elimina guiones, espacios o símbolos raros
                .toLowerCase();
    }
}