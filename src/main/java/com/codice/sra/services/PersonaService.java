package com.codice.sra.services;

import com.codice.sra.dtos.PersonaRegistroRequestDTO;
import com.codice.sra.dtos.PersonaResponseDTO;
import com.codice.sra.models.EstadoRegistroPersona;
import com.codice.sra.models.Persona;
import com.codice.sra.repositories.EstadoRegistroPersonaRepository;
import com.codice.sra.repositories.PersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final EstadoRegistroPersonaRepository estadoRegistroRepository;

    public PersonaService(PersonaRepository personaRepository,
                          EstadoRegistroPersonaRepository estadoRegistroRepository) {
        this.personaRepository = personaRepository;
        this.estadoRegistroRepository = estadoRegistroRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PersonaResponseDTO> buscarPorDocumento(String documento) {
        return personaRepository.findByDocumento(documento)
                .map(this::mapToResponseDTO);
    }

    @Transactional
    public PersonaResponseDTO registrarPersona(PersonaRegistroRequestDTO request) {
        // Validaciones de unicidad
        if (personaRepository.existsByDocumento(request.getDocumento())) {
            throw new IllegalArgumentException("Ya existe una persona registrada con este documento");
        }
        if (request.getCorreoPersonal() != null && !request.getCorreoPersonal().isEmpty()
                && personaRepository.existsByCorreoPersonal(request.getCorreoPersonal())) {
            throw new IllegalArgumentException("Ya existe una persona registrada con este correo personal");
        }

        // Obtener el estado "PENDIENTE" de la base de datos
        EstadoRegistroPersona estadoPendiente = estadoRegistroRepository.findByEstadoRegistro("PENDIENTE")
                .orElseThrow(() -> new IllegalStateException("El estado 'PENDIENTE' no está configurado en la base de datos"));

        // Mapeo de DTO a Entidad
        Persona persona = new Persona();
        persona.setDocumento(request.getDocumento());
        persona.setNombres(request.getNombres());
        persona.setApellidos(request.getApellidos());
        persona.setFechaNacimiento(request.getFechaNacimiento());
        persona.setTelefono(request.getTelefono());
        persona.setCorreoPersonal(request.getCorreoPersonal());
        persona.setDireccion(request.getDireccion());
        persona.setEstadoRegistro(estadoPendiente);

        persona = personaRepository.save(persona);

        return mapToResponseDTO(persona);
    }

    private PersonaResponseDTO mapToResponseDTO(Persona persona) {
        return new PersonaResponseDTO(
                persona.getIdPersona(),
                persona.getDocumento(),
                persona.getNombres(),
                persona.getApellidos(),
                "Persona registrada en estado PENDIENTE"
        );
    }
}