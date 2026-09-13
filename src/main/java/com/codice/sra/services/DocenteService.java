package com.codice.sra.services;

import com.codice.sra.dtos.DocenteRegistroRequestDTO;
import com.codice.sra.dtos.DocenteRegistroResponseDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.DocenteRepository;
import com.codice.sra.repositories.EstadoDocenteRepository;
import com.codice.sra.repositories.SedeRepository;
import com.codice.sra.repositories.TipoContratacionDocenteRepository;
import com.codice.sra.utils.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocenteService {

    public static final String ROL_DOCENTE = "DOCENTE";
    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String PREFIJO_CODIGO = "DOC";
    private static final int LONGITUD_PASSWORD = 10;

    private final DocenteRepository docenteRepository;
    private final SedeRepository sedeRepository;
    private final TipoContratacionDocenteRepository tipoContratacionRepository;
    private final EstadoDocenteRepository estadoDocenteRepository;
    private final PersonaService personaService;
    private final UsuarioService usuarioService;

    public DocenteService(DocenteRepository docenteRepository,
                          SedeRepository sedeRepository,
                          TipoContratacionDocenteRepository tipoContratacionRepository,
                          EstadoDocenteRepository estadoDocenteRepository,
                          PersonaService personaService,
                          UsuarioService usuarioService) {
        this.docenteRepository = docenteRepository;
        this.sedeRepository = sedeRepository;
        this.tipoContratacionRepository = tipoContratacionRepository;
        this.estadoDocenteRepository = estadoDocenteRepository;
        this.personaService = personaService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public DocenteRegistroResponseDTO registrarDocente(DocenteRegistroRequestDTO request) {
        // 1. Obtener o crear persona mediante el contrato PersonaInputDTO
        Persona persona = personaService.obtenerOCrearPersona(request);

        // 2. Aprovisionar credenciales y cuenta de usuario delegando en UsuarioService
        Usuario usuario = usuarioService.aprovisionarUsuario(persona, ROL_DOCENTE, LONGITUD_PASSWORD);

        // 3. Resolver catálogos específicos del dominio Docente
        Sede sede = sedeRepository.findById(request.getIdSede())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + request.getIdSede()));

        TipoContratacionDocente tipoContratacion = tipoContratacionRepository.findById(request.getIdTipoContratacion())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de contratación no encontrado con ID: " + request.getIdTipoContratacion()));

        EstadoDocente estadoDocente = estadoDocenteRepository.findByEstadoDocente(ESTADO_ACTIVO)
                .orElseThrow(() -> new IllegalStateException("Estado '" + ESTADO_ACTIVO + "' para Docente no encontrado en la base de datos"));

        // 4. Construir y persistir la entidad Docente
        String codigoDocente = UserUtils.generarCodigoUnico(PREFIJO_CODIGO);

        Docente docente = new Docente();
        docente.setPersona(persona);
        docente.setUsuario(usuario);
        docente.setSede(sede);
        docente.setTipoContratacion(tipoContratacion);
        docente.setEstadoDocente(estadoDocente);
        docente.setCodigoDocente(codigoDocente);
        docente.setEspecialidad(request.getEspecialidad());
        docente = docenteRepository.save(docente);

        // 5. Finalizar el ciclo de registro de la persona física
        personaService.marcarComoCompletada(persona);

        return new DocenteRegistroResponseDTO(
                docente.getIdDocente(),
                codigoDocente,
                persona.getNombres(),
                persona.getApellidos(),
                usuario.getCorreoInstitucional(),
                "Docente registrado exitosamente. Credenciales enviadas."
        );
    }
}