package com.codice.sra.controllers;

import com.codice.sra.dtos.NuevoIngresoRequestDTO;
import com.codice.sra.services.NuevoIngresoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nuevo-ingreso")
@RequiredArgsConstructor
public class NuevoIngresoController {

    private final NuevoIngresoService nuevoIngresoService;

    @PostMapping("/pre-matricula")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'REGISTRO_ACADEMICO', 'EMPLEADO')")
    public ResponseEntity<?> registrarPreMatricula(@RequestBody NuevoIngresoRequestDTO request) {
        try {
            // El servicio ejecutará toda la lógica transaccional de la Fase 1
            Map<String, Object> respuesta = nuevoIngresoService.procesarPreMatricula(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("mensaje", "Error interno al procesar la pre-matrícula: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}