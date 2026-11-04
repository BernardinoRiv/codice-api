package com.codice.sra.controllers;

import com.codice.sra.models.EstadoMatricula;
import com.codice.sra.services.EstadoMatriculaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-matriculas")
public class EstadoMatriculaController {
    @Autowired
    private EstadoMatriculaService service;

    @GetMapping
    public ResponseEntity<List<EstadoMatricula>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoMatricula> crear(@RequestBody EstadoMatricula estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoMatricula> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}