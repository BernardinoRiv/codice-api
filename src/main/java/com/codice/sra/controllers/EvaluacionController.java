package com.codice.sra.controllers;

import com.codice.sra.services.EvaluacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/evaluaciones")
@Tag(name = "Gestión de Evaluaciones")
public class EvaluacionController {

    @Autowired
    private EvaluacionService evaluacionService;

    @PostMapping("/{idEvaluacion}/publicar")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Publicar calificaciones de una evaluación")
    public ResponseEntity<?> publicarEvaluacion(@PathVariable Long idEvaluacion) {
        try {
            evaluacionService.publicarEvaluacion(idEvaluacion);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            String mensaje = e.getMessage() == null ? "No se pudo publicar la evaluación." : e.getMessage();
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", mensaje));
        }
    }
}
