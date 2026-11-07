package com.codice.sra.controllers;

import com.codice.sra.dtos.DashboardDTO.DashboardGerencialDTO;
import com.codice.sra.dtos.DashboardDTO.DashboardDocentesDTO;
import com.codice.sra.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/metricas/ciclo-activo")
    public ResponseEntity<DashboardGerencialDTO> obtenerMetricasGerenciales(
            @RequestParam(value = "codigoCiclo", required = false) String codigoCiclo,
            @RequestParam(value = "idSede", required = false) Long idSede,
            @RequestParam(value = "granularidad", defaultValue = "semanal") String granularidad) {

        DashboardGerencialDTO response = dashboardService.obtenerMetricas(codigoCiclo, idSede, granularidad);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/metricas/docentes")
    public ResponseEntity<DashboardDocentesDTO> obtenerMetricasDocentes(
            @RequestParam(value = "codigoCiclo", required = false) String codigoCiclo,
            @RequestParam(value = "idSede", required = false) Long idSede) {
        return ResponseEntity.ok(dashboardService.obtenerMetricasDocentes(codigoCiclo, idSede));
    }
}
