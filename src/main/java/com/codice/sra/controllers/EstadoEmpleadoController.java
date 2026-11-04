package com.codice.sra.controllers;

import com.codice.sra.models.EstadoEmpleado;
import com.codice.sra.services.EstadoEmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-empleados")
public class EstadoEmpleadoController {
    @Autowired
    private EstadoEmpleadoService service;

    @GetMapping
    public ResponseEntity<List<EstadoEmpleado>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoEmpleado> crear(@RequestBody EstadoEmpleado estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoEmpleado> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}