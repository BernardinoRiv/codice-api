package com.codice.sra.controllers;

import com.codice.sra.models.EstadoPago;
import com.codice.sra.services.EstadoPagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-pagos")
public class EstadoPagoController {
    @Autowired
    private EstadoPagoService service;

    @GetMapping
    public ResponseEntity<List<EstadoPago>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoPago> crear(@RequestBody EstadoPago estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoPago> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}