package com.codice.sra.controllers;

import com.codice.sra.dtos.CicloOperativoDTO;
import com.codice.sra.dtos.CicloPlanificacionDTO;
import com.codice.sra.dtos.CrearCicloPlanificacionDTO;
import com.codice.sra.dtos.SiguienteCicloSugeridoDTO;
import com.codice.sra.services.CicloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ciclos")
@RequiredArgsConstructor
public class CicloController {

    private final CicloService cicloService;

    /**
     * Endpoint consumido por la pantalla de Apertura de Sección.
     * Retorna el ciclo lectivo operativo (prioridad PLANIFICACIÓN, fallback ACTIVO)
     * para fijarlo en modo solo lectura (readonly).
     */
    @GetMapping("/operativo-oferta")
    public ResponseEntity<CicloOperativoDTO> obtenerCicloOperativo() {
        return ResponseEntity.ok(cicloService.obtenerCicloParaOferta());
    }

    /**
     * Retorna la proyección matemática del siguiente ciclo (ej. '01-2027')
     * para prellenar los campos bloqueados del modal de planificación.
     */
    @GetMapping("/siguiente-sugerido")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    public ResponseEntity<SiguienteCicloSugeridoDTO> obtenerSiguienteSugerido() {
        return ResponseEntity.ok(cicloService.sugerirSiguienteCiclo());
    }

    /**
     * Registra un nuevo ciclo lectivo en estado PLANIFICACIÓN.
     */
    @PostMapping("/planificacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    public ResponseEntity<CicloOperativoDTO> crearCicloPlanificado(
            @RequestBody(required = false) CrearCicloPlanificacionDTO dto) {
        CicloOperativoDTO creado = cicloService.registrarCicloPlanificado(
                dto != null ? dto : new CrearCicloPlanificacionDTO());

        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Lista todos los periodos en estado PLANIFICACIÓN junto con el número de secciones
     * ya configuradas, alimentando la tabla de relevo institucional.
     */
    @GetMapping("/en-planificacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    public ResponseEntity<List<CicloPlanificacionDTO>> listarEnPlanificacion() {
        return ResponseEntity.ok(cicloService.listarCiclosEnPlanificacion());
    }

    /**
     * Ejecuta la transición de estado atómica institucional:
     * El ciclo activo actual pasa a 'FINALIZADO' y el ciclo planificado pasa a 'ACTIVO'.
     */
    @PostMapping("/{idCiclo}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, String>> activarCicloPlanificado(
            @PathVariable Long idCiclo) {
        cicloService.promoverCicloAActivo(idCiclo);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Ciclo activado exitosamente. El periodo anterior ha sido finalizado automáticamente."
        ));
    }
}