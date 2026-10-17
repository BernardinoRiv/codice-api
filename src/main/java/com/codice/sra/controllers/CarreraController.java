package com.codice.sra.controllers;

import com.codice.sra.dtos.CarreraActualizacionRequestDTO;
import com.codice.sra.dtos.CarreraCreacionRequestDTO;
import com.codice.sra.dtos.CarreraDesactivacionRequestDTO;
import com.codice.sra.dtos.CarreraResponseDTO;
import com.codice.sra.services.CarreraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/carreras")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CarreraController {

    private final CarreraService carreraService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CarreraResponseDTO>> listarCarreras(
            @RequestParam(name = "idSede", required = false) Long idSede) {
        return ResponseEntity.ok(carreraService.listarCarrerasOfertables(idSede));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    @Operation(
            summary = "Registrar nueva carrera",
            description = "Crea una carrera en el catálogo institucional. El código mnemónico es generado automáticamente por el sistema."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Carrera creada exitosamente. Se retorna la cabecera Location con el recurso creado."),
            @ApiResponse(responseCode = "400", description = "Nombre de carrera duplicado, o facultad/nivel inexistente o inactivo"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso restringido a Administradores"),
            @ApiResponse(responseCode = "422", description = "Error de validación sintáctica en el cuerpo de la petición")
    })
    public ResponseEntity<Void> crearCarrera(
            @Valid @RequestBody CarreraCreacionRequestDTO request) {

        Long idCarrera = carreraService.crearCarrera(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(idCarrera)
                .toUri();

        return ResponseEntity.created(location).build(); // HTTP 201 Created
    }

    @PutMapping("/{idCarrera}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    @Operation(
            summary = "Actualizar carrera existente (HU-ADM-05)",
            description = "Actualiza el nombre oficial, facultad o nivel académico de una carrera existente. El código institucional permanece inmutable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carrera actualizada exitosamente (o procesada de forma idempotente sin cambios)"),
            @ApiResponse(responseCode = "400", description = "Nombre duplicado en otra carrera, ID inválido o dependencias inactivas"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso restringido a Administradores"),
            @ApiResponse(responseCode = "404", description = "Carrera no encontrada"),
            @ApiResponse(responseCode = "422", description = "Error de validación en campos obligatorios")
    })
    public ResponseEntity<Void> actualizarCarrera(
            @Parameter(description = "Identificador único de la carrera a modificar", example = "4")
            @PathVariable Long idCarrera,
            @Valid @RequestBody CarreraActualizacionRequestDTO request) {

        carreraService.actualizarCarrera(idCarrera, request);
        return ResponseEntity.noContent().build(); // HTTP 204 No Content
    }

    @PatchMapping("/{idCarrera}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    @Operation(
            summary = "Desactivar carrera académica",
            description = "Efectúa la baja lógica de una carrera transicionándola al estado INACTIVA. Requiere una justificación formal auditable."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carrera desactivada exitosamente (o ya inactiva procesada de forma idempotente)"),
            @ApiResponse(responseCode = "400", description = "ID inválido o motivo de justificación insuficiente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso restringido a Administradores"),
            @ApiResponse(responseCode = "404", description = "Carrera no encontrada"),
            @ApiResponse(responseCode = "422", description = "Error de validación sintáctica en el cuerpo de la petición")
    })
    public ResponseEntity<Void> desactivarCarrera(
            @Parameter(description = "Identificador único de la carrera a desactivar", example = "4")
            @PathVariable Long idCarrera,
            @Valid @RequestBody CarreraDesactivacionRequestDTO request) {

        carreraService.desactivarCarrera(idCarrera, request);
        return ResponseEntity.noContent().build(); // HTTP 204 No Content
    }
}