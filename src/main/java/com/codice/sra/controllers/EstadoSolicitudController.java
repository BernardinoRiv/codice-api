package com.codice.sra.controllers;

import com.codice.sra.models.EstadoSolicitud;
import com.codice.sra.services.EstadoSolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-solicituds")
public class EstadoSolicitudController {
    @Autowired
    private EstadoSolicitudService service;

    @GetMapping
    public ResponseEntity<List<EstadoSolicitud>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoSolicitud> crear(@RequestBody EstadoSolicitud estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoSolicitud> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}