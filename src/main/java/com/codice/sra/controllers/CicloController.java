package com.codice.sra.controllers;

import com.codice.sra.dtos.*;
import com.codice.sra.services.CicloService;
import io.swagger.v3.oas.annotations.Operation;
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
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    public ResponseEntity<SiguienteCicloSugeridoDTO> obtenerSiguienteSugerido() {
        return ResponseEntity.ok(cicloService.sugerirSiguienteCiclo());
    }

    /**
     * Registra un nuevo ciclo lectivo en estado PLANIFICACIÓN.
     */
    @PostMapping("/planificacion")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
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
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    public ResponseEntity<List<CicloPlanificacionDTO>> listarEnPlanificacion() {
        return ResponseEntity.ok(cicloService.listarCiclosEnPlanificacion());
    }

    // Botón: "Confirmar Planificación"
    @PatchMapping("/{idCiclo}/confirmar-planificacion")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Confirmar planificación: Pasa el ciclo a PLANIFICADO (permite prematrícula)")
    public ResponseEntity<CicloOperativoDTO> confirmarPlanificacion(@PathVariable Long idCiclo) {
        CicloOperativoDTO dto = cicloService.confirmarPlanificacion(idCiclo);
        return ResponseEntity.ok(dto);
    }

    // Botón: "Activar / Relevo Institucional" (o ejecutado por el Scheduler)
    @PostMapping("/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Relevo definitivo: Pasa de PLANIFICADO a ACTIVO y genera cobros masivos")
    public ResponseEntity<ActivacionCicloResponseDTO> activarCiclo(
            @Valid @RequestBody GenerarCobrosMasivosRequestDTO request) {
        ActivacionCicloResponseDTO response = cicloService.ejecutarRelevoYActivacion(request);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{idCiclo}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('EMPLEADO')")
    @Operation(summary = "Eliminar un ciclo en estado PLANIFICACIÓN",
            description = "Purga de forma atómica un ciclo lectivo en preparación junto a sus secciones y horarios asociados.")
    public ResponseEntity<Void> eliminarCicloPlanificado(@PathVariable Long idCiclo) {
        cicloService.eliminarCicloPlanificado(idCiclo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idCiclo}/aranceles-apertura")
    public ResponseEntity<ArancelesAperturaDTO> obtenerArancelesApertura(@PathVariable Long idCiclo) {
        return ResponseEntity.ok(cicloService.obtenerArancelesApertura(idCiclo));
    }
}