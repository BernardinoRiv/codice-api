package com.codice.sra.controllers;

import com.codice.sra.dtos.ClaseInscritaDTO;
import com.codice.sra.dtos.PensumEstudianteDTO;
import com.codice.sra.dtos.SolicitudRetiroDTO;
import com.codice.sra.models.Estudiante;
import com.codice.sra.repositories.EstudianteRepository;
import com.codice.sra.security.JwtService;
import com.codice.sra.services.EstudianteClasesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/estudiantes")
public class EstudianteClasesController {

    private final EstudianteClasesService service;
    private final JwtService jwtService;
    private final EstudianteRepository estudianteRepository;

    public EstudianteClasesController(EstudianteClasesService service,
                                      JwtService jwtService,
                                      EstudianteRepository estudianteRepository) {
        this.service = service;
        this.jwtService = jwtService;
        this.estudianteRepository = estudianteRepository;
    }

    // Método auxiliar que extrae el carnet directamente del token JWT y busca al estudiante
    private Long obtenerIdEstudianteDesdeToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token de autorización no válido o ausente.");
        }
        String token = authHeader.substring(7);

        // Extraemos el carnet que ya viene guardado en el token
        String carnet = jwtService.extractCarnet(token);

        if (carnet == null || carnet.trim().isEmpty()) {
            // Como respaldo por si el token fuera antiguo, extraemos el ID de usuario y buscamos
            Long idUsuario = jwtService.extractIdUsuario(token);
            Estudiante estudiante = estudianteRepository.findByUsuario_IdUsuario(idUsuario)
                    .orElseThrow(() -> new RuntimeException("No se encontró un estudiante asociado al usuario actual."));
            return estudiante.getIdEstudiante();
        }

        Estudiante estudiante = estudianteRepository.findByCarnet(carnet)
                .orElseThrow(() -> new RuntimeException("No se encontró ningún estudiante con el carnet: " + carnet));

        return estudiante.getIdEstudiante();
    }

    @GetMapping("/mis-clases")
    public ResponseEntity<List<ClaseInscritaDTO>> obtenerMisClases(@RequestHeader("Authorization") String authHeader) {
        Long idEstudiante = obtenerIdEstudianteDesdeToken(authHeader);
        return ResponseEntity.ok(service.obtenerClasesCicloActivo(idEstudiante));
    }

    @GetMapping("/pensum")
    public ResponseEntity<PensumEstudianteDTO> obtenerPensum(@RequestHeader("Authorization") String authHeader) {
        Long idEstudiante = obtenerIdEstudianteDesdeToken(authHeader);
        return ResponseEntity.ok(service.obtenerPensum(idEstudiante));
    }

    // Endpoint para generar el cargo y cambiar la materia a RETIRO PENDIENTE
    @PostMapping("/mis-clases/solicitar-retiro")
    public ResponseEntity<?> solicitarRetiro(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @jakarta.validation.Valid SolicitudRetiroDTO solicitud) {
        try {
            Long idEstudiante = obtenerIdEstudianteDesdeToken(authHeader);
            Map<String, String> resultado = service.solicitarRetiroMateria(idEstudiante, solicitud.getIdGrupo());
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Endpoint para eliminar el cargo pendiente y devolver la materia a CURSANDO
    @PostMapping("/mis-clases/cancelar-retiro")
    public ResponseEntity<?> cancelarRetiro(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @jakarta.validation.Valid SolicitudRetiroDTO solicitud) {
        try {
            Long idEstudiante = obtenerIdEstudianteDesdeToken(authHeader);
            Map<String, String> resultado = service.cancelarRetiroMateria(idEstudiante, solicitud.getIdGrupo());
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}