package com.codice.sra.controllers;

import com.codice.sra.dtos.SesionUsuarioDTO;
import com.codice.sra.services.SesionUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sesiones")
@Tag(name = "Gestion De Sesiones", description = "Endpoints para el monitoreo y control de sesiones de usuario")
public class SesionUsuarioController {

    private final SesionUsuarioService sesionUsuarioService;

    public SesionUsuarioController(SesionUsuarioService sesionUsuarioService) {
        this.sesionUsuarioService = sesionUsuarioService;
    }

    @GetMapping("/historial")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener historial de sesiones del usuario autenticado")
    public ResponseEntity<List<SesionUsuarioDTO>> obtenerHistorial() {
        Long idUsuario = obtenerIdUsuarioAutenticado();
        List<SesionUsuarioDTO> historial = sesionUsuarioService.obtenerHistorialSesiones(idUsuario);
        return ResponseEntity.ok(historial);
    }

    @PostMapping("/{idSesion}/cerrar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cerrar una sesion especifica por su identificador")
    public ResponseEntity<Void> cerrarSesion(@PathVariable Long idSesion) {
        sesionUsuarioService.cerrarSesion(idSesion);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cerrar-todas")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cerrar todas las sesiones activas del usuario autenticado")
    public ResponseEntity<Void> cerrarTodasLasSesiones() {
        Long idUsuario = obtenerIdUsuarioAutenticado();
        sesionUsuarioService.cerrarTodasLasSesiones(idUsuario);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/registrar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar nueva sesion de usuario")
    public ResponseEntity<SesionUsuarioDTO> registrarSesion(
            @RequestParam String direccionIp,
            @RequestParam String agenteUsuario) {

        Long idUsuario = obtenerIdUsuarioAutenticado();
        SesionUsuarioDTO sesion = sesionUsuarioService.registrarSesion(
                idUsuario,
                direccionIp,
                agenteUsuario,
                true
        );
        return ResponseEntity.ok(sesion);
    }

    private Long obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}