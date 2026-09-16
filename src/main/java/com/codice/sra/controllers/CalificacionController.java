package com.codice.sra.controllers;

import com.codice.sra.dtos.CalificacionRegistroRequestDTO;
import com.codice.sra.dtos.CalificacionResponseDTO;
import com.codice.sra.services.CalificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calificaciones")
public class CalificacionController {

    private final CalificacionService calificacionService;

    public CalificacionController(CalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    @PostMapping
    public ResponseEntity<CalificacionResponseDTO> registrarCalificacion(
            @Valid @RequestBody CalificacionRegistroRequestDTO request) {

        Long idUsuarioDocente = obtenerIdUsuarioAutenticado();

        CalificacionResponseDTO response = calificacionService.registrarCalificacion(request, idUsuarioDocente);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}