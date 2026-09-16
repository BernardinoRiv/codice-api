package com.codice.sra.controllers;

import com.codice.sra.dtos.CalificacionResponseDTO;
import com.codice.sra.dtos.EvaluacionResponseDTO;
import com.codice.sra.dtos.InscripcionResponseDTO;
import com.codice.sra.services.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grupos")
@Tag(name = "Gestión de Grupos")
public class GrupoController {

    @Autowired
    private GrupoService grupoService;

    @GetMapping("/{idGrupo}/inscripciones")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Obtener inscripciones de un grupo")
    public ResponseEntity<List<InscripcionResponseDTO>> obtenerInscripciones(@PathVariable Long idGrupo) {
        return ResponseEntity.ok(grupoService.obtenerInscripcionesConEstudiantes(idGrupo));
    }

    @GetMapping("/{idGrupo}/evaluaciones")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Obtener evaluaciones de un grupo")
    public ResponseEntity<List<EvaluacionResponseDTO>> obtenerEvaluaciones(@PathVariable Long idGrupo) {
        return ResponseEntity.ok(grupoService.obtenerEvaluacionesPorGrupo(idGrupo));
    }

    @GetMapping("/{idGrupo}/calificaciones")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Obtener calificaciones de un grupo")
    public ResponseEntity<List<CalificacionResponseDTO>> obtenerCalificaciones(@PathVariable Long idGrupo) {
        return ResponseEntity.ok(grupoService.obtenerCalificacionesPorGrupo(idGrupo));
    }
}