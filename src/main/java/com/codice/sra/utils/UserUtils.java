package com.codice.sra.utils;

import java.security.SecureRandom;

public class UserUtils {

    private static final String CARACTERES_PASSWORD = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera un correo institucional basado en nombres, apellidos y rol.
     * Formato: iniciales.apellido@rol.uma.edu.svvv
     */
    public static String generarCorreoInstitucional(String nombres, String apellidos, String rol) {
        String iniciales = extraerIniciales(nombres);
        String primerApellido = apellidos.split("\\s+")[0].toLowerCase().replaceAll("[^a-z]", "");

        String sufijoRol = switch (rol.toUpperCase()) {
            case "DOCENTE" -> "docente";
            case "ESTUDIANTE", "ALUMNO" -> "alumno";
            case "ADMINISTRADOR", "ADMIN" -> "admin";
            default -> "usuario";
        };

        return String.format("%s.%s@%s.uma.edu.svvv", iniciales, primerApellido, sufijoRol);
    }

    /**
     * Genera un correo único añadiendo un contador si el base ya existe.
     */
    public static String generarCorreoUnico(String nombres, String apellidos, String rol, java.util.function.Function<String, Boolean> checker) {
        String base = generarCorreoInstitucional(nombres, apellidos, rol);
        String localPart = base.substring(0, base.indexOf('@'));
        String domain = base.substring(base.indexOf('@') + 1);

        int contador = 1;
        String candidato;
        do {
            candidato = String.format("%s%d@%s", localPart, contador, domain);
            contador++;
        } while (checker.apply(candidato)); // Usamos el repositorio pasado como función

        return candidato;
    }

    /**
     * Genera una contraseña aleatoria segura.
     */
    public static String generarPasswordAleatorio(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(CARACTERES_PASSWORD.charAt(RANDOM.nextInt(CARACTERES_PASSWORD.length())));
        }
        return sb.toString();
    }

    /**
     * Extrae las iniciales de los nombres.
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
     * Genera un código único para Docentes o Empleados.
     */
    public static String generarCodigoUnico(String prefijo) {
        int year = java.time.OffsetDateTime.now().getYear();
        int random = RANDOM.nextInt(9000) + 1000;
        return String.format("%s-%d-%d", prefijo, year, random);
    }
}