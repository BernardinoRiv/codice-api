package com.codice.sra.controllers;

import com.codice.sra.dtos.OfertaNuevoIngresoDTO;
import com.codice.sra.models.Pensum;
import com.codice.sra.repositories.PensumRepository;
import com.codice.sra.services.OfertaNuevoIngresoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/oferta-academica")
@RequiredArgsConstructor
public class OfertaAcademicaController {

    private final OfertaNuevoIngresoService ofertaNuevoIngresoService;
    private final PensumRepository pensumRepository;

    @GetMapping("/nuevo-ingreso")
    public ResponseEntity<List<OfertaNuevoIngresoDTO>> obtenerOfertaNuevoIngreso(
            @RequestParam Long idPensum,
            @RequestParam Long idSede) {
        return ResponseEntity.ok(ofertaNuevoIngresoService.obtenerOfertaPrimerCiclo(idPensum, idSede));
    }

    // Nuevo endpoint para traer solo el pensum activo de la carrera
    @GetMapping("/pensum-vigente")
    public ResponseEntity<Map<String, Object>> obtenerPensumVigente(@RequestParam Integer idCarreraSede) {
        Pensum pensum = pensumRepository.findPensumVigenteByCarreraSede(idCarreraSede)
                .orElseThrow(() -> new RuntimeException("No hay pensum activo para esta carrera."));

        Map<String, Object> response = new HashMap<>();
        response.put("idPensum", pensum.getIdPensum());
        response.put("version", pensum.getVersion());
        return ResponseEntity.ok(response);
    }
}