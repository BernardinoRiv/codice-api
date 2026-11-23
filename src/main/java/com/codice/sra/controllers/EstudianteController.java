package com.codice.sra.controllers;

import com.codice.sra.dtos.*;
import com.codice.sra.models.Estudiante;
import com.codice.sra.repositories.EstudianteRepository;
import com.codice.sra.services.EstudianteService;
import com.codice.sra.services.PdfAcademicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar todos los estudiantes registrados")
    public ResponseEntity<List<EstudianteListaResponseDTO>> obtenerTodosLosEstudiantes() {
        return ResponseEntity.ok(estudianteService.obtenerTodosLosEstudiantes());
    }

    @PutMapping("/{idEstudiante}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar los datos de contacto del estudiante")
    public ResponseEntity<Void> actualizarEstudiante(
            @PathVariable Long idEstudiante,
            @Valid @RequestBody PersonaContactoEdicionDTO request) {
        estudianteService.actualizarEstudiante(idEstudiante, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEstudiante}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cambiar el estado académico del estudiante con justificación")
    public ResponseEntity<Void> cambiarEstadoEstudiante(
            @PathVariable Long idEstudiante,
            @Valid @RequestBody EstudianteEstadoRequestDTO request) {
        estudianteService.cambiarEstadoEstudiante(idEstudiante, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idEstudiante}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Consultar información administrativa del estudiante")
    public ResponseEntity<EstudianteListaResponseDTO> obtenerEstudiantePorId(@PathVariable Long idEstudiante) {
        return ResponseEntity.ok(estudianteService.obtenerEstudiantePorId(idEstudiante));
    }


    @PatchMapping("/{idEstudiante}/usuario/bloquear")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Bloquear administrativamente la cuenta de un estudiante")
    public ResponseEntity<Void> bloquearUsuario(
            @PathVariable Long idEstudiante,
            @Valid @RequestBody EstudianteBloqueoUsuarioDTO request) {

        estudianteService.bloquearUsuario(idEstudiante, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEstudiante}/usuario/desbloquear")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desbloquear la cuenta institucional de un estudiante")
    public ResponseEntity<Void> desbloquearUsuario(
            @PathVariable Long idEstudiante,
            @Valid @RequestBody EstudianteBloqueoUsuarioDTO request) {

        estudianteService.desbloquearUsuario(idEstudiante, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{idEstudiante}/datos-personales")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar la información personal de un estudiante")
    public ResponseEntity<Void> actualizarDatosPersonales(
            @PathVariable Long idEstudiante,
            @Valid @RequestBody EstudianteDatosPersonalesDTO request) {

        estudianteService.actualizarDatosPersonales(idEstudiante, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEstudiante}/documento")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cambiar el documento de identidad de un estudiante con auditoría")
    public ResponseEntity<Void> cambiarDocumento(
            @PathVariable Long idEstudiante,
            @Valid @RequestBody EstudianteCambioDocumentoDTO request) {

        estudianteService.cambiarDocumento(idEstudiante, request);
        return ResponseEntity.noContent().build();
    }

}
