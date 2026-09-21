package com.codice.sra.controllers;

import com.codice.sra.dtos.EmpleadoRegistroRequestDTO;
import com.codice.sra.dtos.EmpleadoRegistroResponseDTO;
import com.codice.sra.services.EmpleadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/empleados")
@Tag(name = "Gestión de Empleados")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR')")
    @Operation(summary = "Registrar nuevo empleado")
    public ResponseEntity<EmpleadoRegistroResponseDTO> crearEmpleado(@Valid @RequestBody EmpleadoRegistroRequestDTO request) {
        return ResponseEntity.ok(empleadoService.registrarEmpleado(request));
    }
}