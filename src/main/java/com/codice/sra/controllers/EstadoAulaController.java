package com.codice.sra.controllers;

import com.codice.sra.models.EstadoAula;
import com.codice.sra.services.EstadoAulaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-aulas")
public class EstadoAulaController {

    @Autowired
    private EstadoAulaService service;

    @GetMapping
    public ResponseEntity<List<EstadoAula>> listar() {
        return ResponseEntity.ok(service.obtenerActivos());
    }

    @PostMapping
    public ResponseEntity<EstadoAula> crear(@RequestBody EstadoAula estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoAula> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}