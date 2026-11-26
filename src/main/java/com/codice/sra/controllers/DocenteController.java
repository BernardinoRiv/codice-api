package com.codice.sra.controllers;

import com.codice.sra.security.PermisosEmpleado;
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

    @Autowired
    private PermisosEmpleado permisosEmpleado;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeRegistrarEnSede(authentication, #request.idSede, 'registro-docente')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puede(authentication, 'directorio-docentes')")
    @Operation(summary = "Obtener lista de todos los docentes registrados")
    public ResponseEntity<List<DocenteListaResponseDTO>> obtenerTodosLosDocentes(Authentication authentication) {
        List<DocenteListaResponseDTO> docentes = docenteService.obtenerTodosLosDocentes(permisosEmpleado.sedeConsulta(authentication, "directorio-docentes"));
        return ResponseEntity.ok(docentes);
    }

    private Long obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }

    @PutMapping("/{idDocente}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @permisosEmpleado.puedeEditarDocente(authentication, #idDocente, #request.idSede)")
    @Operation(summary = "Editar docente", description = "Actualiza los datos personales permitidos y la ficha contractual del docente.")
    public ResponseEntity<Void> actualizarDocente(
            @PathVariable Long idDocente,
            @Valid @RequestBody DocenteEdicionRequestDTO request) {
        docenteService.actualizarDocente(idDocente, request);
        return ResponseEntity.noContent().build(); // Retorna HTTP 204 sin cuerpo
    }

    @PatchMapping("/{idDocente}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @permisosEmpleado.puedeEditarDocente(authentication, #idDocente)")
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

    @GetMapping("/{idDocente}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puede(authentication, 'directorio-docentes')")
    public DocenteDetalleDTO detalle(@PathVariable Long idDocente, Authentication authentication) {
        return docenteService.detalle(idDocente, permisosEmpleado.sedeConsulta(authentication, "directorio-docentes"));
    }

    @PutMapping("/{idDocente}/datos-personales")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @permisosEmpleado.puedeEditarDocente(authentication, #idDocente)")
    public ResponseEntity<Void> datos_personales(@PathVariable Long idDocente, @Valid @RequestBody EmpleadoDatosPersonalesDTO dto) {
        docenteService.datosPersonales(idDocente, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idDocente}/documento")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @permisosEmpleado.puedeEditarDocente(authentication, #idDocente)")
    public ResponseEntity<Void> documento(@PathVariable Long idDocente, @Valid @RequestBody EmpleadoCambioDocumentoDTO dto) {
        docenteService.documento(idDocente, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idDocente}/datos-laborales")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @permisosEmpleado.puedeEditarDocente(authentication, #idDocente, #dto.idSede())")
    public ResponseEntity<Void> datos_laborales(@PathVariable Long idDocente, @Valid @RequestBody DocenteDatosLaboralesDTO dto) {
        docenteService.datosLaborales(idDocente, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idDocente}/usuario/bloquear")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @permisosEmpleado.puedeEditarDocente(authentication, #idDocente)")
    public ResponseEntity<Void> bloquear(@PathVariable Long idDocente, @Valid @RequestBody EmpleadoBloqueoUsuarioDTO dto) {
        docenteService.estadoCuenta(idDocente, true, dto.getMotivo());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idDocente}/usuario/desbloquear")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @permisosEmpleado.puedeEditarDocente(authentication, #idDocente)")
    public ResponseEntity<Void> desbloquear(@PathVariable Long idDocente, @Valid @RequestBody EmpleadoBloqueoUsuarioDTO dto) {
        docenteService.estadoCuenta(idDocente, false, dto.getMotivo());
        return ResponseEntity.noContent().build();
    }
}