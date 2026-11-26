
package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.exceptions.ResourceNotFoundException;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.utils.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class EmpleadoService {

    public static final String ROL_EMPLEADO = "EMPLEADO";
    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String PREFIJO_CODIGO = "EMP";
    private static final int LONGITUD_PASSWORD = 12;

    private final EmpleadoRepository empleadoRepository;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final EstadoEmpleadoRepository estadoEmpleadoRepository;
    private final PersonaService personaService;
    private final UsuarioService usuarioService;
    private final SedeRepository sedeRepository;
    private final AuditoriaService auditoriaService;

    public EmpleadoService(EmpleadoRepository empleadoRepository, AreaRepository areaRepository,
                           CargoRepository cargoRepository, EstadoEmpleadoRepository estadoEmpleadoRepository,
                           PersonaService personaService, UsuarioService usuarioService,
                           SedeRepository sedeRepository, AuditoriaService auditoriaService) {
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.estadoEmpleadoRepository = estadoEmpleadoRepository;
        this.personaService = personaService;
        this.usuarioService = usuarioService;
        this.sedeRepository = sedeRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public EmpleadoRegistroResponseDTO registrarEmpleado(EmpleadoRegistroRequestDTO request) {
        Persona persona = personaService.obtenerOCrearPersona(request);

        Sede sede = sedeRepository.findById(request.getIdSede())
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna sede registrada con el ID: " + request.getIdSede()));

        Usuario usuario = usuarioService.aprovisionarUsuario(persona, ROL_EMPLEADO, LONGITUD_PASSWORD);

        Area area = areaRepository.findById(request.getIdArea())
                .orElseThrow(() -> new IllegalArgumentException("Área no encontrada con ID: " + request.getIdArea()));

        Cargo cargo = cargoRepository.findById(request.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado con ID: " + request.getIdCargo()));

        EstadoEmpleado estadoEmpleado = estadoEmpleadoRepository.findByEstadoEmpleado(ESTADO_ACTIVO)
                .orElseThrow(() -> new IllegalStateException("Estado '" + ESTADO_ACTIVO + "' para Empleado no encontrado en la base de datos"));

        String codigoEmpleado = UserUtils.generarCodigoUnico(PREFIJO_CODIGO);
        LocalDate fechaIngresoActual = LocalDate.now();
        LocalDate fechaFinContrato = request.getFechaFin();

        if (fechaFinContrato != null && fechaFinContrato.isBefore(fechaIngresoActual)) {
            throw new IllegalArgumentException("La fecha de fin de contrato (" + fechaFinContrato +
                    ") no puede ser anterior a la fecha de ingreso (" + fechaIngresoActual + ").");
        }

        Empleado empleado = new Empleado();
        empleado.setPersona(persona);
        empleado.setUsuario(usuario);
        empleado.setSede(sede);
        empleado.setArea(area);
        empleado.setCargo(cargo);
        empleado.setEstadoEmpleado(estadoEmpleado);
        empleado.setCodigoEmpleado(codigoEmpleado);
        empleado.setFechaIngreso(fechaIngresoActual);
        empleado.setFechaFin(fechaFinContrato);

        empleado = empleadoRepository.save(empleado);
        personaService.marcarComoCompletada(persona);

        return new EmpleadoRegistroResponseDTO(
                empleado.getIdEmpleado(), codigoEmpleado, persona.getNombres(), persona.getApellidos(),
                usuario.getCorreoInstitucional(), "Empleado registrado exitosamente. Credenciales enviadas."
        );
    }

    @Transactional(readOnly = true)
    public List<EmpleadoListaResponseDTO> obtenerTodosLosEmpleados() {
        return empleadoRepository.findAllBy().stream().map(this::convertirAListaDTO).toList();
    }

    @Transactional(readOnly = true)
    public EmpleadoListaResponseDTO obtenerEmpleadoPorId(Long idEmpleado) {
        if (idEmpleado == null || idEmpleado <= 0) {
            throw new IllegalArgumentException("El ID del empleado debe ser positivo");
        }

        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + idEmpleado));

        return convertirAListaDTO(empleado);
    }

    private EmpleadoListaResponseDTO convertirAListaDTO(Empleado empleado) {
        Persona persona = empleado.getPersona();
        Usuario usuario = empleado.getUsuario();

        return new EmpleadoListaResponseDTO(
                empleado.getIdEmpleado(),
                usuario != null ? usuario.getIdUsuario() : null,
                empleado.getCodigoEmpleado(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getNumeroDocumento(),
                persona.getTipoDocumento() != null ? persona.getTipoDocumento().getIdTipoDocumento() : null,
                persona.getCorreoPersonal(),
                usuario != null ? usuario.getCorreoInstitucional() : null,
                persona.getTelefono(),
                persona.getDireccion(),
                persona.getDistrito() != null ? persona.getDistrito().getIdDistrito() : null,
                empleado.getEstadoEmpleado().getEstadoEmpleado(),
                usuario != null && usuario.getEstadoUsuario() != null ? usuario.getEstadoUsuario().getEstadoUsuario() : "SIN USUARIO",
                empleado.getFechaIngreso(),
                empleado.getFechaFin(),
                persona.getFechaNacimiento(),
                persona.getSexo(),
                persona.getDistrito() != null ? persona.getDistrito().getDepartamento().getIdDepartamento() : null,
                persona.getDistrito() != null ? persona.getDistrito().getDepartamento().getNombre() : null,
                persona.getDistrito() != null ? persona.getDistrito().getNombre() : null,
                empleado.getArea() != null ? empleado.getArea().getIdArea() : null,
                empleado.getArea() != null ? empleado.getArea().getArea() : null,
                empleado.getCargo() != null ? empleado.getCargo().getIdCargo() : null,
                empleado.getCargo() != null ? empleado.getCargo().getCargo() : null,
                empleado.getSede() != null ? empleado.getSede().getIdSede() : null,
                empleado.getSede() != null ? empleado.getSede().getNombreSede() : null
        );
    }

    @Transactional
    public void actualizarEmpleado(Long idEmpleado, PersonaContactoEdicionDTO request) {
        Empleado empleado = buscarEmpleadoParaActualizar(idEmpleado);

        if (personaService.actualizarDatosContacto(empleado.getPersona().getIdPersona(), request)) {
            auditoriaService.registrarEvento("ACTUALIZACION_EMPLEADO", "empleados", idEmpleado,
                    "Se actualizaron los datos de contacto del empleado.");
        }
    }

    @Transactional
    public void actualizarDatosPersonales(Long idEmpleado, EmpleadoDatosPersonalesDTO request) {
        Empleado empleado = buscarEmpleadoParaActualizar(idEmpleado);
        personaService.actualizarDatosPersonalesEmpleado(empleado.getPersona().getIdPersona(), request);
    }

    @Transactional
    public void cambiarDocumento(Long idEmpleado, EmpleadoCambioDocumentoDTO request) {
        Empleado empleado = buscarEmpleadoParaActualizar(idEmpleado);
        personaService.cambiarDocumentoEmpleado(empleado.getPersona().getIdPersona(), request);
    }

    @Transactional
    public void actualizarDatosLaborales(Long idEmpleado, EmpleadoDatosLaboralesDTO request) {
        Empleado empleado = buscarEmpleadoParaActualizar(idEmpleado);

        Area area = areaRepository.findById(request.getIdArea())
                .orElseThrow(() -> new IllegalArgumentException("Área no encontrada con ID: " + request.getIdArea()));

        Cargo cargo = cargoRepository.findById(request.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado con ID: " + request.getIdCargo()));

        Sede sede = sedeRepository.findById(request.getIdSede())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + request.getIdSede()));

        if (request.getFechaFin() != null && request.getFechaFin().isBefore(empleado.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha de finalización no puede ser anterior a la fecha de ingreso.");
        }

        List<String> cambios = new ArrayList<>();

        if (!Objects.equals(empleado.getArea().getIdArea(), area.getIdArea())) {
            cambios.add("area: " + empleado.getArea().getArea() + " -> " + area.getArea());
            empleado.setArea(area);
        }

        if (!Objects.equals(empleado.getCargo().getIdCargo(), cargo.getIdCargo())) {
            cambios.add("cargo: " + empleado.getCargo().getCargo() + " -> " + cargo.getCargo());
            empleado.setCargo(cargo);
        }

        if (!Objects.equals(empleado.getSede().getIdSede(), sede.getIdSede())) {
            cambios.add("sede: " + empleado.getSede().getNombreSede() + " -> " + sede.getNombreSede());
            empleado.setSede(sede);
        }

        if (!Objects.equals(empleado.getFechaFin(), request.getFechaFin())) {
            cambios.add("fecha_fin: " + empleado.getFechaFin() + " -> " + request.getFechaFin());
            empleado.setFechaFin(request.getFechaFin());
        }

        if (cambios.isEmpty()) return;

        empleadoRepository.save(empleado);
        auditoriaService.registrarEvento("ACTUALIZACION_DATOS_LABORALES_EMPLEADO", "empleados", idEmpleado,
                "Se modificaron los datos laborales: " + String.join("; ", cambios));
    }

    @Transactional
    public void cambiarEstadoEmpleado(Long idEmpleado, EmpleadoEstadoRequestDTO request) {
        if (request.getMotivo() == null || request.getMotivo().trim().length() < 5) {
            throw new IllegalArgumentException("Escribe un motivo de al menos 5 caracteres.");
        }
        Empleado empleado = buscarEmpleadoParaActualizar(idEmpleado);

        EstadoEmpleado nuevoEstado = estadoEmpleadoRepository.findById(request.getIdEstadoEmpleado())
                .filter(estado -> Boolean.TRUE.equals(estado.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El estado de empleado no existe o está dado de baja: " + request.getIdEstadoEmpleado()));

        EstadoEmpleado estadoAnterior = empleado.getEstadoEmpleado();

        if (estadoAnterior.getIdEstadoEmpleado().equals(nuevoEstado.getIdEstadoEmpleado())) {
            throw new IllegalArgumentException("El empleado ya posee el estado '" + nuevoEstado.getEstadoEmpleado() + "'");
        }

        empleado.setEstadoEmpleado(nuevoEstado);

        auditoriaService.registrarEvento("CAMBIO_ESTADO_EMPLEADO", "empleados", idEmpleado,
                "Cambio de '%s' a '%s'. Motivo: %s".formatted(
                        estadoAnterior.getEstadoEmpleado(), nuevoEstado.getEstadoEmpleado(), request.getMotivo().trim()));
    }

    @Transactional
    public void bloquearUsuario(Long idEmpleado, EmpleadoBloqueoUsuarioDTO request) {
        Empleado empleado = buscarEmpleadoParaActualizar(idEmpleado);

        if (empleado.getUsuario() == null) {
            throw new IllegalArgumentException("El empleado no tiene una cuenta institucional asignada.");
        }

        usuarioService.bloquearUsuarioEmpleado(empleado.getUsuario().getIdUsuario(), request.getMotivo());
    }

    @Transactional
    public void desbloquearUsuario(Long idEmpleado, EmpleadoBloqueoUsuarioDTO request) {
        Empleado empleado = buscarEmpleadoParaActualizar(idEmpleado);

        if (empleado.getUsuario() == null) {
            throw new IllegalArgumentException("El empleado no tiene una cuenta institucional asignada.");
        }

        usuarioService.desbloquearUsuarioEmpleado(empleado.getUsuario().getIdUsuario(), request.getMotivo());
    }

    private Empleado buscarEmpleadoParaActualizar(Long idEmpleado) {
        if (idEmpleado == null || idEmpleado <= 0) {
            throw new IllegalArgumentException("El ID del empleado debe ser positivo");
        }

        return empleadoRepository.findForUpdateByIdEmpleado(idEmpleado)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + idEmpleado));
    }
}