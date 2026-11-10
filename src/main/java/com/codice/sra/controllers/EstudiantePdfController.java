package com.codice.sra.controllers;

import com.codice.sra.services.EstudiantePdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estudiantes")
@RequiredArgsConstructor
public class EstudiantePdfController {

    private final EstudiantePdfService estudiantePdfService;

    @GetMapping("/generar-pdf")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<byte[]> generarPdfAcademico(@RequestParam("tipo") String tipo) {
        try {
            byte[] pdfBytes = estudiantePdfService.generarDocumentoAcademico(tipo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "Reporte_Academico.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            // Esto imprimirá en la consola de tu backend la línea exacta del error
            System.out.println("===== ERROR AL GENERAR PDF =====");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}