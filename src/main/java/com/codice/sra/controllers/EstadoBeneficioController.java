package com.codice.sra.controllers;

import com.codice.sra.models.EstadoBeneficio;
import com.codice.sra.services.EstadoBeneficioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-beneficios")
public class EstadoBeneficioController {

    @Autowired
    private EstadoBeneficioService service;

    @GetMapping
    public ResponseEntity<List<EstadoBeneficio>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoBeneficio> crear(@RequestBody EstadoBeneficio estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoBeneficio> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}