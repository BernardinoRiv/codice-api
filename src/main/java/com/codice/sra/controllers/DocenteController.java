package com.codice.sra.controllers;

import com.codice.sra.dtos.*;
import com.codice.sra.models.Grupo;
import com.codice.sra.services.DocenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/docentes")
@Tag(name = "Gestión de Docentes")
public class DocenteController {

    @Autowired
    private DocenteService docenteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR')")
    @Operation(summary = "Registrar nuevo docente")
    public ResponseEntity<DocenteRegistroResponseDTO> crearDocente(@Valid @RequestBody DocenteRegistroRequestDTO request) {
        return ResponseEntity.ok(docenteService.registrarDocente(request));
    }

    @GetMapping("/grupos")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<List<GrupoResponseDTO>> obtenerGruposDelDocente() {
        Long idUsuario = obtenerIdUsuarioAutenticado();
        List<GrupoResponseDTO> grupos = docenteService.obtenerGruposPorDocenteDTO(idUsuario);
        return ResponseEntity.ok(grupos);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR')")
    @Operation(summary = "Obtener lista de todos los docentes registrados")
    public ResponseEntity<List<DocenteListaResponseDTO>> obtenerTodosLosDocentes() {
        List<DocenteListaResponseDTO> docentes = docenteService.obtenerTodosLosDocentes();
        return ResponseEntity.ok(docentes);
    }

    private Long obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }

    @PutMapping("/{idDocente}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar docente", description = "Actualiza los datos personales permitidos y la ficha contractual del docente.")
    public ResponseEntity<Void> actualizarDocente(
            @PathVariable Long idDocente,
            @Valid @RequestBody DocenteEdicionRequestDTO request) {
        docenteService.actualizarDocente(idDocente, request);
        return ResponseEntity.noContent().build(); // Retorna HTTP 204 sin cuerpo
    }

    @PatchMapping("/{idDocente}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    @Operation(
            summary = "Cambiar estado del docente",
            description = "Permite a un Administrador bloquear o reactivar a un docente justificando el motivo en auditoría."
    )
    public ResponseEntity<Void> cambiarEstadoDocente(
            @PathVariable Long idDocente,
            @Valid @RequestBody DocenteEstadoRequestDTO request) {

        docenteService.cambiarEstadoDocente(idDocente, request);
        return ResponseEntity.noContent().build(); // HTTP 204 No Content
    }
}