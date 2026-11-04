package com.codice.sra.controllers;

import com.codice.sra.models.EstadoEstudiante;
import com.codice.sra.services.EstadoEstudianteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-estudiantes")
public class EstadoEstudianteController {
    @Autowired
    private EstadoEstudianteService service;

    @GetMapping
    public ResponseEntity<List<EstadoEstudiante>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoEstudiante> crear(@RequestBody EstadoEstudiante estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoEstudiante> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}