package com.codice.sra.controllers;

import com.codice.sra.dtos.DocenteRegistroRequestDTO;
import com.codice.sra.dtos.DocenteRegistroResponseDTO;
import com.codice.sra.services.DocenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/docentes")
@Tag(name = "Gestión de Docentes")
public class DocenteController {

    @Autowired
    private DocenteService docenteService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar nuevo docente")
    public ResponseEntity<DocenteRegistroResponseDTO> crearDocente(@Valid @RequestBody DocenteRegistroRequestDTO request) {
        return ResponseEntity.ok(docenteService.registrarDocente(request));
    }
}