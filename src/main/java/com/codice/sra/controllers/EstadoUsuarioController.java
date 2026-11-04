package com.codice.sra.controllers;

import com.codice.sra.models.EstadoUsuario;
import com.codice.sra.services.EstadoUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-usuarios")
public class EstadoUsuarioController {
    @Autowired
    private EstadoUsuarioService service;

    @GetMapping
    public ResponseEntity<List<EstadoUsuario>> listar() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoUsuario> crear(@RequestBody EstadoUsuario estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoUsuario> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}