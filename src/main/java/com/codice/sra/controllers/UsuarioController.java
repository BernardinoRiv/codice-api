package com.codice.sra.controllers;

import com.codice.sra.dtos.UsuarioRegistroRequestDTO;
import com.codice.sra.dtos.UsuarioRegistroResponseDTO;
import com.codice.sra.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioRegistroResponseDTO> registrar(@Valid @RequestBody UsuarioRegistroRequestDTO request) {
        UsuarioRegistroResponseDTO response = usuarioService.registrarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}