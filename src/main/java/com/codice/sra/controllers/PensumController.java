package com.codice.sra.controllers;

import com.codice.sra.dtos.PensumCatalogoMateriaDTO;
import com.codice.sra.dtos.PensumDetalleDTO;
import com.codice.sra.dtos.PensumListadoDTO;
import com.codice.sra.services.PensumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.codice.sra.dtos.PensumMateriaDetalleDTO;
import com.codice.sra.dtos.PensumCrearDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.codice.sra.dtos.PensumBorradorDTO;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pensum")
@RequiredArgsConstructor
public class PensumController {

    private final PensumService pensumService;


    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PensumListadoDTO> crearPensum(@Valid @RequestBody PensumCrearDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pensumService.crearPensum(dto));
    }


    @PostMapping("/{idOrigen}/crear-version")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PensumListadoDTO> crearVersionDesdeExistente(@PathVariable Long idOrigen, @Valid @RequestBody PensumCrearDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pensumService.crearVersionDesdeExistente(idOrigen, dto));
    }


    @PutMapping("/{idPensum}/borrador")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PensumDetalleDTO> guardarBorrador(
            @PathVariable Long idPensum,
            @Valid @RequestBody PensumBorradorDTO dto) {

        return ResponseEntity.ok(pensumService.guardarBorrador(idPensum, dto));
    }

    @PatchMapping("/{idPensum}/enviar-aprobacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PensumListadoDTO> enviarAprobacion(@PathVariable Long idPensum) {
        return ResponseEntity.ok(pensumService.enviarAprobacion(idPensum));
    }


    @PatchMapping("/{idPensum}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PensumListadoDTO> activarPensum(@PathVariable Long idPensum) {
        return ResponseEntity.ok(pensumService.activarPensum(idPensum));
    }


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

    @GetMapping("/catalogos/materias")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<PensumCatalogoMateriaDTO>> listarMateriasActivas() {
        return ResponseEntity.ok(pensumService.listarMateriasActivas());
    }

    @GetMapping("/catalogos/materias/{idMateria}/prerrequisitos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<PensumMateriaDetalleDTO.PrerrequisitoDTO>> obtenerPrerrequisitos(@PathVariable Long idMateria) {
        return ResponseEntity.ok(pensumService.obtenerPrerrequisitos(idMateria));
    }

    @DeleteMapping("/{idPensum}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarPensum(@PathVariable Long idPensum) {
        pensumService.eliminarPensum(idPensum);
        return ResponseEntity.noContent().build();
    }
}
