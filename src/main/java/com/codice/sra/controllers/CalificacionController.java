package com.codice.sra.controllers;

import com.codice.sra.dtos.CalificacionRegistroRequestDTO;
import com.codice.sra.dtos.CalificacionResponseDTO;
import com.codice.sra.dtos.PlantillaNotasResponseDTO;
import com.codice.sra.services.CalificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calificaciones")
@Tag(name = "Gestión de Calificaciones", description = "Endpoints para ingreso individual y carga masiva de notas")
public class CalificacionController {

    private final CalificacionService calificacionService;

    public CalificacionController(CalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    // ==========================================
    // 1. REGISTRO INDIVIDUAL
    // ==========================================

    @PostMapping
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Registrar una calificación individual")
    public ResponseEntity<CalificacionResponseDTO> registrarCalificacion(
            @Valid @RequestBody CalificacionRegistroRequestDTO request) {

        Long idUsuarioDocente = obtenerIdUsuarioAutenticado();
        CalificacionResponseDTO response = calificacionService.registrarCalificacion(request, idUsuarioDocente);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // 2. CARGA MASIVA (EXCEL)
    // ==========================================

    @GetMapping("/plantilla/{idGrupo}")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Descargar plantilla Excel pre-llenada para carga masiva")
    public ResponseEntity<byte[]> descargarPlantilla(@PathVariable Long idGrupo) {

        PlantillaNotasResponseDTO plantilla = calificacionService.generarPlantilla(idGrupo);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + plantilla.getNombreArchivo() + "\"")
                .contentType(MediaType.parseMediaType(plantilla.getTipoContenido()))
                .body(plantilla.getContenido());
    }

    @PostMapping("/carga-masiva/{idGrupo}")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Cargar notas masivamente desde un archivo Excel")
    public ResponseEntity<List<CalificacionResponseDTO>> cargarNotasMasivamente(
            @PathVariable Long idGrupo,
            @RequestParam("archivo") MultipartFile archivo) {

        Long idUsuarioDocente = obtenerIdUsuarioAutenticado();

        List<CalificacionResponseDTO> respuestas = calificacionService.cargarNotasDesdeExcel(
                archivo,
                idGrupo,
                idUsuarioDocente
        );

        return ResponseEntity.ok(respuestas);
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private Long obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}