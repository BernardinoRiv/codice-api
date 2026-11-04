package com.codice.sra.controllers;

import com.codice.sra.models.EstadoPensum;
import com.codice.sra.services.EstadoPensumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-pensums")
public class EstadoPensumController {
    @Autowired
    private EstadoPensumService service;

    @GetMapping
    public ResponseEntity<List<EstadoPensum>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoPensum> crear(@RequestBody EstadoPensum estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoPensum> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}