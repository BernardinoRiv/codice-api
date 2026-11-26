package com.codice.sra.controllers;

import com.codice.sra.security.PermisosEmpleado;
import com.codice.sra.dtos.*;
import com.codice.sra.services.EmpleadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/empleados")
@RequiredArgsConstructor
@Tag(name = "Gestión de Empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;
    private final PermisosEmpleado permisosEmpleado;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeRegistrarEnSede(authentication, #request.idSede, 'registro-empleados')")
    @Operation(summary = "Registrar nuevo empleado")
    public ResponseEntity<EmpleadoRegistroResponseDTO> crearEmpleado(@Valid @RequestBody EmpleadoRegistroRequestDTO request,
                                                                    Authentication authentication) {
        boolean altaDelegada = authentication.getAuthorities().stream()
                .anyMatch(autoridad -> "ROLE_EMPLEADO".equals(autoridad.getAuthority()));
        return ResponseEntity.ok(empleadoService.registrarEmpleado(request, altaDelegada));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puede(authentication, 'directorio-empleados')")
    @Operation(summary = "Consultar directorio de empleados")
    public ResponseEntity<List<EmpleadoListaResponseDTO>> obtenerTodosLosEmpleados(Authentication authentication) {
        return ResponseEntity.ok(empleadoService.obtenerTodosLosEmpleados(permisosEmpleado.sedeConsulta(authentication, "directorio-empleados")));
    }

    @GetMapping("/{idEmpleado}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puede(authentication, 'directorio-empleados')")
    @Operation(summary = "Consultar información de un empleado")
    public ResponseEntity<EmpleadoListaResponseDTO> obtenerEmpleadoPorId(@PathVariable Long idEmpleado, Authentication authentication) {
        return ResponseEntity.ok(empleadoService.obtenerEmpleadoPorId(idEmpleado, permisosEmpleado.sedeConsulta(authentication, "directorio-empleados")));
    }

    @PutMapping("/{idEmpleado}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeEditarEmpleado(authentication, #idEmpleado)")
    @Operation(summary = "Actualizar datos de contacto del empleado")
    public ResponseEntity<Void> actualizarEmpleado(@PathVariable Long idEmpleado, @Valid @RequestBody PersonaContactoEdicionDTO request) {
        empleadoService.actualizarEmpleado(idEmpleado, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{idEmpleado}/datos-personales")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeEditarEmpleado(authentication, #idEmpleado)")
    @Operation(summary = "Actualizar datos personales del empleado")
    public ResponseEntity<Void> actualizarDatosPersonales(@PathVariable Long idEmpleado, @Valid @RequestBody EmpleadoDatosPersonalesDTO request) {
        empleadoService.actualizarDatosPersonales(idEmpleado, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEmpleado}/documento")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeEditarEmpleado(authentication, #idEmpleado)")
    @Operation(summary = "Cambiar documento de identidad del empleado")
    public ResponseEntity<Void> cambiarDocumento(@PathVariable Long idEmpleado, @Valid @RequestBody EmpleadoCambioDocumentoDTO request) {
        empleadoService.cambiarDocumento(idEmpleado, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEmpleado}/datos-laborales")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeEditarEmpleado(authentication, #idEmpleado, #request.idSede, #request.idArea, #request.idCargo)")
    @Operation(summary = "Actualizar información laboral del empleado")
    public ResponseEntity<Void> actualizarDatosLaborales(@PathVariable Long idEmpleado, @Valid @RequestBody EmpleadoDatosLaboralesDTO request) {
        empleadoService.actualizarDatosLaborales(idEmpleado, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEmpleado}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeEditarEmpleado(authentication, #idEmpleado)")
    @Operation(summary = "Cambiar estado laboral del empleado")
    public ResponseEntity<Void> cambiarEstadoEmpleado(@PathVariable Long idEmpleado, @Valid @RequestBody EmpleadoEstadoRequestDTO request) {
        empleadoService.cambiarEstadoEmpleado(idEmpleado, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEmpleado}/usuario/bloquear")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeEditarEmpleado(authentication, #idEmpleado)")
    @Operation(summary = "Bloquear cuenta institucional del empleado")
    public ResponseEntity<Void> bloquearUsuario(@PathVariable Long idEmpleado, @Valid @RequestBody EmpleadoBloqueoUsuarioDTO request) {
        empleadoService.bloquearUsuario(idEmpleado, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEmpleado}/usuario/desbloquear")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR') or @permisosEmpleado.puedeEditarEmpleado(authentication, #idEmpleado)")
    @Operation(summary = "Desbloquear cuenta institucional del empleado")
    public ResponseEntity<Void> desbloquearUsuario(@PathVariable Long idEmpleado, @Valid @RequestBody EmpleadoBloqueoUsuarioDTO request) {
        empleadoService.desbloquearUsuario(idEmpleado, request);
        return ResponseEntity.noContent().build();
    }
}