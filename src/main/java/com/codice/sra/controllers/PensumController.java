package com.codice.sra.controllers;

import com.codice.sra.dtos.PensumDetalleDTO;
import com.codice.sra.dtos.PensumListadoDTO;
import com.codice.sra.services.PensumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pensum")
@RequiredArgsConstructor
public class PensumController {

    private final PensumService pensumService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<PensumListadoDTO>> listarTodos() {
        return ResponseEntity.ok(pensumService.listarTodos());
    }

    @GetMapping("/{idPensum}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PensumDetalleDTO> obtenerDetalle(@PathVariable Long idPensum) {
        return ResponseEntity.ok(pensumService.obtenerDetalle(idPensum));
    }
}
