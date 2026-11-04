package com.codice.sra.controllers;

import com.codice.sra.models.EstadoMateriaEstudiante;
import com.codice.sra.services.EstadoMateriaEstudianteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-materia-estudiantes")
public class EstadoMateriaEstudianteController {
    @Autowired
    private EstadoMateriaEstudianteService service;

    @GetMapping
    public ResponseEntity<List<EstadoMateriaEstudiante>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoMateriaEstudiante> crear(@RequestBody EstadoMateriaEstudiante estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoMateriaEstudiante> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}