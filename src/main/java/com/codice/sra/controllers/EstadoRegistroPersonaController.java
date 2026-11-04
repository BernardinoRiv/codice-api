package com.codice.sra.controllers;

import com.codice.sra.models.EstadoRegistroPersona;
import com.codice.sra.services.EstadoRegistroPersonaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-registro-personas")
public class EstadoRegistroPersonaController {
    @Autowired
    private EstadoRegistroPersonaService service;

    @GetMapping
    public ResponseEntity<List<EstadoRegistroPersona>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoRegistroPersona> crear(@RequestBody EstadoRegistroPersona estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoRegistroPersona> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}