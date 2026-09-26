package com.codice.sra.controllers;

import com.codice.sra.dtos.CatalogoDTO;
import com.codice.sra.enums.TipoCatalogo;
import com.codice.sra.services.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalogos")
@Validated
@Tag(name = "Catálogos Maestros", description = "Endpoints optimizados para poblar selectores simples y en cascada")
public class CatalogoController {

    private final CatalogoService catalogoService;

    // Inyección de dependencias por constructor (Inmutabilidad y testeabilidad)
    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @Operation(
            summary = "Obtener catálogo individual",
            description = "Retorna la lista de elementos para un catálogo específico. Permite el parámetro opcional 'padreId' para catálogos dependientes (ej. distritos por departamento)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Catálogo recuperado exitosamente.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogoDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Slug de catálogo no registrado o parámetros inválidos.",
                    content = @Content
            )
    })
    @GetMapping("/{nombre}")
    public ResponseEntity<List<CatalogoDTO>> getCatalogo(
            @Parameter(description = "Identificador del catálogo (kebab-case)", example = "distritos", required = true)
            @PathVariable @NotBlank(message = "El nombre del catálogo es obligatorio") String nombre,

            @Parameter(description = "ID del registro padre para catálogos jerárquicos (ej: id_departamento)", example = "2")
            @RequestParam(required = false) Long padreId) {

        TipoCatalogo tipo = TipoCatalogo.fromSlug(nombre);
        return ResponseEntity.ok(catalogoService.obtenerPorTipoYPadre(tipo, padreId));
    }

    @Operation(
            summary = "Obtener múltiples catálogos en lote",
            description = "Permite solicitar varios catálogos en una sola petición HTTP para optimizar el renderizado inicial en el frontend."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mapa de catálogos recuperado exitosamente."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Uno o más nombres de catálogo no son válidos.",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<Map<String, List<CatalogoDTO>>> getCatalogosMultiples(
            @Parameter(
                    description = "Lista de identificadores separados por comas",
                    example = "departamentos,tipos-documento,roles",
                    required = true
            )
            @RequestParam @NotEmpty(message = "Debe proporcionar al menos un nombre de catálogo") List<String> nombres) {

        return ResponseEntity.ok(catalogoService.obtenerMultiples(nombres));
    }
}