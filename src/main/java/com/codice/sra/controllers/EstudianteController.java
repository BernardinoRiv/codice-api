package com.codice.sra.controllers;

import com.codice.sra.dtos.NotasEstudianteResponseDTO;
import com.codice.sra.models.Estudiante;
import com.codice.sra.repositories.EstudianteRepository;
import com.codice.sra.services.EstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estudiantes")
@Tag(name = "Estudiantes - Notas")
public class EstudianteController {

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @GetMapping("/notas")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'ADMIN')")
    @Operation(summary = "Obtener notas del ciclo actual")
    public ResponseEntity<NotasEstudianteResponseDTO> obtenerNotas() {
        Long idEstudianteReal = obtenerIdEstudianteAutenticado();
        return ResponseEntity.ok(estudianteService.obtenerNotasCicloActual(idEstudianteReal));
    }

    private Long obtenerIdEstudianteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long idUsuario = (Long) authentication.getPrincipal();

        Estudiante estudiante = estudianteRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new RuntimeException("No se encontró el perfil de estudiante para este usuario."));

        return estudiante.getIdEstudiante();
    }
}