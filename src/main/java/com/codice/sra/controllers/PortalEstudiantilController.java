package com.codice.sra.controllers;

import com.codice.sra.dtos.DashboardEstudianteDTO;
import com.codice.sra.services.PortalEstudiantilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal-estudiantil")
@RequiredArgsConstructor
public class PortalEstudiantilController {

    private final PortalEstudiantilService portalEstudiantilService;

    @GetMapping("/{carnet}/resumen")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<DashboardEstudianteDTO> obtenerResumen(@PathVariable String carnet) {
        return ResponseEntity.ok(portalEstudiantilService.obtenerResumenDashboard(carnet));
    }
}