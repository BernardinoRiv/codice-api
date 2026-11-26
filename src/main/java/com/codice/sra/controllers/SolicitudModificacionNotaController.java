package com.codice.sra.controllers;

import com.codice.sra.dtos.SolicitudModificacionNotaDTO;
import com.codice.sra.services.GestionEvaluacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/docente/notas")
public class SolicitudModificacionNotaController {
    private final GestionEvaluacionesService service;

    @PostMapping("/solicitudes-modificacion")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Map<String, Object>> crear(@RequestBody SolicitudModificacionNotaDTO request) {
        List<Long> ids = service.crearSolicitud(request);
        return ResponseEntity.status(201).body(Map.of("idsSolicitud", ids, "creadas", ids.size()));
    }
}
