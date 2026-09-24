package com.codice.sra.services;

import com.codice.sra.dtos.PersonaConsultaResponseDTO;
import com.codice.sra.dtos.PersonaInputDTO;
import com.codice.sra.dtos.PersonaRegistroRequestDTO;
import com.codice.sra.dtos.PersonaResponseDTO;
import com.codice.sra.models.Docente;
import com.codice.sra.models.EstadoRegistroPersona;
import com.codice.sra.models.Persona;
import com.codice.sra.models.TipoDocumento;
import com.codice.sra.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PersonaService {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_COMPLETADO = "COMPLETADO";

    private final PersonaRepository personaRepository;
    private final EstadoRegistroPersonaRepository estadoRegistroRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;

    public PersonaService(PersonaRepository personaRepository,
                          EstadoRegistroPersonaRepository estadoRegistroRepository,
                          TipoDocumentoRepository tipoDocumentoRepository,
                          UsuarioRepository usuarioRepository,
                          DocenteRepository docenteRepository) {
        this.personaRepository = personaRepository;
        this.estadoRegistroRepository = estadoRegistroRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
    }


    @Transactional(readOnly = true)
    public Optional<PersonaConsultaResponseDTO> buscarPorDocumento(String numeroDocumento) {
        return personaRepository.findByNumeroDocumento(numeroDocumento)
                .map(persona -> {
                    List<String> roles = usuarioRepository.findRolesByPersonaId(persona.getIdPersona());

                    // Trae el docente, la sede y el tipo de contratación en una sola sentencia SQL
                    Optional<Docente> docenteOpt = docenteRepository.findByPersonaIdConRelaciones(persona.getIdPersona());

                    return PersonaConsultaResponseDTO.builder()
                            .idPersona(persona.getIdPersona())
                            .idTipoDocumento(persona.getTipoDocumento() != null
                                    ? persona.getTipoDocumento().getIdTipoDocumento()
                                    : null)
                            .numeroDocumento(persona.getNumeroDocumento())
                            .nombres(persona.getNombres())
                            .apellidos(persona.getApellidos())
                            .fechaNacimiento(persona.getFechaNacimiento())
                            .telefono(persona.getTelefono())
                            .correoPersonal(persona.getCorreoPersonal())
                            .direccion(persona.getDireccion())
                            .rolesActivos(roles)
                            // Atributos de enlace para el formulario del frontend
                            .idSede(docenteOpt.map(d -> d.getSede() != null ? d.getSede().getIdSede() : null).orElse(null))
                            .nombreSede(docenteOpt.map(d -> d.getSede() != null ? d.getSede().getNombreSede() : null).orElse(null))
                            .idTipoContratacion(docenteOpt.map(d -> d.getTipoContratacion() != null ? d.getTipoContratacion().getIdTipoContratacion() : null).orElse(null))
                            .tipoContratacion(docenteOpt.map(d -> d.getTipoContratacion() != null ? d.getTipoContratacion().getTipoContratacion() : null).orElse(null))
                            .idEspecialidad(null)
                            .especialidad(docenteOpt.map(Docente::getEspecialidad).orElse(null))
                            .build();
                });
    }

    @Transactional
    public PersonaResponseDTO registrarPersona(PersonaRegistroRequestDTO request) {
        Persona persona = obtenerOCrearPersona(request);
        return mapToResponseDTO(persona);
    }

    /**
     * Busca una persona por su número de documento. Si no existe, valida el correo
     * y la registra en estado PENDIENTE.
     *
     * @param input Cualquier DTO que implemente PersonaInputDTO (Docente, Empleado, etc.)
     * @return La entidad Persona persistida o recuperada de la base de datos.
     */
    @Transactional
    public Persona obtenerOCrearPersona(PersonaInputDTO input) {
        return personaRepository.findByNumeroDocumentoForUpdate(input.getNumeroDocumento())
                .orElseGet(() -> {
                    if (input.getCorreoPersonal() != null && !input.getCorreoPersonal().isBlank()
                            && personaRepository.existsByCorreoPersonal(input.getCorreoPersonal())) {
                        throw new IllegalArgumentException("Ya existe una persona registrada con el correo personal: " + input.getCorreoPersonal());
                    }

                    TipoDocumento tipoDoc = tipoDocumentoRepository.findById(input.getIdTipoDocumento())
                            .orElseThrow(() -> new IllegalArgumentException("Tipo de documento no válido con ID: " + input.getIdTipoDocumento()));

                    EstadoRegistroPersona estadoPendiente = estadoRegistroRepository.findByEstadoRegistro(ESTADO_PENDIENTE)
                            .orElseThrow(() -> new IllegalStateException("El estado '" + ESTADO_PENDIENTE + "' no está configurado en la base de datos"));

                    Persona nuevaPersona = new Persona();
                    nuevaPersona.setTipoDocumento(tipoDoc);
                    nuevaPersona.setNumeroDocumento(input.getNumeroDocumento());
                    nuevaPersona.setNombres(input.getNombres());
                    nuevaPersona.setApellidos(input.getApellidos());
                    nuevaPersona.setFechaNacimiento(input.getFechaNacimiento());
                    nuevaPersona.setTelefono(input.getTelefono());
                    nuevaPersona.setCorreoPersonal(input.getCorreoPersonal());
                    nuevaPersona.setDireccion(input.getDireccion());
                    nuevaPersona.setEstadoRegistro(estadoPendiente);
                    nuevaPersona.setFechaRegistro(OffsetDateTime.now());

                    return personaRepository.save(nuevaPersona);
                });
    }

    @Transactional
    public void marcarComoCompletada(Persona persona) {
        EstadoRegistroPersona estadoCompletado = estadoRegistroRepository.findByEstadoRegistro(ESTADO_COMPLETADO)
                .orElseThrow(() -> new IllegalStateException("El estado '" + ESTADO_COMPLETADO + "' no está configurado en la base de datos"));
        persona.setEstadoRegistro(estadoCompletado);
        personaRepository.save(persona);
    }

    private PersonaResponseDTO mapToResponseDTO(Persona persona) {
        return new PersonaResponseDTO(
                persona.getIdPersona(),
                persona.getNumeroDocumento(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getEstadoRegistro().getEstadoRegistro(),
                "Persona procesada exitosamente"
        );
    }
}