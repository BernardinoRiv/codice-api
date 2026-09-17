package com.codice.sra.controllers;

import com.codice.sra.dtos.AuthLoginRequestDTO;
import com.codice.sra.dtos.AuthLoginResponseDTO;
import com.codice.sra.dtos.CambiarContrasenaRequestDTO;
import com.codice.sra.dtos.CambiarContrasenaResponseDTO;
import com.codice.sra.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDTO> login(@Valid @RequestBody AuthLoginRequestDTO request) {
        AuthLoginResponseDTO response = authService.login(request);
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
}