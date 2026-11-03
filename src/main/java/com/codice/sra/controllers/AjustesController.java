package com.codice.sra.controllers;

import com.codice.sra.dtos.ActualizarPerfilDTO;
import com.codice.sra.dtos.CambiarPasswordDTO;
import com.codice.sra.services.AjustesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ajustes")
@RequiredArgsConstructor
public class AjustesController {

    private final AjustesService ajustesService;

    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerPerfil(Authentication auth) {
        Long idUsuario = (Long) auth.getPrincipal();
        return ResponseEntity.ok(
                ajustesService.obtenerPerfilGeneral(idUsuario)
        );
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @RequestBody ActualizarPerfilDTO dto,
            Authentication auth) {
        Long idUsuario = (Long) auth.getPrincipal();
        try {
            return ResponseEntity.ok(
                    ajustesService.actualizarDatosBasicos(idUsuario, dto)
            );
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> cambiarPassword(
            @RequestBody CambiarPasswordDTO dto,
            Authentication auth) {
        Long idUsuario = (Long) auth.getPrincipal();
        try {
            return ResponseEntity.ok(
                    ajustesService.cambiarPassword(idUsuario, dto)
            );
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/foto")
    public ResponseEntity<?> subirFotoPerfil(
            @RequestParam("foto") MultipartFile foto,
            Authentication auth) {
        Long idUsuario = (Long) auth.getPrincipal();
        try {
            return ResponseEntity.ok(
                    ajustesService.actualizarFotoPerfil(idUsuario, foto)
            );
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}