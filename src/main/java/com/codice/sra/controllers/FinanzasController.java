package com.codice.sra.controllers;

import com.codice.sra.dtos.EstadoCuentaCicloDTO;
import com.codice.sra.dtos.EstadoCuentaResponseDTO;
import com.codice.sra.dtos.GenerarCobrosMasivosRequestDTO;
import com.codice.sra.dtos.ProcesarPagoRequestDTO;
import com.codice.sra.models.Beneficio;
import com.codice.sra.repositories.BeneficioRepository;
import com.codice.sra.services.FinanzasService;
import com.codice.sra.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/finanzas")
@RequiredArgsConstructor
public class FinanzasController {

    private final FinanzasService finanzasService;
    private final JwtService jwtService;
    private final BeneficioRepository beneficioRepository;

    // ====================================================================
    // NUEVO: CLONAR ARANCELES DEL CICLO PASADO AL NUEVO
    // ====================================================================
    @PostMapping("/clonar-aranceles")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('FINANZAS')")
    public ResponseEntity<Map<String, Object>> clonarAranceles(
            @RequestParam Long idCicloAnterior,
            @RequestParam Long idCicloNuevo) {

        int cantidad = finanzasService.clonarArancelesCicloAnterior(idCicloAnterior, idCicloNuevo);

        Map<String, Object> response = new HashMap<>();
        response.put("exito", true);
        response.put("mensaje", "Se clonaron " + cantidad + " aranceles exitosamente al nuevo ciclo.");

        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ACTUALIZADO: GENERACIÓN MASIVA CON EL DTO
    // ====================================================================
    @PostMapping("/generar-cargos-apertura")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('FINANZAS')")
    public ResponseEntity<Map<String, Object>> generarCobrosMasivos(
            @Valid @RequestBody GenerarCobrosMasivosRequestDTO request) {

        int generados = finanzasService.generarCobrosMatriculaAperturaCiclo(request);

        Map<String, Object> response = new HashMap<>();
        response.put("exito", true);
        response.put("mensaje", "Proceso Batch finalizado. Se generaron " + generados + " cargos financieros.");
        response.put("cargosGenerados", generados);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/estudiantes/{identificador}/estado-cuenta")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<?> obtenerEstadoCuentaVentanilla(@PathVariable String identificador) {
        try {
            // Llamamos al nuevo método que busca por Carnet o por DUI/NIT
            EstadoCuentaResponseDTO estadoCuenta = finanzasService.consultarEstadoCuentaPorCarnetODocumento(identificador);
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

            // EL 1L AL FINAL INDICA CANAL "VENTANILLA"
            Map<String, Object> resultado = finanzasService.procesarPagoGral(request, correoCajero, idSedeCajero, 1L);

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

    // ====================================================================
    // ENDPOINTS DE GESTIÓN DE ARANCELES (CRUD)
    // ====================================================================

    @GetMapping("/ciclos/{idCiclo}/aranceles")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('FINANZAS')")
    public ResponseEntity<List<Map<String, Object>>> listarArancelesDelCiclo(@PathVariable Long idCiclo) {
        return ResponseEntity.ok(finanzasService.obtenerArancelesPorCiclo(idCiclo));
    }

    @PutMapping("/aranceles/{idConcepto}")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<Map<String, Object>> actualizarPrecioArancel(
            @PathVariable Long idConcepto,
            @RequestParam BigDecimal nuevoMonto) {

        try {
            Map<String, Object> resultado = finanzasService.actualizarPrecioArancel(idConcepto, nuevoMonto);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("exito", false);
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/ciclos/{idCiclo}/aranceles")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<Map<String, Object>> crearNuevoArancel(
            @PathVariable Long idCiclo,
            @RequestBody Map<String, Object> payload) {
        try {
            String conceptoStr = (String) payload.get("concepto");
            String montoStr = String.valueOf(payload.get("montoBase"));

            if (conceptoStr == null || conceptoStr.trim().isEmpty() || montoStr == null || montoStr.trim().isEmpty()) {
                throw new RuntimeException("Debe proporcionar un concepto y un monto válido.");
            }

            BigDecimal montoBase = new BigDecimal(montoStr);
            Map<String, Object> resultado = finanzasService.crearNuevoArancelEnCiclo(idCiclo, conceptoStr, montoBase);

            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("exito", false);
            error.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/estudiantes/{carnet}/estado-cuenta/ciclos/{idCiclo}")
    @PreAuthorize("hasRole('ESTUDIANTE') or hasRole('FINANZAS')")
    public ResponseEntity<?> obtenerEstadoCuentaPorCiclo(
            @PathVariable String carnet,
            @PathVariable Long idCiclo) {
        try {
            EstadoCuentaCicloDTO estadoCuenta = finanzasService.consultarEstadoCuentaPorCiclo(carnet, idCiclo);
            return ResponseEntity.ok(estadoCuenta);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // ====================================================================
    // NUEVO: PAGO EN LÍNEA PARA ESTUDIANTES (Self-Service)
    // ====================================================================
    @PostMapping("/estudiantes/pagos/en-linea")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<?> procesarPagoEnLinea(
            @Valid @RequestBody ProcesarPagoRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String correoEstudiante = jwtService.extractUsername(token);

            // EL 2L AL FINAL INDICA CANAL "PORTAL"
            Map<String, Object> resultado = finanzasService.procesarPagoGral(request, correoEstudiante, 1L, 2L);

            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno al procesar el pago online: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ACA ESTA LA CORRECCIÓN: Se agregó <Map<String, Object>> dentro del List
    @GetMapping("/estudiantes/{carnet}/ciclos")
    @PreAuthorize("hasRole('ESTUDIANTE') or hasRole('FINANZAS')")
    public ResponseEntity<List<Map<String, Object>>> obtenerCiclosEstudiante(@PathVariable String carnet) {
        return ResponseEntity.ok(finanzasService.obtenerCiclosPorEstudiante(carnet));
    }

    @GetMapping("/pagos/{idPago}/comprobante")
    @PreAuthorize("hasRole('ESTUDIANTE') or hasRole('FINANZAS')")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Long idPago) {
        byte[] pdfBytes = finanzasService.descargarComprobanteHistorico(idPago);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Factura.pdf");
        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/pagos/historial")
    public ResponseEntity<?> obtenerHistorialPagos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if(fecha == null) fecha = LocalDate.now();
        return ResponseEntity.ok(finanzasService.obtenerPagosPorFecha(fecha));
    }

    @PostMapping("/pagos/{idPago}/reenviar-comprobante")
    public ResponseEntity<?> reenviarComprobante(@PathVariable Long idPago) {
        return ResponseEntity.ok(finanzasService.reenviarComprobantePorCorreo(idPago));
    }

    @GetMapping("/beneficios")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'REGISTRO_ACADEMICO', 'EMPLEADO')")
    public ResponseEntity<List<Map<String, Object>>> obtenerBeneficiosActivos() {
        List<Map<String, Object>> lista = beneficioRepository.findAll().stream().map(b -> {
            Map<String, Object> map = new HashMap<>();

            // Usamos exactamente los getters de tu entidad
            map.put("id", b.getIdBeneficio());
            map.put("nombre", b.getBeneficio()); // El frontend espera 'nombre'
            map.put("porcentaje", b.getPorcentaje());
            map.put("monto", b.getMonto());
            map.put("cumMinimo", b.getCumMinimo());
            map.put("fechaInicio", b.getFechaInicio());
            map.put("fechaFin", b.getFechaFin());


            return map;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/aranceles/ciclo/{idCiclo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'REGISTRO_ACADEMICO', 'EMPLEADO')")
    public ResponseEntity<List<Map<String, Object>>> obtenerArancelesCicloParaFrontend(@PathVariable Long idCiclo) {
        return ResponseEntity.ok(finanzasService.obtenerArancelesPorCiclo(idCiclo));
    }

}