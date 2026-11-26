package com.codice.sra.controllers;

import com.codice.sra.dtos.OfertaNuevoIngresoDTO;
import com.codice.sra.models.Pensum;
import com.codice.sra.repositories.CarreraSedeRepository;
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
    private final CarreraSedeRepository carreraSedeRepository;

    @GetMapping("/carreras-por-sede")
    public ResponseEntity<List<Map<String, Object>>> obtenerCarrerasPorSede(@RequestParam Long idSede) {
        var carreras = carreraSedeRepository.listarParaNuevoIngreso(idSede).stream().map(asignacion -> {
            var carrera = asignacion.getCarrera();
            Map<String, Object> opcion = new HashMap<>();
            opcion.put("idCarreraSede", asignacion.getIdCarreraSede());
            opcion.put("nombreCarrera", carrera.getNombreCarrera());
            opcion.put("nombreNivel", carrera.getNivel() == null ? null : carrera.getNivel().getNivel());
            return opcion;
        }).toList();
        return ResponseEntity.ok(carreras);
    }

    @GetMapping("/nuevo-ingreso")
    public ResponseEntity<List<OfertaNuevoIngresoDTO>> obtenerOfertaNuevoIngreso(
            @RequestParam Long idPensum,
            @RequestParam Long idSede) {
        return ResponseEntity.ok(ofertaNuevoIngresoService.obtenerOfertaPrimerCiclo(idPensum, idSede));
    }

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