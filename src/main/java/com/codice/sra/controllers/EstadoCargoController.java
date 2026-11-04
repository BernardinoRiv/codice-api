package com.codice.sra.controllers;

import com.codice.sra.models.EstadoCargo;
import com.codice.sra.services.EstadoCargoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-cargos")
public class EstadoCargoController {

    @Autowired
    private EstadoCargoService service;

    @GetMapping
    public ResponseEntity<List<EstadoCargo>> listarParaMantenimiento() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoCargo> crear(@RequestBody EstadoCargo estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoCargo> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.cambiarEstado(id, false));
    }

}