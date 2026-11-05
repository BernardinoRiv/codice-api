package com.codice.sra.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    // Carpeta donde se guardarán las fotos en tu servidor/computadora
    private final String uploadDir = "uploads/fotos_perfil/";

    public String guardarFoto(MultipartFile archivo) {
        try {
            if (archivo.isEmpty()) {
                throw new RuntimeException("El archivo está vacío.");
            }
            if (archivo.getSize() > 2 * 1024 * 1024) {
                throw new RuntimeException("La fotografía no debe superar los 2MB.");
            }
            String contentType = archivo.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                throw new RuntimeException("Formato no permitido. Solo se aceptan fotos JPG o PNG.");
            }

            Path directorio = Paths.get(uploadDir);
            if (!Files.exists(directorio)) {
                Files.createDirectories(directorio);
            }

            // Prevención del NullPointerException (Causa común del error 500 aquí)
            String originalName = archivo.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                originalName = "foto_perfil.jpg";
            }

            String nombreArchivo = UUID.randomUUID().toString() + "_" + originalName.replace(" ", "_");
            Path rutaDestino = directorio.resolve(nombreArchivo);

            Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

            return "/api/v1/archivos/fotos/" + nombreArchivo;

        } catch (IOException e) {
            e.printStackTrace(); // Esto imprimirá el error real en tu consola de Spring Boot
            throw new RuntimeException("Error al guardar la fotografía en el servidor: " + e.getMessage());
        }
    }
}