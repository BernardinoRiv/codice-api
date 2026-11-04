package com.codice.sra.controllers;

import com.codice.sra.models.EstadoInscripcion;
import com.codice.sra.services.EstadoInscripcionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-inscripcions")
public class EstadoInscripcionController {
    @Autowired
    private EstadoInscripcionService service;

    @GetMapping
    public ResponseEntity<List<EstadoInscripcion>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoInscripcion> crear(@RequestBody EstadoInscripcion estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoInscripcion> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}