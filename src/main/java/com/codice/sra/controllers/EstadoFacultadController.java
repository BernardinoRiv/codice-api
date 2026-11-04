package com.codice.sra.controllers;

import com.codice.sra.models.EstadoFacultad;
import com.codice.sra.services.EstadoFacultadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-facultads")
public class EstadoFacultadController {
    @Autowired
    private EstadoFacultadService service;

    @GetMapping
    public ResponseEntity<List<EstadoFacultad>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoFacultad> crear(@RequestBody EstadoFacultad estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoFacultad> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}