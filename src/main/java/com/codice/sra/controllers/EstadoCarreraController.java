package com.codice.sra.controllers;

import com.codice.sra.models.EstadoCarrera;
import com.codice.sra.services.EstadoCarreraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-carreras")
public class EstadoCarreraController {

    @Autowired
    private EstadoCarreraService service;

    @GetMapping
    public ResponseEntity<List<EstadoCarrera>> listarParaMantenimiento() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoCarrera> crear(@RequestBody EstadoCarrera estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoCarrera> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.cambiarEstado(id, false));
    }

}