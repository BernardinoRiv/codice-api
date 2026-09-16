package com.codice.sra.controllers;

import com.codice.sra.dtos.SolvenciaEstudianteDTO;
import com.codice.sra.services.SolvenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/solvencia")
@Tag(name = "Solvencia de Estudiantes")
public class SolvenciaController {

    @Autowired
    private SolvenciaService solvenciaService;

    @GetMapping("/grupo/{idGrupo}")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Obtener solvencia de estudiantes en un grupo")
    public ResponseEntity<List<SolvenciaEstudianteDTO>> obtenerSolvenciaPorGrupo(@PathVariable Long idGrupo) {
        List<SolvenciaEstudianteDTO> solvencias = solvenciaService.obtenerSolvenciaPorGrupo(idGrupo);
        return ResponseEntity.ok(solvencias);
    }

    @GetMapping("/inscripcion/{idInscripcion}")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Verificar solvencia de una inscripción específica")
    public ResponseEntity<Boolean> verificarSolvencia(@PathVariable Long idInscripcion) {
        Boolean esSolvente = solvenciaService.verificarSolvencia(idInscripcion);
        return ResponseEntity.ok(esSolvente);
    }
}