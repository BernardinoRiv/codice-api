package com.codice.sra.controllers;

import com.codice.sra.dtos.FinalizarAsistenciaDTO;
import com.codice.sra.models.Asistencia;
import com.codice.sra.services.AsistenciaCacheService;
import com.codice.sra.services.AsistenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/asistencia")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaCacheService cacheService;
    private final AsistenciaService asistenciaService;

    @PostMapping("/iniciar")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Map<String, Object>> iniciarClase(@RequestParam Long idGrupo) {
        Long idClaseCreada = asistenciaService.crearNuevaClase(idGrupo);
        cacheService.inicializarClase(idClaseCreada);

        Map<String, Object> response = new HashMap<>();
        response.put("idClase", idClaseCreada);
        response.put("mensaje", "Modo de escaneo iniciado");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/token-qr/{idClase}")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Map<String, String>> obtenerTokenRotativo(@PathVariable Long idClase) {
        String token = cacheService.generarTokenRotativo(idClase);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/presentes/{idClase}")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Map<String, Object>> obtenerPresentes(@PathVariable Long idClase) {
        Map<Long, LocalDateTime> presentesMap = cacheService.obtenerPresentes(idClase);
        List<Map<String, Object>> presentesList = presentesMap.entrySet().stream().map(entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("idEstudiante", entry.getKey());
            map.put("fechaRegistro", entry.getValue());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("presentes", presentesList);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/finalizar/{idClase}")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Map<String, Object>> finalizarClase(
            @PathVariable Long idClase,
            @RequestBody FinalizarAsistenciaDTO request) {

        Map<Long, LocalDateTime> presentes = cacheService.finalizarClaseYObtenerPresentes(idClase);
        asistenciaService.guardarAsistenciaMasiva(idClase, presentes, request.getManuales());

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Asistencia guardada en base de datos. Total presentes: " + presentes.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/historial/{idGrupo}")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<List<Map<String, Object>>> obtenerHistorialAsistencias(
            @PathVariable Long idGrupo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fecha) {

        List<Asistencia> asistencias;
        if (fecha != null) {
            asistencias = asistenciaService.obtenerHistorialPorGrupoYFecha(idGrupo, fecha);
        } else {
            asistencias = asistenciaService.obtenerHistorialAsistencias(idGrupo);
        }

        List<Map<String, Object>> resultado = asistencias.stream().map(asistencia -> {
            Map<String, Object> map = new HashMap<>();
            map.put("idAsistencia", asistencia.getIdAsistencia());
            map.put("fechaClase", asistencia.getClase().getFechaClase());
            map.put("carnet", asistencia.getInscripcion().getMatricula().getEstudiante().getCarnet());
            map.put("nombreEstudiante", asistencia.getInscripcion().getMatricula().getEstudiante().getPersona().getNombres() + " " +
                    asistencia.getInscripcion().getMatricula().getEstudiante().getPersona().getApellidos());
            map.put("estadoAsistencia", asistencia.getEstadoAsistencia().getEstadoAsistencia());
            map.put("fechaRegistro", asistencia.getFechaRegistro());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/registrar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> registrarAsistencia(
            @RequestParam String token,
            Authentication authentication) {

        Long idEstudiante = (Long) authentication.getPrincipal();
        Map<String, Object> response = new HashMap<>();

        try {
            Long idClase = cacheService.obtenerIdClasePorToken(token);
            if (idClase == null) {
                response.put("exito", false);
                response.put("mensaje", "Código inválido o caducado. Vuelve a escanear.");
                return ResponseEntity.badRequest().body(response);
            }

            if (!asistenciaService.estudiantePerteneceAClase(idClase, idEstudiante)) {
                response.put("exito", false);
                response.put("mensaje", "No estás inscrito en esta materia o grupo.");
                return ResponseEntity.badRequest().body(response);
            }

            boolean exito = cacheService.registrarAsistencia(token, idEstudiante);

            if (exito) {
                response.put("exito", true);
                response.put("mensaje", "Asistencia registrada correctamente.");
                return ResponseEntity.ok(response);
            } else {
                response.put("exito", false);
                response.put("mensaje", "El código acaba de caducar. Vuelve a escanear.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (RuntimeException e) {
            response.put("exito", false);
            response.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/modificar/{idAsistencia}")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Map<String, Object>> modificarAsistencia(
            @PathVariable Long idAsistencia,
            @RequestParam String nuevoEstado) {

        asistenciaService.actualizarEstadoAsistencia(idAsistencia, nuevoEstado);

        Map<String, Object> response = new HashMap<>();
        response.put("exito", true);
        response.put("mensaje", "Asistencia actualizada correctamente a " + nuevoEstado.toLowerCase());
        return ResponseEntity.ok(response);
    }
}