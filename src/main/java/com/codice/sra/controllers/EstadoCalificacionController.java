package com.codice.sra.controllers;

import com.codice.sra.models.EstadoCalificacion;
import com.codice.sra.services.EstadoCalificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estado-calificaciones")
public class EstadoCalificacionController {

    @Autowired
    private EstadoCalificacionService service;

    @GetMapping
    public ResponseEntity<List<EstadoCalificacion>> listarParaMantenimiento() {
        return ResponseEntity.ok(service.obtenerTodos());
    }


    @PostMapping
    public ResponseEntity<EstadoCalificacion> crear(@RequestBody EstadoCalificacion estado) {
        return ResponseEntity.ok(service.guardar(estado));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<EstadoCalificacion> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(service.cambiarEstado(id, false));
    }

}