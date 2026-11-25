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
import java.time.Period;
import java.util.*;

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
    private final AuditoriaService auditoriaService;

    public PersonaService(PersonaRepository personaRepository,
                          EstadoRegistroPersonaRepository estadoRegistroRepository,
                          TipoDocumentoRepository tipoDocumentoRepository,
                          UsuarioRepository usuarioRepository,
                          DocenteRepository docenteRepository,
                          DistritoRepository distritoRepository,
                          EmpleadoRepository empleadoRepository,
                          AuditoriaService auditoriaService) {
        this.personaRepository = personaRepository;
        this.estadoRegistroRepository = estadoRegistroRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.distritoRepository = distritoRepository;
        this.empleadoRepository = empleadoRepository;
        this.auditoriaService = auditoriaService;
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


    @Transactional
    public boolean actualizarDatosPersonalesEstudiante(Long idPersona, EstudianteDatosPersonalesDTO dto) {
        Persona persona = personaRepository.findByIdForUpdate(idPersona)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada: " + idPersona));

        String nombres = normalizarNombre(dto.getNombres());
        String apellidos = normalizarNombre(dto.getApellidos());
        String sexo = dto.getSexo().trim().toUpperCase(Locale.ROOT);
        String telefono = normalizarTelefonoEstudiante(dto.getTelefono());
        String correo = dto.getCorreoPersonal() == null || dto.getCorreoPersonal().isBlank()
                ? null : dto.getCorreoPersonal().trim().toLowerCase(Locale.ROOT);
        String direccion = dto.getDireccion().trim().replaceAll("\\s+", " ");

        if (nombres.isBlank() || apellidos.isBlank() || direccion.isBlank()) {
            throw new IllegalArgumentException("Nombres, apellidos y dirección son obligatorios.");
        }

        if (nombres.length() > 100 || apellidos.length() > 100) {
            throw new IllegalArgumentException("Los nombres y apellidos no pueden superar 100 caracteres.");
        }

        if (dto.getFechaNacimiento() == null || !dto.getFechaNacimiento().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento debe ser anterior a la fecha actual.");
        }

        if (!Objects.equals(persona.getFechaNacimiento(), dto.getFechaNacimiento())) {
            validarEdadPorDocumento(dto.getFechaNacimiento(), persona.getTipoDocumento().getIdTipoDocumento());
        }

        if (!sexo.equals("M") && !sexo.equals("F")) {
            throw new IllegalArgumentException("El sexo debe ser M o F.");
        }

        if (correo != null && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo personal no tiene un formato válido.");
        }

        if (correo != null && correo.length() > 150) {
            throw new IllegalArgumentException("El correo personal supera la longitud permitida.");
        }

        if (!Objects.equals(persona.getCorreoPersonal(), correo)
                && correo != null
                && personaRepository.existsByCorreoPersonalAndIdPersonaNot(correo, idPersona)) {
            throw new DuplicateResourceException("El correo personal ya está registrado por otra persona.");
        }

        Distrito distrito = distritoRepository.findById(dto.getIdDistrito())
                .orElseThrow(() -> new IllegalArgumentException("Distrito no encontrado: " + dto.getIdDistrito()));

        List<String> cambios = new ArrayList<>();

        if (!Objects.equals(persona.getNombres(), nombres)) {
            cambios.add("nombres");
            persona.setNombres(nombres);
        }

        if (!Objects.equals(persona.getApellidos(), apellidos)) {
            cambios.add("apellidos");
            persona.setApellidos(apellidos);
        }

        if (!Objects.equals(persona.getFechaNacimiento(), dto.getFechaNacimiento())) {
            cambios.add("fecha_nacimiento");
            persona.setFechaNacimiento(dto.getFechaNacimiento());
        }

        if (!Objects.equals(persona.getSexo(), sexo)) {
            cambios.add("sexo");
            persona.setSexo(sexo);
        }

        if (!Objects.equals(persona.getTelefono(), telefono)) {
            cambios.add("telefono");
            persona.setTelefono(telefono);
        }

        if (!Objects.equals(persona.getCorreoPersonal(), correo)) {
            cambios.add("correo_personal");
            persona.setCorreoPersonal(correo);
        }

        if (!Objects.equals(persona.getDireccion(), direccion)) {
            cambios.add("direccion");
            persona.setDireccion(direccion);
        }

        if (persona.getDistrito() == null
                || !Objects.equals(persona.getDistrito().getIdDistrito(), distrito.getIdDistrito())) {
            cambios.add("id_distrito");
            persona.setDistrito(distrito);
        }

        if (cambios.isEmpty()) {
            return false;
        }

        personaRepository.save(persona);

        auditoriaService.registrarEvento(
                "EDICION_DATOS_PERSONALES_ESTUDIANTE",
                "personas",
                idPersona,
                "Se modificaron los campos: " + String.join(", ", cambios)
        );

        return true;
    }


    @Transactional
    public void cambiarDocumentoEstudiante(Long idPersona, EstudianteCambioDocumentoDTO dto) {
        Persona persona = personaRepository.findByIdForUpdate(idPersona)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada: " + idPersona));

        String documentoActual = ValidadorDocumentoUtil.normalizar(dto.getDocumentoActual());
        String documentoNuevo = ValidadorDocumentoUtil.normalizar(dto.getNumeroDocumento());
        String motivo = dto.getMotivo() == null ? "" : dto.getMotivo().trim();

        if (motivo.length() < 10 || motivo.length() > 500) {
            throw new IllegalArgumentException("Debe proporcionar una justificación de 10 a 500 caracteres.");
        }

        if (!Objects.equals(persona.getNumeroDocumento(), documentoActual)) {
            throw new IllegalArgumentException("El documento actual no coincide con el registrado.");
        }

        ValidadorDocumentoUtil.validar(dto.getIdTipoDocumento(), documentoNuevo);
        validarEdadPorDocumento(persona.getFechaNacimiento(), dto.getIdTipoDocumento());

        TipoDocumento nuevoTipo = tipoDocumentoRepository.findById(dto.getIdTipoDocumento())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de documento no encontrado."));

        Long tipoAnterior = persona.getTipoDocumento().getIdTipoDocumento();
        String numeroAnterior = persona.getNumeroDocumento();

        if (Objects.equals(tipoAnterior, dto.getIdTipoDocumento())
                && Objects.equals(numeroAnterior, documentoNuevo)) {
            throw new IllegalArgumentException("El documento nuevo es igual al actual.");
        }

        if (!Objects.equals(numeroAnterior, documentoNuevo)
                && personaRepository.existsByNumeroDocumento(documentoNuevo)) {
            throw new DuplicateResourceException("El documento ya está registrado por otra persona.");
        }

        persona.setTipoDocumento(nuevoTipo);
        persona.setNumeroDocumento(documentoNuevo);

        personaRepository.saveAndFlush(persona);

        auditoriaService.registrarEvento(
                "CAMBIO_DOCUMENTO_ESTUDIANTE",
                "personas",
                idPersona,
                "Cambio de documento. Tipo anterior: " + tipoAnterior
                        + ", tipo nuevo: " + dto.getIdTipoDocumento()
                        + ". Documento anterior: " + enmascararDocumento(numeroAnterior)
                        + ", documento nuevo: " + enmascararDocumento(documentoNuevo)
                        + ". Motivo: " + motivo
        );
    }

    private String enmascararDocumento(String documento) {
        if (documento == null || documento.isBlank()) return "SIN DOCUMENTO";

        String valor = documento.replaceAll("[^A-Za-z0-9]", "");
        if (valor.length() <= 4) return "****";

        return "*".repeat(valor.length() - 4) + valor.substring(valor.length() - 4);
    }


    private String normalizarNombre(String valor) {
        if (valor == null) return "";
        return valor.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String normalizarTelefonoEstudiante(String telefono) {
        if (telefono == null || telefono.isBlank()) return null;

        String digitos = telefono.trim().replaceAll("[\\s-]", "");

        if (!digitos.matches("^[67][0-9]{7}$")) {
            throw new IllegalArgumentException("El teléfono debe tener 8 dígitos e iniciar con 6 o 7.");
        }

        return digitos.substring(0, 4) + "-" + digitos.substring(4);
    }


    private void validarEdadPorDocumento(LocalDate fechaNacimiento, Long idTipoDocumento) {
        if (fechaNacimiento == null || idTipoDocumento == null) {
            throw new IllegalArgumentException(
                    "La fecha de nacimiento y el tipo de documento son obligatorios."
            );
        }

        LocalDate hoy = LocalDate.now();

        if (fechaNacimiento.isAfter(hoy)) {
            throw new IllegalArgumentException(
                    "La fecha de nacimiento no puede ser posterior a la fecha actual."
            );
        }

        int edad = Period.between(fechaNacimiento, hoy).getYears();

        if (idTipoDocumento.equals(1L) && edad < 18) {
            throw new IllegalArgumentException(
                    "Para utilizar DUI, el estudiante debe tener al menos 18 años."
            );
        }

        if (idTipoDocumento.equals(2L) && edad < 15) {
            throw new IllegalArgumentException(
                    "Para utilizar pasaporte, el estudiante debe tener al menos 15 años."
            );
        }

        if (idTipoDocumento.equals(3L) && edad >= 18) {
            throw new IllegalArgumentException(
                    "El NIT solamente se permite a estudiantes menores de 18 años."
            );
        }
    }


    @Transactional
    public boolean actualizarDatosPersonalesEmpleado(Long idPersona, EmpleadoDatosPersonalesDTO dto) {
        return actualizarDatosPersonales(idPersona, dto, "EDICION_DATOS_PERSONALES_EMPLEADO");
    }

    @Transactional
    public boolean actualizarDatosPersonalesDocente(Long idPersona, EmpleadoDatosPersonalesDTO dto) {
        return actualizarDatosPersonales(idPersona, dto, "EDICION_DATOS_PERSONALES_DOCENTE");
    }

    private boolean actualizarDatosPersonales(Long idPersona, EmpleadoDatosPersonalesDTO dto, String eventoAuditoria) {
        Persona persona = personaRepository.findByIdForUpdate(idPersona)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada: " + idPersona));

        if (dto == null) throw new IllegalArgumentException("Los datos personales son obligatorios.");

        if ("EDICION_DATOS_PERSONALES_DOCENTE".equals(eventoAuditoria))
            validarEdadPorDocumento(dto.getFechaNacimiento(), persona.getTipoDocumento().getIdTipoDocumento());

        String nombres = normalizarNombre(dto.getNombres());
        String apellidos = normalizarNombre(dto.getApellidos());
        String sexo = dto.getSexo() == null ? "" : dto.getSexo().trim().toUpperCase(Locale.ROOT);
        String telefono = normalizarTelefonoEstudiante(dto.getTelefono());
        String correo = dto.getCorreoPersonal() == null || dto.getCorreoPersonal().isBlank()
                ? null : dto.getCorreoPersonal().trim().toLowerCase(Locale.ROOT);
        String direccion = dto.getDireccion() == null ? "" : dto.getDireccion().trim().replaceAll("\\s+", " ");

        if (nombres.isBlank() || apellidos.isBlank() || direccion.isBlank()) {
            throw new IllegalArgumentException("Nombres, apellidos y dirección son obligatorios.");
        }

        if (nombres.length() > 100 || apellidos.length() > 100) {
            throw new IllegalArgumentException("Los nombres y apellidos no pueden superar 100 caracteres.");
        }

        if (dto.getFechaNacimiento() == null || !dto.getFechaNacimiento().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento debe ser anterior a la fecha actual.");
        }

        if (!sexo.equals("M") && !sexo.equals("F")) {
            throw new IllegalArgumentException("El sexo debe ser M o F.");
        }

        if (correo != null && (!correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || correo.length() > 150)) {
            throw new IllegalArgumentException("El correo personal no tiene un formato válido.");
        }

        if (correo != null && !Objects.equals(persona.getCorreoPersonal(), correo)
                && personaRepository.existsByCorreoPersonalAndIdPersonaNot(correo, idPersona)) {
            throw new DuplicateResourceException("El correo personal ya está registrado por otra persona.");
        }

        Distrito distrito = distritoRepository.findById(dto.getIdDistrito())
                .orElseThrow(() -> new IllegalArgumentException("Distrito no encontrado: " + dto.getIdDistrito()));

        List<String> cambios = new ArrayList<>();

        if (!Objects.equals(persona.getNombres(), nombres)) {
            cambios.add("nombres");
            persona.setNombres(nombres);
        }

        if (!Objects.equals(persona.getApellidos(), apellidos)) {
            cambios.add("apellidos");
            persona.setApellidos(apellidos);
        }

        if (!Objects.equals(persona.getFechaNacimiento(), dto.getFechaNacimiento())) {
            cambios.add("fecha_nacimiento");
            persona.setFechaNacimiento(dto.getFechaNacimiento());
        }

        if (!Objects.equals(persona.getSexo(), sexo)) {
            cambios.add("sexo");
            persona.setSexo(sexo);
        }

        if (!Objects.equals(persona.getTelefono(), telefono)) {
            cambios.add("telefono");
            persona.setTelefono(telefono);
        }

        if (!Objects.equals(persona.getCorreoPersonal(), correo)) {
            cambios.add("correo_personal");
            persona.setCorreoPersonal(correo);
        }

        if (!Objects.equals(persona.getDireccion(), direccion)) {
            cambios.add("direccion");
            persona.setDireccion(direccion);
        }

        if (persona.getDistrito() == null || !Objects.equals(persona.getDistrito().getIdDistrito(), distrito.getIdDistrito())) {
            cambios.add("id_distrito");
            persona.setDistrito(distrito);
        }

        if (cambios.isEmpty()) return false;

        personaRepository.save(persona);
        auditoriaService.registrarEvento(eventoAuditoria, "personas", idPersona,
                "Se modificaron los campos: " + String.join(", ", cambios));

        return true;
    }


    @Transactional
    public void cambiarDocumentoEmpleado(Long idPersona, EmpleadoCambioDocumentoDTO dto) {
        cambiarDocumento(idPersona, dto, "CAMBIO_DOCUMENTO_EMPLEADO");
    }

    @Transactional
    public void cambiarDocumentoDocente(Long idPersona, EmpleadoCambioDocumentoDTO dto) {
        cambiarDocumento(idPersona, dto, "CAMBIO_DOCUMENTO_DOCENTE");
    }

    private void cambiarDocumento(Long idPersona, EmpleadoCambioDocumentoDTO dto, String eventoAuditoria) {
        Persona persona = personaRepository.findByIdForUpdate(idPersona)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada: " + idPersona));

        if (dto == null) throw new IllegalArgumentException("Los datos del documento son obligatorios.");

        String documentoActual = ValidadorDocumentoUtil.normalizar(dto.getDocumentoActual());
        String documentoNuevo = ValidadorDocumentoUtil.normalizar(dto.getNumeroDocumento());
        String motivo = dto.getMotivo() == null ? "" : dto.getMotivo().trim();

        if (motivo.length() < 10 || motivo.length() > 500) {
            throw new IllegalArgumentException("Debe proporcionar una justificación de 10 a 500 caracteres.");
        }

        if (!Objects.equals(persona.getNumeroDocumento(), documentoActual)) {
            throw new IllegalArgumentException("El documento actual no coincide con el registrado.");
        }

        if ("CAMBIO_DOCUMENTO_DOCENTE".equals(eventoAuditoria))
            validarEdadPorDocumento(persona.getFechaNacimiento(), dto.getIdTipoDocumento());

        ValidadorDocumentoUtil.validar(dto.getIdTipoDocumento(), documentoNuevo);

        TipoDocumento nuevoTipo = tipoDocumentoRepository.findById(dto.getIdTipoDocumento())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de documento no encontrado."));

        Long tipoAnterior = persona.getTipoDocumento().getIdTipoDocumento();
        String numeroAnterior = persona.getNumeroDocumento();

        if (Objects.equals(tipoAnterior, dto.getIdTipoDocumento()) && Objects.equals(numeroAnterior, documentoNuevo)) {
            throw new IllegalArgumentException("El documento nuevo es igual al actual.");
        }

        if (!Objects.equals(numeroAnterior, documentoNuevo) && personaRepository.existsByNumeroDocumento(documentoNuevo)) {
            throw new DuplicateResourceException("El documento ya está registrado por otra persona.");
        }

        persona.setTipoDocumento(nuevoTipo);
        persona.setNumeroDocumento(documentoNuevo);
        personaRepository.saveAndFlush(persona);

        auditoriaService.registrarEvento(eventoAuditoria, "personas", idPersona,
                "Cambio de documento. Tipo anterior: " + tipoAnterior
                        + ", tipo nuevo: " + dto.getIdTipoDocumento()
                        + ". Documento anterior: " + enmascararDocumento(numeroAnterior)
                        + ", documento nuevo: " + enmascararDocumento(documentoNuevo)
                        + ". Motivo: " + motivo);
    }




}