package com.codice.sra.controllers;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/archivos")
public class ArchivoController {

    // SE CORRIGIÓ LA RUTA: Ahora apunta a "uploads/fotos_perfil" para coincidir con tu carpeta real
    private final String uploadDir = "uploads/fotos_perfil";

    @GetMapping("/fotos/{nombreArchivo:.+}")
    public ResponseEntity<Resource> obtenerFoto(@PathVariable String nombreArchivo) {
        try {
            Path rutaArchivo = Paths.get(uploadDir).resolve(nombreArchivo).normalize();
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() && recurso.isReadable()) {
                String contentType = Files.probeContentType(rutaArchivo);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                        .body(recurso);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}