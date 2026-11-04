package com.codice.sra.controllers;

import com.codice.sra.models.EstadoFactura;
import com.codice.sra.services.EstadoFacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-facturas")
public class EstadoFacturaController {
    @Autowired
    private EstadoFacturaService service;

    @GetMapping
    public ResponseEntity<List<EstadoFactura>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoFactura> crear(@RequestBody EstadoFactura estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoFactura> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}