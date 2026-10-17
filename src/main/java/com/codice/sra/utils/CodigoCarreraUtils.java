package com.codice.sra.utils;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class CodigoCarreraUtils {

    private static final Set<String> STOP_WORDS = Set.of(
            "DE", "DEL", "LA", "LAS", "EL", "LOS", "EN", "Y", "E", "O", "U", "A", "POR", "PARA"
    );

    private static final Pattern DIACRITICS_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private CodigoCarreraUtils() {
        // Clase utilitaria, no instanciable
    }

    public static String generarCodigoCarrera(String nombreCarrera, Long idNivel, Predicate<String> existeCodigoPredicate) {
        if (nombreCarrera == null || nombreCarrera.isBlank()) {
            throw new IllegalArgumentException("El nombre de la carrera es requerido para generar su código");
        }

        // 1. Sanitizar y limpiar diacríticos (tildes, caracteres especiales)
        String textoLimpio = Normalizer.normalize(nombreCarrera.trim(), Normalizer.Form.NFD);
        textoLimpio = DIACRITICS_PATTERN.matcher(textoLimpio).replaceAll("");
        textoLimpio = textoLimpio.replaceAll("[^a-zA-Z0-9\\s]", " ").toUpperCase();

        // 2. Tokenizar y filtrar stopwords
        String[] tokens = textoLimpio.split("\\s+");
        List<String> palabrasClave = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank() && !STOP_WORDS.contains(token)) {
                palabrasClave.add(token);
            }
        }

        if (palabrasClave.isEmpty()) {
            throw new IllegalArgumentException("No se pudieron extraer términos válidos del nombre de la carrera");
        }

        // 3. Extracción heurística del código base
        String codigoBase = construirCodigoBase(palabrasClave, idNivel);

        // Limitar a longitud máxima según DDL (máximo 15 para dar margen al sufijo numérico)
        if (codigoBase.length() > 15) {
            codigoBase = codigoBase.substring(0, 15);
        }

        // 4. Resolución determinista de colisiones
        String codigoCandidato = codigoBase;
        int secuencia = 2;

        while (existeCodigoPredicate.test(codigoCandidato)) {
            codigoCandidato = codigoBase + secuencia;
            secuencia++;
        }

        return codigoCandidato;
    }

    private static String construirCodigoBase(List<String> palabras, Long idNivel) {
        // Caso A: Técnicos o Maestrías (Suelen basarse en las iniciales de todas sus palabras clave)
        // Ej: Técnico Contaduría Pública -> TCP | Maestría Docencia Universitaria -> MDU
        if (idNivel != null && (idNivel == 1L || idNivel == 3L || palabras.size() >= 3)) {
            StringBuilder iniciales = new StringBuilder();
            for (String p : palabras) {
                iniciales.append(p.charAt(0));
            }
            if (iniciales.length() >= 3) {
                return iniciales.toString();
            }
        }

        // Caso B: Licenciaturas con nombre corto o palabra dominante
        // Omitir prefijos descriptivos comunes si existen en las palabras clave
        List<String> especialidad = new ArrayList<>(palabras);
        if (!especialidad.isEmpty() && (especialidad.get(0).equals("LICENCIATURA") || especialidad.get(0).equals("INGENIERIA"))) {
            especialidad.remove(0);
        }

        if (especialidad.isEmpty()) {
            especialidad = palabras;
        }

        // Si queda una sola palabra de especialidad (ej. "Informática", "Psicología", "Mercadotecnia")
        // Toma las primeras 3 letras (INF, PSI, MER)
        if (especialidad.size() == 1) {
            String unica = especialidad.get(0);
            return unica.length() >= 3 ? unica.substring(0, 3) : unica;
        }

        // Si son 2 palabras de especialidad (ej. "Administración Empresas" -> ADM, o "Ciencias Jurídicas" -> JUR)
        // Si una de ellas es "CIENCIAS", priorizar la disciplina principal
        if (especialidad.size() == 2 && especialidad.get(0).equals("CIENCIAS")) {
            String disciplina = especialidad.get(1);
            return disciplina.length() >= 3 ? disciplina.substring(0, 3) : disciplina;
        }

        // Formato compuesto iniciales + silábico (ej. 2 palabras -> 2 letras de la primera + 2 de la segunda)
        StringBuilder combinado = new StringBuilder();
        for (String p : especialidad) {
            combinado.append(p.charAt(0));
        }

        return combinado.length() >= 3 ? combinado.toString() : especialidad.get(0).substring(0, Math.min(3, especialidad.get(0).length()));
    }
}