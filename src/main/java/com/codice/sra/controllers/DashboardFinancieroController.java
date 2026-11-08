package com.codice.sra.controllers;

import com.codice.sra.services.DashboardFinancieroService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard/metricas")
@RequiredArgsConstructor
public class DashboardFinancieroController {

    private final DashboardFinancieroService service;

    @GetMapping("/cobros")
    public ResponseEntity<JsonNode> obtenerCobros(
            @RequestParam(required = false) String codigoCiclo,
            @RequestParam(required = false) Long idSede
    ) {

        return ResponseEntity.ok(
                service.obtenerDashboardCompleto(
                        codigoCiclo,
                        idSede
                )
        );
    }
}