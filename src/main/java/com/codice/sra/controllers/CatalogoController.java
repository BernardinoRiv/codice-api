package com.codice.sra.controllers;

import com.codice.sra.dtos.CatalogoDTO;
import com.codice.sra.enums.TipoCatalogo;
import com.codice.sra.services.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // GET /api/catalogos/roles
    // GET /api/catalogos/estados-usuario
    @GetMapping("/{nombre}")
    public ResponseEntity<List<CatalogoDTO>> getCatalogo(@PathVariable String nombre) {
        TipoCatalogo tipo = TipoCatalogo.fromSlug(nombre);
        return ResponseEntity.ok(catalogoService.obtenerPorTipo(tipo));
    }

    // GET /api/catalogos?nombres=roles,tipos-documento,estados-sede
    @GetMapping
    public ResponseEntity<Map<String, List<CatalogoDTO>>> getCatalogosMultiples(
            @RequestParam List<String> nombres) {
        return ResponseEntity.ok(catalogoService.obtenerMultiples(nombres));
    }
}