package com.codice.sra.controllers;

import com.codice.sra.models.EstadoGrupo;
import com.codice.sra.services.EstadoGrupoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-grupos")
public class EstadoGrupoController {
    @Autowired
    private EstadoGrupoService service;

    @GetMapping
    public ResponseEntity<List<EstadoGrupo>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoGrupo> crear(@RequestBody EstadoGrupo estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoGrupo> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}