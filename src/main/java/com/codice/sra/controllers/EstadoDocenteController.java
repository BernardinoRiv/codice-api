package com.codice.sra.controllers;

import com.codice.sra.models.EstadoDocente;
import com.codice.sra.services.EstadoDocenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-docentes")
public class EstadoDocenteController {

    @Autowired
    private EstadoDocenteService service;

    @GetMapping
    public ResponseEntity<List<EstadoDocente>> listarParaMantenimiento() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoDocente> crear(@RequestBody EstadoDocente estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoDocente> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.cambiarEstado(id, false));
    }
}