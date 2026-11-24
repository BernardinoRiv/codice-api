package com.codice.sra.controllers;

import com.codice.sra.dtos.MateriaActualizarDTO;
import com.codice.sra.dtos.MateriaCrearDTO;
import com.codice.sra.dtos.MateriaDetalleDTO;
import com.codice.sra.dtos.MateriaEstadoDTO;
import com.codice.sra.dtos.MateriaListadoDTO;
import com.codice.sra.dtos.MateriaPrerrequisitosDTO;
import com.codice.sra.services.MateriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/materias")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaService materiaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PagedModel<MateriaListadoDTO>> listar(
            @RequestParam(name = "busqueda", defaultValue = "") String busqueda,
            @RequestParam(name = "estado", required = false) Boolean estado,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanio", defaultValue = "20") int tamanio) {
        return ResponseEntity.ok(
                new PagedModel<>(materiaService.listar(busqueda, estado, pagina, tamanio)));
    }

    @GetMapping("/{idMateria}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MateriaDetalleDTO> obtenerDetalle(@PathVariable Long idMateria) {
        return ResponseEntity.ok(materiaService.obtenerDetalle(idMateria));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MateriaDetalleDTO> crear(@Valid @RequestBody MateriaCrearDTO dto) {
        MateriaDetalleDTO materia = materiaService.crear(dto);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idMateria}")
                .buildAndExpand(materia.idMateria())
                .toUri();

        return ResponseEntity.created(ubicacion).body(materia);
    }

    @PutMapping("/{idMateria}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MateriaDetalleDTO> actualizar(
            @PathVariable Long idMateria, @Valid @RequestBody MateriaActualizarDTO dto) {
        return ResponseEntity.ok(materiaService.actualizar(idMateria, dto));
    }

    @PatchMapping("/{idMateria}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MateriaDetalleDTO> cambiarEstado(
            @PathVariable Long idMateria, @Valid @RequestBody MateriaEstadoDTO dto) {
        return ResponseEntity.ok(materiaService.cambiarEstado(idMateria, dto.estadoMateria()));
    }

    @PutMapping("/{idMateria}/prerrequisitos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MateriaDetalleDTO> reemplazarPrerrequisitos(
            @PathVariable Long idMateria, @Valid @RequestBody MateriaPrerrequisitosDTO dto) {
        return ResponseEntity.ok(
                materiaService.reemplazarPrerrequisitos(idMateria, dto.idsPrerrequisitos()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> manejarConflicto(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getBody());
    }
}
