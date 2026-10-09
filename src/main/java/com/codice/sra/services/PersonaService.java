package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.exceptions.DuplicateResourceException;
import com.codice.sra.exceptions.ResourceNotFoundException;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.utils.ValidadorDocumentoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class PersonaService {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_COMPLETADO = "COMPLETADO";

    private final PersonaRepository personaRepository;
    private final EstadoRegistroPersonaRepository estadoRegistroRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final DistritoRepository distritoRepository;
    private final EmpleadoRepository empleadoRepository;

    public PersonaService(PersonaRepository personaRepository,
                          EstadoRegistroPersonaRepository estadoRegistroRepository,
                          TipoDocumentoRepository tipoDocumentoRepository,
                          UsuarioRepository usuarioRepository,
                          DocenteRepository docenteRepository,
                          DistritoRepository distritoRepository,
                          EmpleadoRepository empleadoRepository) {
        this.personaRepository = personaRepository;
        this.estadoRegistroRepository = estadoRegistroRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.distritoRepository = distritoRepository;
        this.empleadoRepository = empleadoRepository;
    }


    @Transactional(readOnly = true)
    public Optional<PersonaConsultaResponseDTO> buscarPorDocumento(String numeroDocumento) {
        return personaRepository.findByNumeroDocumentoConUbicacion(numeroDocumento)
                .map(persona -> {
                    // 1. Roles de usuario asignados
                    List<String> roles = usuarioRepository.findRolesByPersonaId(persona.getIdPersona());

                    // 2. Perfiles laborales concurrentes (un solo viaje de red cada uno)
                    Optional<Docente> docenteOpt = docenteRepository.findByPersonaIdConRelaciones(persona.getIdPersona());
                    Optional<Empleado> empleadoOpt = empleadoRepository.findByPersonaIdConRelaciones(persona.getIdPersona());

                    // 3. Resolución polimórfica de Sede (Docente -> Empleado)
                    Sede sedeFinal = docenteOpt
                            .map(Docente::getSede)
                            .or(() -> empleadoOpt.map(Empleado::getSede))
                            .orElse(null);

                    // 4. Resolución polimórfica de Vigencia Laboral (Docente -> Empleado)
                    LocalDate fechaInicioFinal = docenteOpt
                            .map(Docente::getFechaInicio)
                            .or(() -> empleadoOpt.map(Empleado::getFechaIngreso))
                            .orElse(null);

                    LocalDate fechaFinFinal = docenteOpt
                            .map(Docente::getFechaFin)
                            .or(() -> empleadoOpt.map(Empleado::getFechaFin))
                            .orElse(null);

                    // 5. Ubicación Geográfica (Segura contra nulos gracias al fetch temprano)
                    Distrito dist = persona.getDistrito();
                    Departamento dep = (dist != null) ? dist.getDepartamento() : null;

                    return PersonaConsultaResponseDTO.builder()
                            .idPersona(persona.getIdPersona())
                            .idTipoDocumento(persona.getTipoDocumento() != null
                                    ? persona.getTipoDocumento().getIdTipoDocumento()
                                    : null)
                            .numeroDocumento(persona.getNumeroDocumento())
                            .nombres(persona.getNombres())
                            .apellidos(persona.getApellidos())
                            .fechaNacimiento(persona.getFechaNacimiento())
                            .sexo(persona.getSexo())
                            .telefono(persona.getTelefono())
                            .correoPersonal(persona.getCorreoPersonal())
                            .direccion(persona.getDireccion())
                            .rolesActivos(roles)
                            // Ubicación geográfica de El Salvador
                            .idDepartamento(dep != null ? dep.getIdDepartamento() : null)
                            .nombreDepartamento(dep != null ? dep.getNombre() : null)
                            .idDistrito(dist != null ? dist.getIdDistrito() : null)
                            .nombreDistrito(dist != null ? dist.getNombre() : null)
                            // Campus / Sede física unificada
                            .idSede(sedeFinal != null ? sedeFinal.getIdSede() : null)
                            .nombreSede(sedeFinal != null ? sedeFinal.getNombreSede() : null)
                            // Atributos de Contratación (Exclusivos de Docente)
                            .idTipoContratacion(docenteOpt.map(d -> d.getTipoContratacion() != null
                                    ? d.getTipoContratacion().getIdTipoContratacion() : null).orElse(null))
                            .tipoContratacion(docenteOpt.map(d -> d.getTipoContratacion() != null
                                    ? d.getTipoContratacion().getTipoContratacion() : null).orElse(null))
                            .idEspecialidad(null)
                            .especialidad(docenteOpt.map(Docente::getEspecialidad).orElse(null))
                            // Vigencia laboral compartida
                            .fechaInicioContrato(fechaInicioFinal)
                            .fechaFinContrato(fechaFinFinal)
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
        // Validación directa antes de cualquier operación
        String documentoLimpio = ValidadorDocumentoUtil.normalizar(input.getNumeroDocumento());
        ValidadorDocumentoUtil.validar(input.getIdTipoDocumento(), documentoLimpio);
        return personaRepository.findByNumeroDocumentoForUpdate(documentoLimpio)
                .orElseGet(() -> {
                    if (input.getCorreoPersonal() != null && !input.getCorreoPersonal().isBlank()
                            && personaRepository.existsByCorreoPersonal(input.getCorreoPersonal())) {
                        throw new IllegalArgumentException("Ya existe una persona registrada con el correo personal: " + input.getCorreoPersonal());
                    }

                    TipoDocumento tipoDoc = tipoDocumentoRepository.findById(input.getIdTipoDocumento())
                            .orElseThrow(() -> new IllegalArgumentException("Tipo de documento no válido con ID: " + input.getIdTipoDocumento()));

                    EstadoRegistroPersona estadoPendiente = estadoRegistroRepository.findByEstadoRegistro(ESTADO_PENDIENTE)
                            .orElseThrow(() -> new IllegalStateException("El estado '" + ESTADO_PENDIENTE + "' no está configurado en la base de datos"));

                    Distrito distrito = distritoRepository.findById(input.getIdDistrito())
                            .orElseThrow(() -> new IllegalArgumentException("Distrito no válido con ID: " + input.getIdDistrito()));

                    Persona nuevaPersona = new Persona();
                    nuevaPersona.setTipoDocumento(tipoDoc);
                    nuevaPersona.setNumeroDocumento(documentoLimpio);
                    nuevaPersona.setNombres(input.getNombres());
                    nuevaPersona.setApellidos(input.getApellidos());
                    nuevaPersona.setFechaNacimiento(input.getFechaNacimiento());
                    nuevaPersona.setTelefono(input.getTelefono());
                    nuevaPersona.setCorreoPersonal(input.getCorreoPersonal());
                    nuevaPersona.setDireccion(input.getDireccion());
                    nuevaPersona.setEstadoRegistro(estadoPendiente);
                    nuevaPersona.setFechaRegistro(OffsetDateTime.now());
                    nuevaPersona.setDistrito(distrito);
                    nuevaPersona.setSexo(input.getSexo().trim().toUpperCase());

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

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean actualizarDatosContacto(Long idPersona, PersonaContactoEdicionDTO dto) {
        Persona persona = personaRepository.findById(idPersona)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con ID: " + idPersona));

        String telefonoLimpio = dto.getTelefono().trim().replaceAll("[\\s-]", "");
        String telefonoNormalizado = telefonoLimpio.substring(0, 4) + "-" + telefonoLimpio.substring(4);
        String correoNormalizado = dto.getCorreoPersonal().trim().toLowerCase();
        String direccionNormalizada = dto.getDireccion().trim();

        boolean telefonoIgual = Objects.equals(persona.getTelefono(), telefonoNormalizado);
        boolean correoIgual = Objects.equals(persona.getCorreoPersonal(), correoNormalizado);
        boolean direccionIgual = Objects.equals(persona.getDireccion(), direccionNormalizada);
        boolean distritoIgual = persona.getDistrito() != null &&
                Objects.equals(persona.getDistrito().getIdDistrito(), dto.getIdDistrito());

        if (telefonoIgual && correoIgual && direccionIgual && distritoIgual) {
            return false; // Indicamos que no hubo mutación
        }

        if (!correoIgual && personaRepository.existsByCorreoPersonalAndIdPersonaNot(correoNormalizado, idPersona)) {
            throw new IllegalArgumentException("El correo personal '" + correoNormalizado + "' ya está registrado por otra persona en el sistema");
        }

        if (!distritoIgual) {
            Distrito nuevoDistrito = distritoRepository.findById(dto.getIdDistrito())
                    .orElseThrow(() -> new IllegalArgumentException("Distrito no válido con ID: " + dto.getIdDistrito()));
            persona.setDistrito(nuevoDistrito);
        }

        persona.setTelefono(telefonoNormalizado);
        persona.setCorreoPersonal(correoNormalizado);
        persona.setDireccion(direccionNormalizada);

        return true; // Hubo cambio
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