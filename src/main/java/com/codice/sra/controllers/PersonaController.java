package com.codice.sra.controllers;

import com.codice.sra.dtos.PersonaConsultaResponseDTO;
import com.codice.sra.dtos.PersonaRegistroRequestDTO;
import com.codice.sra.dtos.PersonaResponseDTO;
import com.codice.sra.services.PersonaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/personas")
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @PostMapping
    public ResponseEntity<PersonaResponseDTO> registrar(@Valid @RequestBody PersonaRegistroRequestDTO request) {
        PersonaResponseDTO response = personaService.registrarPersona(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/buscar-por-documento")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PersonaConsultaResponseDTO> buscarPorDocumento(@RequestParam String numeroDocumento) {
        return personaService.buscarPorDocumento(numeroDocumento)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}