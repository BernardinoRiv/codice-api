package com.codice.sra.controllers;

import com.codice.sra.models.EstadoTrayectoria;
import com.codice.sra.services.EstadoTrayectoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-trayectorias")
public class EstadoTrayectoriaController {
    @Autowired
    private EstadoTrayectoriaService service;

    @GetMapping
    public ResponseEntity<List<EstadoTrayectoria>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoTrayectoria> crear(@RequestBody EstadoTrayectoria estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoTrayectoria> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}