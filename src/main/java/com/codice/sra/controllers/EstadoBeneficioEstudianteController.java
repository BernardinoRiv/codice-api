package com.codice.sra.controllers;

import com.codice.sra.models.EstadoBeneficioEstudiante;
import com.codice.sra.services.EstadoBeneficioEstudianteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-beneficio-estudiantes")
public class EstadoBeneficioEstudianteController {

    @Autowired
    private EstadoBeneficioEstudianteService service;

    @GetMapping
    public ResponseEntity<List<EstadoBeneficioEstudiante>> obtenerTodos() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @PostMapping
    public ResponseEntity<EstadoBeneficioEstudiante> crear(@RequestBody EstadoBeneficioEstudiante estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoBeneficioEstudiante> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.darDeBaja(id));
    }
}