package com.codice.sra.controllers;

import com.codice.sra.models.EstadoAsistencia;
import com.codice.sra.services.EstadoAsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vi/estado-asistencias")
public class EstadoAsistenciaController {

    @Autowired
    private EstadoAsistenciaService service;

    @GetMapping
    public ResponseEntity<List<EstadoAsistencia>> obtenerTodos() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoAsistencia> crear(@RequestBody EstadoAsistencia estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoAsistencia> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}