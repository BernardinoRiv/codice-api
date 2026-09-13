package com.codice.sra.services;

import com.codice.sra.dtos.EmpleadoRegistroRequestDTO;
import com.codice.sra.dtos.EmpleadoRegistroResponseDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.AreaRepository;
import com.codice.sra.repositories.CargoRepository;
import com.codice.sra.repositories.EmpleadoRepository;
import com.codice.sra.repositories.EstadoEmpleadoRepository;
import com.codice.sra.utils.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           AreaRepository areaRepository,
                           CargoRepository cargoRepository,
                           EstadoEmpleadoRepository estadoEmpleadoRepository,
                           PersonaService personaService,
                           UsuarioService usuarioService) {
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.estadoEmpleadoRepository = estadoEmpleadoRepository;
        this.personaService = personaService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public EmpleadoRegistroResponseDTO registrarEmpleado(EmpleadoRegistroRequestDTO request) {
        // 1. Obtener o crear persona mediante el contrato PersonaInputDTO
        Persona persona = personaService.obtenerOCrearPersona(request);

        // 2. Aprovisionar credenciales y cuenta de usuario delegando en UsuarioService
        Usuario usuario = usuarioService.aprovisionarUsuario(persona, ROL_EMPLEADO, LONGITUD_PASSWORD);

        // 3. Resolver catálogos específicos del dominio Empleado
        Area area = areaRepository.findById(request.getIdArea())
                .orElseThrow(() -> new IllegalArgumentException("Área no encontrada con ID: " + request.getIdArea()));

        Cargo cargo = cargoRepository.findById(request.getIdCargo())
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado con ID: " + request.getIdCargo()));

        EstadoEmpleado estadoEmpleado = estadoEmpleadoRepository.findByEstadoEmpleado(ESTADO_ACTIVO)
                .orElseThrow(() -> new IllegalStateException("Estado '" + ESTADO_ACTIVO + "' para Empleado no encontrado en la base de datos"));

        // 4. Construir y persistir la entidad Empleado
        String codigoEmpleado = UserUtils.generarCodigoUnico(PREFIJO_CODIGO);

        Empleado empleado = new Empleado();
        empleado.setPersona(persona);
        empleado.setUsuario(usuario);
        empleado.setArea(area);
        empleado.setCargo(cargo);
        empleado.setEstadoEmpleado(estadoEmpleado);
        empleado.setCodigoEmpleado(codigoEmpleado);
        empleado.setFechaIngreso(LocalDate.now());
        empleado = empleadoRepository.save(empleado);

        // 5. Finalizar el ciclo de registro de la persona física
        personaService.marcarComoCompletada(persona);

        return new EmpleadoRegistroResponseDTO(
                empleado.getIdEmpleado(),
                codigoEmpleado,
                persona.getNombres(),
                persona.getApellidos(),
                usuario.getCorreoInstitucional(),
                "Empleado registrado exitosamente. Credenciales enviadas."
        );
    }
}