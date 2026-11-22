package com.codice.sra.controllers;

import com.codice.sra.dtos.InscripcionContinuaResponseDTO;
import com.codice.sra.dtos.InscribirMateriasRequestDTO;
import com.codice.sra.dtos.OfertaMatriculaEstudianteDTO;
import com.codice.sra.services.MatriculaIngresoContinuoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/estudiante/matricula")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTUDIANTE')")
public class MatriculaEstudianteController {
    private final MatriculaIngresoContinuoService matriculaService;

    @GetMapping("/oferta")
    public ResponseEntity<OfertaMatriculaEstudianteDTO> obtenerOferta(Authentication authentication) {
        return ResponseEntity.ok(matriculaService.obtenerOferta((Long) authentication.getPrincipal()));
    }

    @PostMapping("/inscribir")
    public ResponseEntity<InscripcionContinuaResponseDTO> inscribir(
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InscribirMateriasRequestDTO request) {
        return ResponseEntity.ok(matriculaService.inscribirMaterias(
                (Long) authentication.getPrincipal(), request.idsGrupos(), request, idempotencyKey));
    }
}
