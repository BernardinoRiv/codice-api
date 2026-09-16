package com.codice.sra.controllers;

import com.codice.sra.dtos.DocenteListaResponseDTO;
import com.codice.sra.dtos.DocenteRegistroRequestDTO;
import com.codice.sra.dtos.DocenteRegistroResponseDTO;
import com.codice.sra.dtos.GrupoResponseDTO;
import com.codice.sra.models.Grupo;
import com.codice.sra.services.DocenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    @Operation(summary = "Obtener lista de todos los docentes registrados")
    public ResponseEntity<List<DocenteListaResponseDTO>> obtenerTodosLosDocentes() {
        List<DocenteListaResponseDTO> docentes = docenteService.obtenerTodosLosDocentes();
        return ResponseEntity.ok(docentes);
    }

    private Long obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}