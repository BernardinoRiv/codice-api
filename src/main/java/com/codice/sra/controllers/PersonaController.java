package com.codice.sra.controllers;

import com.codice.sra.dtos.PersonaRegistroRequestDTO;
import com.codice.sra.dtos.PersonaResponseDTO;
import com.codice.sra.services.PersonaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/buscar")
    public ResponseEntity<PersonaResponseDTO> buscarPorDocumento(@RequestParam String documento) {
        return personaService.buscarPorDocumento(documento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}