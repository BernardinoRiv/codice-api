package com.codice.sra.controllers;

import com.codice.sra.models.EstadoRecargo;
import com.codice.sra.services.EstadoRecargoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-recargos")
public class EstadoRecargoController {
    @Autowired
    private EstadoRecargoService service;

    @GetMapping
    public ResponseEntity<List<EstadoRecargo>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoRecargo> crear(@RequestBody EstadoRecargo estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoRecargo> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}