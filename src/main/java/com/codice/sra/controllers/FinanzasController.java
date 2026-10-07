package com.codice.sra.controllers;

import com.codice.sra.dtos.EstadoCuentaResponseDTO;
import com.codice.sra.dtos.ProcesarPagoRequestDTO;
import com.codice.sra.services.FinanzasService;
import com.codice.sra.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/finanzas")
@RequiredArgsConstructor
public class FinanzasController {

    private final FinanzasService finanzasService;
    private final JwtService jwtService;

    @PostMapping("/ciclos/{idCiclo}/generar-matriculas")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'FINANZAS')")
    public ResponseEntity<Map<String, Object>> generarCobrosMasivos(@PathVariable Long idCiclo) {
        Map<String, Object> response = new HashMap<>();
        try {
            int cobrosGenerados = finanzasService.generarCobrosMatriculaAperturaCiclo(idCiclo);
            response.put("exito", true);
            response.put("mensaje", "Proceso financiero de apertura de ciclo ejecutado con éxito.");
            response.put("cobrosNuevosGenerados", cobrosGenerados);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("exito", false);
            response.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("exito", false);
            response.put("mensaje", "Ocurrió un error interno al generar los cobros masivos.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/estudiantes/{carnet}/estado-cuenta")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<?> obtenerEstadoCuentaVentanilla(@PathVariable String carnet) {
        try {
            EstadoCuentaResponseDTO estadoCuenta = finanzasService.consultarEstadoCuentaPorCarnet(carnet);
            return ResponseEntity.ok(estadoCuenta);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @PostMapping("/pagos/procesar")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<?> procesarPagoVentanilla(
            @Valid @RequestBody ProcesarPagoRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String correoCajero = jwtService.extractUsername(token);
            Number idSedeNumber = jwtService.extractClaim(token, claims -> claims.get("idSede", Number.class));
            Long idSedeCajero = idSedeNumber != null ? idSedeNumber.longValue() : 1L;

            Map<String, Object> resultado = finanzasService.procesarPagoVentanilla(request, correoCajero, idSedeCajero);

            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno al procesar el pago: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/aranceles-extras")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<List<Map<String, Object>>> obtenerArancelesExtras() {
        try {
            List<Map<String, Object>> aranceles = finanzasService.obtenerCatalogoArancelesExtras();
            return ResponseEntity.ok(aranceles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}