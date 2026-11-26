package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.exceptions.ResourceNotFoundException;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.repositories.DocenteRepository;
import com.codice.sra.repositories.EstadoDocenteRepository;
import com.codice.sra.repositories.SedeRepository;
import com.codice.sra.repositories.TipoContratacionDocenteRepository;
import com.codice.sra.utils.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CicloRepository cicloRepository;
    private final AuditoriaService auditoriaService;

    public DocenteService(DocenteRepository docenteRepository,
                          SedeRepository sedeRepository,
                          TipoContratacionDocenteRepository tipoContratacionRepository,
                          EstadoDocenteRepository estadoDocenteRepository,
                          PersonaService personaService,
                          UsuarioService usuarioService,
                          GrupoRepository grupoRepository,
                          UsuarioRepository usuarioRepository,
                          CicloRepository cicloRepository,
                          AuditoriaService auditoriaService) {
        this.docenteRepository = docenteRepository;
        this.sedeRepository = sedeRepository;
        this.tipoContratacionRepository = tipoContratacionRepository;
        this.estadoDocenteRepository = estadoDocenteRepository;
        this.personaService = personaService;
        this.usuarioService = usuarioService;
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cicloRepository = cicloRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public DocenteRegistroResponseDTO registrarDocente(DocenteRegistroRequestDTO request) {
        Persona persona = personaService.obtenerOCrearPersona(request);
        Usuario usuario = usuarioService.aprovisionarUsuario(persona, ROL_DOCENTE, LONGITUD_PASSWORD);

        Sede sede = sedeRepository.findById(request.getIdSede())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + request.getIdSede()));

        TipoContratacionDocente tipoContratacion = tipoContratacionRepository.findById(request.getIdTipoContratacion())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de contratación no encontrado con ID: " + request.getIdTipoContratacion()));

        EstadoDocente estadoDocente = estadoDocenteRepository.findByEstadoDocente(ESTADO_ACTIVO)
                .orElseThrow(() -> new IllegalStateException("Estado '" + ESTADO_ACTIVO + "' para Docente no encontrado en la base de datos"));

        String codigoDocente = UserUtils.generarCodigoUnico(PREFIJO_CODIGO);
        LocalDate fechaIngresoActual = LocalDate.now();
        LocalDate fechaFinContrato = request.getFechaFin();

        if (fechaFinContrato != null && fechaFinContrato.isBefore(fechaIngresoActual)) {
            throw new IllegalArgumentException(
                    "La fecha de fin de contrato (" + fechaFinContrato +
                            ") no puede ser anterior a la fecha de ingreso (" + fechaIngresoActual + ")."
            );
        }

        Docente docente = new Docente();
        docente.setPersona(persona);
        docente.setUsuario(usuario);
        docente.setSede(sede);
        docente.setTipoContratacion(tipoContratacion);
        docente.setEstadoDocente(estadoDocente);
        docente.setCodigoDocente(codigoDocente);
        docente.setEspecialidad(request.getEspecialidad());
        docente.setFechaInicio(fechaIngresoActual);
        docente.setFechaFin(fechaFinContrato);
        docente = docenteRepository.save(docente);

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

    public List<Grupo> obtenerGruposPorDocente(Long idUsuario) {
        Docente docente = docenteRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Docente no encontrado para el usuario con ID: " + idUsuario));

        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("No hay ciclo activo en el sistema."));

        return grupoRepository.findByDocenteIdDocenteAndCicloIdCiclo(docente.getIdDocente(), cicloActivo.getIdCiclo());
    }

    public List<GrupoResponseDTO> obtenerGruposPorDocenteDTO(Long idUsuario) {
        List<Grupo> grupos = obtenerGruposPorDocente(idUsuario);

        return grupos.stream().map(grupo -> {
            return new GrupoResponseDTO(
                    grupo.getIdGrupo(),
                    grupo.getCodigoGrupo(),
                    grupo.getMateria().getNombreMateria(),
                    grupo.getMateria().getCodigoMateria(),
                    grupo.getCiclo().getCodigoCiclo(),
                    grupo.getSede().getNombreSede(),
                    grupo.getDocente().getPersona().getNombres(),
                    grupo.getDocente().getPersona().getApellidos()
            );
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocenteListaResponseDTO> obtenerTodosLosDocentes(Long idSede) {
        List<Docente> docentes = idSede == null ? docenteRepository.findAll()
                : docenteRepository.findAllBySede_IdSede(idSede);

        return docentes.stream().map(this::aLista).toList();
    }

    private DocenteListaResponseDTO aLista(Docente docente) {
        Persona persona = docente.getPersona();
        Long idDistrito = persona.getDistrito() != null ? persona.getDistrito().getIdDistrito() : null;

        Usuario usuario = docente.getUsuario();
        String estadoUsuario = usuario == null ? "SIN USUARIO" :
                usuario.getEstadoUsuario() == null ? "SIN ESTADO" : usuario.getEstadoUsuario().getEstadoUsuario();
        return new DocenteListaResponseDTO(
                docente.getIdDocente(),
                docente.getUsuario() == null ? null : docente.getUsuario().getIdUsuario(),
                docente.getCodigoDocente(),
                persona.getNombres(),
                persona.getApellidos(),
                docente.getUsuario() == null ? null : docente.getUsuario().getCorreoInstitucional(),
                docente.getEspecialidad(),
                docente.getSede().getNombreSede(),
                docente.getTipoContratacion().getTipoContratacion(),
                docente.getEstadoDocente().getEstadoDocente(),
                persona.getTelefono(),
                persona.getCorreoPersonal(),
                persona.getDireccion(),
                idDistrito,
                docente.getSede().getIdSede(),
                docente.getTipoContratacion().getIdTipoContratacion(),
                persona.getNumeroDocumento(),
                docente.getFechaInicio(),
                docente.getFechaFin(),
                estadoUsuario
        );
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void actualizarDocente(Long idDocente, DocenteEdicionRequestDTO dto) {
        if (idDocente == null || idDocente <= 0) {
            throw new IllegalArgumentException("El ID del docente debe ser positivo");
        }

        Docente docente = docenteRepository.findByIdConRelaciones(idDocente)
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado con ID: " + idDocente));

        boolean cambiosPersona = personaService.actualizarDatosContacto(docente.getPersona().getIdPersona(), dto);

        boolean sedeIgual = docente.getSede() != null &&
                Objects.equals(docente.getSede().getIdSede(), dto.getIdSede());

        boolean tipoContratoIgual = docente.getTipoContratacion() != null &&
                Objects.equals(docente.getTipoContratacion().getIdTipoContratacion(), dto.getIdTipoContratacion());

        boolean cambiosDocente = false;

        if (!sedeIgual) {
            Sede nuevaSede = sedeRepository.findById(dto.getIdSede())
                    .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + dto.getIdSede()));
            docente.setSede(nuevaSede);
            cambiosDocente = true;
        }

        if (!tipoContratoIgual) {
            TipoContratacionDocente nuevoTipo = tipoContratacionRepository.findById(dto.getIdTipoContratacion())
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de contratación no encontrado con ID: " + dto.getIdTipoContratacion()));
            docente.setTipoContratacion(nuevoTipo);
            cambiosDocente = true;
        }

        if (cambiosPersona || cambiosDocente) {
            auditoriaService.registrarEvento(
                    "ACTUALIZACION_DOCENTE",
                    "docentes",
                    docente.getIdDocente(),
                    "El administrador actualizó sede=" + dto.getIdSede() + ", tipoContrato=" + dto.getIdTipoContratacion()
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void cambiarEstadoDocente(Long idDocente, DocenteEstadoRequestDTO dto) {
        if (idDocente == null || idDocente <= 0) {
            throw new IllegalArgumentException("El ID del docente debe ser positivo");
        }

        Docente docente = docenteRepository.findById(idDocente)
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado con ID: " + idDocente));

        EstadoDocente nuevoEstado = estadoDocenteRepository.findById(dto.getIdEstadoDocente())
                .orElseThrow(() -> new IllegalArgumentException("Estado de docente no válido con ID: " + dto.getIdEstadoDocente()));

        if (docente.getEstadoDocente() != null &&
                Objects.equals(docente.getEstadoDocente().getIdEstadoDocente(), nuevoEstado.getIdEstadoDocente())) {
            throw new IllegalArgumentException(
                    "El docente con ID " + idDocente + " ya posee el estado '" + nuevoEstado.getEstadoDocente() + "'"
            );
        }

        docente.setEstadoDocente(nuevoEstado);

        auditoriaService.registrarEvento(
                "CAMBIO_ESTADO_DOCENTE",
                "docentes",
                docente.getIdDocente(),
                String.format("Transición a estado '%s'. Motivo: %s", nuevoEstado.getEstadoDocente(), dto.getMotivo().trim())
        );
    }

    private Docente buscarDocente(Long id) {
        return docenteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public DocenteDetalleDTO detalle(Long id, Long idSede) {
        Docente d = idSede == null ? buscarDocente(id)
                : docenteRepository.findByIdDocenteAndSede_IdSede(id, idSede)
                        .orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado: " + id));
        Persona p = d.getPersona();
        DocenteDetalleDTO dto = new DocenteDetalleDTO();
        org.springframework.beans.BeanUtils.copyProperties(aLista(d), dto);
        dto.setSexo(p.getSexo());
        dto.setFechaNacimiento(p.getFechaNacimiento());
        dto.setIdTipoDocumento(p.getTipoDocumento().getIdTipoDocumento());
        dto.setIdEstadoDocente(d.getEstadoDocente().getIdEstadoDocente());
        if (p.getDistrito() != null) {
            dto.setNombreDistrito(p.getDistrito().getNombre());
            dto.setNombreDepartamento(p.getDistrito().getDepartamento().getNombre());
            dto.setIdDepartamento(p.getDistrito().getDepartamento().getIdDepartamento());
        }
        return dto;
    }

    @Transactional
    public void datosPersonales(Long id, EmpleadoDatosPersonalesDTO dto) {
        personaService.actualizarDatosPersonalesDocente(buscarDocente(id).getPersona().getIdPersona(), dto);
    }

    @Transactional
    public void documento(Long id, EmpleadoCambioDocumentoDTO dto) {
        personaService.cambiarDocumentoDocente(buscarDocente(id).getPersona().getIdPersona(), dto);
    }

    @Transactional
    public void estadoCuenta(Long id, boolean bloquear, String motivo) {
        Usuario usuario = buscarDocente(id).getUsuario();
        if (usuario == null) throw new IllegalStateException("Docente sin cuenta institucional.");
        usuarioService.cambiarEstadoCuentaDocente(usuario.getIdUsuario(), bloquear, motivo);
    }

    @Transactional
    public void datosLaborales(Long id, DocenteDatosLaboralesDTO dto) {
        Docente d = buscarDocente(id);
        if (dto.fechaFin() != null && dto.fechaFin().isBefore(dto.fechaInicio()))
            throw new IllegalArgumentException("La fecha de finalización debe ser igual o posterior al inicio.");
        d.setEspecialidad(dto.especialidad() == null ? null : dto.especialidad().trim());
        d.setSede(sedeRepository.findById(dto.idSede()).orElseThrow(() -> new IllegalArgumentException("Sede no encontrada.")));
        d.setTipoContratacion(tipoContratacionRepository.findById(dto.idTipoContratacion()).orElseThrow(() -> new IllegalArgumentException("Contratación no encontrada.")));
        d.setFechaInicio(dto.fechaInicio());
        d.setFechaFin(dto.fechaFin());
        auditoriaService.registrarEvento("ACTUALIZACION_DOCENTE", "docentes", id, "Actualización de datos laborales.");
    }
}