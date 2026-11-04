package com.codice.sra.controllers;

import com.codice.sra.models.EstadoSede;
import com.codice.sra.services.EstadoSedeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-sedes")
public class EstadoSedeController {
    @Autowired
    private EstadoSedeService service;

    @GetMapping
    public ResponseEntity<List<EstadoSede>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoSede> crear(@RequestBody EstadoSede estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoSede> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}