package com.codice.sra.controllers;

import com.codice.sra.dtos.ConfiguracionFechasEvaluacionDTO;
import com.codice.sra.dtos.ResolverSolicitudesCalificacionDTO;
import com.codice.sra.services.GestionEvaluacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gestion-evaluaciones")
public class GestionEvaluacionesController {
    private final GestionEvaluacionesService service;

    @GetMapping("/ciclos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REGISTRO_ACADEMICO')")
    public List<Map<String, Object>> ciclos() { return service.listarCiclos(); }

    @GetMapping("/ciclos/{idCiclo}/fechas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REGISTRO_ACADEMICO')")
    public Map<String, Object> fechas(@PathVariable Long idCiclo) { return service.obtenerFechas(idCiclo); }

    @PutMapping("/ciclos/{idCiclo}/fechas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REGISTRO_ACADEMICO')")
    public Map<String, Object> guardarFechas(@PathVariable Long idCiclo, @RequestBody ConfiguracionFechasEvaluacionDTO request) {
        return service.guardarFechas(idCiclo, request);
    }

    @GetMapping("/solicitudes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REGISTRO_ACADEMICO')")
    public List<Map<String, Object>> solicitudes(@RequestParam(defaultValue = "PENDIENTE") String estado) {
        return service.listarSolicitudes(estado);
    }

    @PutMapping({"/solicitudes", "/solicitudes/resolver"})
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','REGISTRO_ACADEMICO')")
    public ResponseEntity<Map<String, Object>> resolver(@RequestBody ResolverSolicitudesCalificacionDTO request) {
        return ResponseEntity.ok(Map.of("resueltas", service.resolverSolicitudes(request)));
    }

}
