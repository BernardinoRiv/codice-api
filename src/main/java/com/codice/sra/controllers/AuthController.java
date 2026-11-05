package com.codice.sra.controllers;

import com.codice.sra.dtos.*;
import com.codice.sra.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDTO> login(
            @Valid @RequestBody AuthLoginRequestDTO request,
            HttpServletRequest httpRequest) {

        AuthLoginResponseDTO response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cambiar-contrasena")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar contraseña del usuario autenticado")
    public ResponseEntity<CambiarContrasenaResponseDTO> cambiarContrasena(
            @Valid @RequestBody CambiarContrasenaRequestDTO request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long idUsuario = (Long) authentication.getPrincipal();

        CambiarContrasenaResponseDTO response = authService.cambiarContrasena(idUsuario, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/solicitar-recuperacion")
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDTO request) {
        String mensaje = authService.solicitarRecuperacionClave(request);
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", mensaje);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restablecer-clave")
    public ResponseEntity<CambiarContrasenaResponseDTO> restablecerClave(@Valid @RequestBody RestablecerClaveDTO request) {
        CambiarContrasenaResponseDTO response = authService.restablecerClave(request);
        if (response.isExito()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}