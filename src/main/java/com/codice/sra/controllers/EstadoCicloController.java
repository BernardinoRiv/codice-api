package com.codice.sra.controllers;

import com.codice.sra.models.EstadoCiclo;
import com.codice.sra.services.EstadoCicloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-ciclos")
public class EstadoCicloController {

    @Autowired
    private EstadoCicloService service;

    @GetMapping
    public ResponseEntity<List<EstadoCiclo>> listarParaMantenimiento() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoCiclo> crear(@RequestBody EstadoCiclo estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoCiclo> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.cambiarEstado(id, false));
    }

}