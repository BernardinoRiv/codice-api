package com.codice.sra.controllers;

import com.codice.sra.dtos.DocenteDashboardResponseDTO;
import com.codice.sra.services.PanelDocenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/docentes/panel")
public class DocenteDashboardController {

    private final PanelDocenteService panelDocenteService;

    public DocenteDashboardController(PanelDocenteService panelDocenteService) {
        this.panelDocenteService = panelDocenteService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DocenteDashboardResponseDTO> obtenerDashboard(@AuthenticationPrincipal Long idUsuario) {
        // Como tu JwtAuthenticationFilter inyecta el ID como Principal, Spring lo mapea automáticamente aquí.
        return ResponseEntity.ok(panelDocenteService.obtenerPanelPrincipal(idUsuario));
    }
}