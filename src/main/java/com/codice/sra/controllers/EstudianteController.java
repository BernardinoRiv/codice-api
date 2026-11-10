package com.codice.sra.controllers;

import com.codice.sra.dtos.HistorialAcademicoDTO;
import com.codice.sra.dtos.NotasEstudianteResponseDTO;
import com.codice.sra.models.Estudiante;
import com.codice.sra.repositories.EstudianteRepository;
import com.codice.sra.services.EstudianteService;
import com.codice.sra.services.PdfAcademicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estudiantes")
@Tag(name = "Estudiantes - Notas")
public class EstudianteController {

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private PdfAcademicoService pdfAcademicoService;

    @GetMapping("/notas")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'ADMIN')")
    @Operation(summary = "Obtener notas del ciclo actual")
    public ResponseEntity<NotasEstudianteResponseDTO> obtenerNotas() {
        Long idEstudianteReal = obtenerIdEstudianteAutenticado();
        return ResponseEntity.ok(estudianteService.obtenerNotasCicloActual(idEstudianteReal));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<HistorialAcademicoDTO> obtenerHistorial() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long idUsuario = (Long) authentication.getPrincipal();

        Long idEstudiante = estudianteRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"))
                .getIdEstudiante();

        HistorialAcademicoDTO historial = estudianteService.obtenerHistorialAcademico(idEstudiante);
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/notas/pdf")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Descargar boleta de notas en PDF")
    public ResponseEntity<byte[]> descargarNotasPdf() {
        Long idEstudianteReal = obtenerIdEstudianteAutenticado();
        Estudiante estudiante = estudianteRepository.findById(idEstudianteReal)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        NotasEstudianteResponseDTO notas = estudianteService.obtenerNotasCicloActual(idEstudianteReal);
        HistorialAcademicoDTO historial = estudianteService.obtenerHistorialAcademico(idEstudianteReal);

        String nombreCompleto = estudiante.getPersona().getNombres() + " " + estudiante.getPersona().getApellidos();
        byte[] pdfBytes = pdfAcademicoService.generarBoletaNotasPDF(notas, historial, estudiante.getCarnet(), nombreCompleto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Boleta_Notas_" + estudiante.getCarnet() + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/historial/pdf")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Operation(summary = "Descargar historial académico en PDF filtrado por ciclo")
    public ResponseEntity<byte[]> descargarHistorialPdf(@RequestParam(defaultValue = "TODOS") String ciclo) {
        Long idEstudianteReal = obtenerIdEstudianteAutenticado();
        Estudiante estudiante = estudianteRepository.findById(idEstudianteReal)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        HistorialAcademicoDTO historial = estudianteService.obtenerHistorialAcademico(idEstudianteReal);

        String nombreCompleto = estudiante.getPersona().getNombres() + " " + estudiante.getPersona().getApellidos();
        byte[] pdfBytes = pdfAcademicoService.generarHistorialAcademicoPDF(historial, estudiante.getCarnet(), nombreCompleto, ciclo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Historial_" + estudiante.getCarnet() + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private Long obtenerIdEstudianteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long idUsuario = (Long) authentication.getPrincipal();

        Estudiante estudiante = estudianteRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new RuntimeException("No se encontró el perfil de estudiante para este usuario."));

        return estudiante.getIdEstudiante();
    }


}