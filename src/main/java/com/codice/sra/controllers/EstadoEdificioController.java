package com.codice.sra.controllers;

import com.codice.sra.models.EstadoEdificio;
import com.codice.sra.services.EstadoEdificioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-edificios")
public class EstadoEdificioController {

    @Autowired
    private EstadoEdificioService service;

    @GetMapping
    public ResponseEntity<List<EstadoEdificio>> listarParaMantenimiento() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoEdificio> crear(@RequestBody EstadoEdificio estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoEdificio> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.cambiarEstado(id, false));
    }

}