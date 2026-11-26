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
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'REGISTRO_ACADEMICO') or @permisosEmpleado.puedeRegistrarEnSede(authentication, #request.idSede, 'nuevo-ingreso')")
    public ResponseEntity<?> registrarPreMatricula(@RequestBody NuevoIngresoRequestDTO request) {
        Map<String, Object> respuesta = nuevoIngresoService.procesarPreMatricula(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
