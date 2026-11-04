package com.codice.sra.controllers;

import com.codice.sra.dtos.DashboardDTO.DashboardGerencialDTO;
import com.codice.sra.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // RBAC: Estricto control de acceso para proteger datos gerenciales sensibles
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR_REGIONAL')")
    @GetMapping("/metricas/ciclo-activo")
    public ResponseEntity<DashboardGerencialDTO> obtenerMetricas(
            @RequestParam(defaultValue = "2-2026B") String codigoCiclo) {

        DashboardGerencialDTO metricas = dashboardService.obtenerMetricas(codigoCiclo);
        return ResponseEntity.ok(metricas);
    }
}