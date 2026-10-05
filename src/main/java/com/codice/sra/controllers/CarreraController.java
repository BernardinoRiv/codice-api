package com.codice.sra.controllers;

import com.codice.sra.dtos.CarreraResponseDTO;
import com.codice.sra.services.CarreraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/carreras")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CarreraController {

    private final CarreraService carreraService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CarreraResponseDTO>> listarCarreras(
            @RequestParam(name = "idSede", required = false) Long idSede) {
        return ResponseEntity.ok(carreraService.listarCarrerasOfertables(idSede));
    }
}