package com.codice.sra.services;

import com.codice.sra.dtos.ConfiguracionFechasEvaluacionDTO;
import com.codice.sra.dtos.ResolverSolicitudesCalificacionDTO;
import com.codice.sra.dtos.SolicitudModificacionNotaDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GestionEvaluacionesService {
    private static final List<String> TIPOS = List.of("LABORATORIO", "PARCIAL");
    private final CicloRepository cicloRepository;
    private final GrupoRepository grupoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final PeriodoEvaluacionRepository periodoRepository;
    private final TipoEvaluacionRepository tipoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CalificacionRepository calificacionRepository;
    private final EstadoCalificacionRepository estadoCalificacionRepository;
    private final SolicitudCalificacionRepository solicitudRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarCiclos() {
        return cicloRepository.findAll().stream()
                .filter(c -> c.getEstadoCiclo().getIdEstadoCiclo() != 3L)
                .sorted(Comparator.comparing(Ciclo::getAnio).thenComparing(Ciclo::getNumeroCiclo))
                .map(c -> Map.<String, Object>of("idCiclo", c.getIdCiclo(), "codigoCiclo", c.getCodigoCiclo(),
                        "anio", c.getAnio(), "numeroCiclo", c.getNumeroCiclo(),
                        "estadoCiclo", c.getEstadoCiclo().getEstadoCiclo()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerFechas(Long idCiclo) {
        Ciclo ciclo = cicloRepository.findByIdConEstado(idCiclo).orElseThrow(() -> new IllegalArgumentException("Ciclo no encontrado."));
        List<Grupo> grupos = grupoRepository.findByCiclo_IdCiclo(idCiclo);
        Map<String, Object> slots = new LinkedHashMap<>();
        for (int periodo = 1; periodo <= 3; periodo++) for (String tipo : TIPOS) {
            int numero = periodo;
            Evaluacion existente = grupos.stream().flatMap(g -> evaluacionRepository.findByGrupoIdGrupo(g.getIdGrupo()).stream())
                    .filter(e -> e.getNumeroEvaluacion() == numero && e.getTipoEvaluacion().getTipoEvaluacion().equalsIgnoreCase(tipo))
                    .findFirst().orElse(null);
            Map<String, Object> rango = new LinkedHashMap<>();
            rango.put("fechaInicio", existente == null ? null : existente.getFechaInicio());
            rango.put("fechaFin", existente == null ? null : existente.getFechaFin());
            slots.put("P" + periodo + "_" + tipo, rango);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("idCiclo", ciclo.getIdCiclo());
        result.put("codigoCiclo", ciclo.getCodigoCiclo());
        result.put("estadoCiclo", ciclo.getEstadoCiclo().getEstadoCiclo());
        result.put("conteoGrupos", grupos.size());
        result.put("fechas", slots);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> guardarFechas(Long idCiclo, ConfiguracionFechasEvaluacionDTO request) {
        Ciclo ciclo = cicloRepository.findByIdConEstado(idCiclo).orElseThrow(() -> new IllegalArgumentException("Ciclo no encontrado."));
        validarCicloEditable(ciclo);
        if (request.getFechas() == null) throw new IllegalArgumentException("Debe enviar la configuración de fechas.");
        Map<String, ConfiguracionFechasEvaluacionDTO.RangoFechas> rangos = new HashMap<>();
        for (int periodo = 1; periodo <= 3; periodo++) for (String tipo : TIPOS) {
            String clave = "P" + periodo + "_" + tipo;
            ConfiguracionFechasEvaluacionDTO.RangoFechas rango = request.getFechas().get(clave);
            if (rango == null || rango.getFechaInicio() == null || rango.getFechaFin() == null)
                throw new IllegalArgumentException("Debe configurar fecha de inicio y fin para " + clave + ".");
            if (!rango.getFechaFin().isAfter(rango.getFechaInicio()))
                throw new IllegalArgumentException("La fecha de cierre debe ser posterior al inicio para " + clave + ".");
            rangos.put(clave, rango);
        }

        Map<Integer, PeriodoEvaluacion> periodos = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            final int numero = i;
            PeriodoEvaluacion periodo = periodoRepository.findByCiclo_IdCicloAndPeriodo(idCiclo, i).orElseGet(() -> {
                PeriodoEvaluacion nuevo = new PeriodoEvaluacion();
                nuevo.setCiclo(ciclo);
                nuevo.setPeriodo(numero);
                nuevo.setPorcentaje(switch (numero) {
                    case 1 -> new BigDecimal("30.00");
                    case 2 -> new BigDecimal("30.00");
                    default -> new BigDecimal("40.00");
                });
                return periodoRepository.save(nuevo);
            });
            periodos.put(i, periodo);
        }
        Map<String, TipoEvaluacion> tipos = new HashMap<>();
        for (String tipo : TIPOS) tipos.put(tipo, tipoRepository.findByTipoEvaluacionIgnoreCase(tipo)
                .orElseThrow(() -> new IllegalStateException("No está configurado el tipo de evaluación " + tipo + ".")));

        List<Evaluacion> guardar = new ArrayList<>();
        for (Grupo grupo : grupoRepository.findByCiclo_IdCiclo(idCiclo)) {
            for (int periodo = 1; periodo <= 3; periodo++) for (String tipo : TIPOS) {
                TipoEvaluacion tipoEvaluacion = tipos.get(tipo);
                Evaluacion evaluacion = evaluacionRepository
                        .findByGrupo_IdGrupoAndTipoEvaluacion_IdTipoEvaluacionAndNumeroEvaluacion(
                                grupo.getIdGrupo(), tipoEvaluacion.getIdTipoEvaluacion(), periodo)
                        .orElseGet(Evaluacion::new);
                evaluacion.setGrupo(grupo);
                evaluacion.setTipoEvaluacion(tipoEvaluacion);
                evaluacion.setNumeroEvaluacion(periodo);
                evaluacion.setPeriodoEvaluacion(periodos.get(periodo));
                ConfiguracionFechasEvaluacionDTO.RangoFechas rango = rangos.get("P" + periodo + "_" + tipo);
                evaluacion.setFechaInicio(rango.getFechaInicio());
                evaluacion.setFechaFin(rango.getFechaFin());
                guardar.add(evaluacion);
            }
        }
        evaluacionRepository.saveAll(guardar);
        auditoriaService.registrarEvento("CONFIGURACION_FECHAS_EVALUACION", "evaluaciones", idCiclo,
                "Fechas de evaluaciones configuradas para el ciclo " + ciclo.getCodigoCiclo() + "; evaluaciones actualizadas: " + guardar.size());
        return Map.of("idCiclo", idCiclo, "evaluacionesActualizadas", guardar.size(), "mensaje", "Fechas guardadas correctamente.");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarSolicitudes(String filtro) {
        String estado = filtro == null || filtro.isBlank() ? "PENDIENTE" : filtro.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PENDIENTE", "APROBADA", "RECHAZADA", "TODAS").contains(estado))
            throw new IllegalArgumentException("Filtro de estado inválido.");
        List<SolicitudCalificacion> solicitudes = estado.equals("TODAS") ? solicitudRepository.findAll()
                : solicitudRepository.findByEstadoSolicitud_EstadoSolicitudIgnoreCase(estado);
        return solicitudes.stream().sorted(Comparator.comparing(SolicitudCalificacion::getFechaSolicitud).reversed())
                .map(this::mapSolicitud).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Long> crearSolicitud(SolicitudModificacionNotaDTO request) {
        if (request.getMotivo() == null || request.getMotivo().isBlank()) throw new IllegalArgumentException("El justificante es obligatorio.");
        Long idUsuario = securityUtils.obtenerIdUsuarioAutenticado();
        Usuario docente = usuarioRepository.findById(idUsuario).orElseThrow();
        Grupo grupo = grupoRepository.findById(request.getIdGrupo()).orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado."));
        if (grupo.getDocente().getUsuario() == null || !Objects.equals(grupo.getDocente().getUsuario().getIdUsuario(), idUsuario))
            throw new IllegalArgumentException("El grupo no pertenece al docente autenticado.");
        TipoEvaluacion tipo;
        if (request.getTipoEvaluacion() != null && !request.getTipoEvaluacion().isBlank()) {
            tipo = tipoRepository.findByTipoEvaluacionIgnoreCase(request.getTipoEvaluacion().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de evaluación no encontrado."));
        } else if (request.getIdTipoEvaluacion() != null) {
            tipo = tipoRepository.findById(request.getIdTipoEvaluacion())
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de evaluación no encontrado."));
        } else {
            throw new IllegalArgumentException("Debe indicar tipoEvaluacion o idTipoEvaluacion.");
        }
        Evaluacion evaluacion = evaluacionRepository.findByGrupo_IdGrupoAndTipoEvaluacion_IdTipoEvaluacionAndNumeroEvaluacion(
                grupo.getIdGrupo(), tipo.getIdTipoEvaluacion(), request.getNumeroEvaluacion())
                .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada."));
        LocalDateTime ahora = LocalDateTime.now();
        if (!ahora.isAfter(evaluacion.getFechaFin()) || ahora.isAfter(evaluacion.getFechaFin().plusDays(7)))
            throw new IllegalArgumentException("La evaluación debe estar cerrada y dentro de los 7 días posteriores al cierre.");

        List<Inscripcion> grupoInscripciones = inscripcionRepository.findByGrupoIdGrupo(grupo.getIdGrupo()).stream()
                .filter(this::inscripcionActiva).toList();
        List<Inscripcion> seleccionadas;
        if (Boolean.TRUE.equals(request.getTodosLosAlumnos()) || request.getIdsInscripcion() == null || request.getIdsInscripcion().isEmpty()) {
            seleccionadas = grupoInscripciones;
        } else {
            Set<Long> ids = new HashSet<>(request.getIdsInscripcion());
            seleccionadas = grupoInscripciones.stream().filter(i -> ids.contains(i.getIdInscripcion())).toList();
            if (seleccionadas.size() != ids.size()) throw new IllegalArgumentException("Una o más inscripciones no pertenecen al grupo activo.");
        }
        if (seleccionadas.isEmpty()) throw new IllegalArgumentException("No hay estudiantes activos seleccionados.");

        EstadoCalificacion borrador = estadoCalificacionRepository.findByEstadoCalificacion("BORRADOR")
                .orElseThrow(() -> new IllegalStateException("Estado BORRADOR no configurado."));
        EstadoSolicitud pendiente = estadoSolicitudRepository.findByEstadoSolicitudIgnoreCase("PENDIENTE")
                .orElseThrow(() -> new IllegalStateException("Estado PENDIENTE no configurado."));
        List<Long> idsCreados = new ArrayList<>();
        for (Inscripcion inscripcion : seleccionadas) {
            Calificacion calificacion = calificacionRepository.findByInscripcionIdInscripcionAndEvaluacionIdEvaluacion(
                    inscripcion.getIdInscripcion(), evaluacion.getIdEvaluacion()).orElseGet(() -> {
                Calificacion nueva = new Calificacion();
                nueva.setInscripcion(inscripcion);
                nueva.setEvaluacion(evaluacion);
                nueva.setEstadoCalificacion(borrador);
                nueva.setUsuario(docente);
                nueva.setNota(null);
                nueva.setFechaRegistro(ahora);
                return calificacionRepository.save(nueva);
            });
            if (solicitudRepository.existsByCalificacion_IdCalificacionAndEstadoSolicitud_EstadoSolicitudIgnoreCase(
                    calificacion.getIdCalificacion(), "PENDIENTE")) continue;
            SolicitudCalificacion solicitud = new SolicitudCalificacion();
            solicitud.setCalificacion(calificacion);
            solicitud.setUsuarioSolicitante(docente);
            solicitud.setEstadoSolicitud(pendiente);
            solicitud.setMotivo(request.getMotivo().trim());
            solicitud.setFechaSolicitud(ahora);
            idsCreados.add(solicitudRepository.save(solicitud).getIdSolicitud());
        }
        if (idsCreados.isEmpty()) throw new IllegalArgumentException("Ya existe una solicitud pendiente para todos los alumnos seleccionados.");
        auditoriaService.registrarEvento("SOLICITUD_MODIFICACION_NOTA", "solicitudes_calificacion", idsCreados.get(0),
                "El docente solicitó modificar notas de " + tipo.getTipoEvaluacion() + " " + evaluacion.getNumeroEvaluacion() + " en grupo " + grupo.getCodigoGrupo());
        return idsCreados;
    }

    @Transactional(rollbackFor = Exception.class)
    public int resolverSolicitudes(ResolverSolicitudesCalificacionDTO request) {
        if (request.getIdsSolicitud() == null || request.getIdsSolicitud().isEmpty()) throw new IllegalArgumentException("Seleccione al menos una solicitud.");
        if (request.getAprobar() == null) throw new IllegalArgumentException("Indique si se aprueba o rechaza.");
        Long idAutorizador = securityUtils.obtenerIdUsuarioAutenticado();
        Usuario autorizador = usuarioRepository.findById(idAutorizador).orElseThrow();
        String nuevoEstado = request.getAprobar() ? "APROBADA" : "RECHAZADA";
        EstadoSolicitud estado = estadoSolicitudRepository.findByEstadoSolicitudIgnoreCase(nuevoEstado)
                .orElseThrow(() -> new IllegalStateException("Estado " + nuevoEstado + " no configurado."));
        List<SolicitudCalificacion> solicitudes = solicitudRepository.findAllById(request.getIdsSolicitud());
        if (solicitudes.size() != new HashSet<>(request.getIdsSolicitud()).size()) throw new IllegalArgumentException("Una o más solicitudes no existen.");
        if (solicitudes.stream().anyMatch(s -> !"PENDIENTE".equalsIgnoreCase(s.getEstadoSolicitud().getEstadoSolicitud())))
            throw new IllegalArgumentException("Solo se pueden resolver solicitudes pendientes.");
        LocalDateTime ahora = LocalDateTime.now();
        for (SolicitudCalificacion solicitud : solicitudes) {
            solicitud.setEstadoSolicitud(estado);
            solicitud.setUsuarioAutorizador(autorizador);
            solicitud.setRespuesta(request.getRespuesta() == null ? null : request.getRespuesta().trim());
            solicitud.setFechaRespuesta(ahora);
        }
        solicitudRepository.saveAll(solicitudes);
        auditoriaService.registrarEvento("RESOLUCION_SOLICITUD_NOTA", "solicitudes_calificacion", solicitudes.get(0).getIdSolicitud(),
                nuevoEstado + " solicitudes " + request.getIdsSolicitud());
        solicitudes.stream().map(SolicitudCalificacion::getUsuarioSolicitante).distinct().forEach(docente -> {
            String correo = docente.getPersona().getCorreoPersonal();
            if (correo != null && !correo.isBlank()) emailService.enviarResultadoSolicitudNota(correo,
                    docente.getPersona().getNombres() + " " + docente.getPersona().getApellidos(), nuevoEstado,
                    request.getRespuesta(), request.getAprobar());
        });
        return solicitudes.size();
    }

    private Map<String, Object> mapSolicitud(SolicitudCalificacion s) {
        Calificacion cal = s.getCalificacion(); Evaluacion ev = cal.getEvaluacion(); Grupo g = ev.getGrupo();
        Estudiante estudiante = cal.getInscripcion().getMatricula().getEstudiante();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("idSolicitud", s.getIdSolicitud()); result.put("estado", s.getEstadoSolicitud().getEstadoSolicitud());
        result.put("motivo", s.getMotivo()); result.put("respuesta", s.getRespuesta());
        result.put("fechaSolicitud", s.getFechaSolicitud()); result.put("fechaRespuesta", s.getFechaRespuesta());
        result.put("idDocente", g.getDocente().getIdDocente());
        result.put("docente", s.getUsuarioSolicitante().getPersona().getNombres() + " " + s.getUsuarioSolicitante().getPersona().getApellidos());
        result.put("correoDocente", s.getUsuarioSolicitante().getPersona().getCorreoPersonal());
        result.put("codigoMateria", g.getMateria().getCodigoMateria()); result.put("nombreMateria", g.getMateria().getNombreMateria());
        result.put("codigoGrupo", g.getCodigoGrupo()); result.put("codigoCiclo", g.getCiclo().getCodigoCiclo());
        result.put("tipoEvaluacion", ev.getTipoEvaluacion().getTipoEvaluacion()); result.put("numeroEvaluacion", ev.getNumeroEvaluacion());
        result.put("fechaFinEvaluacion", ev.getFechaFin()); result.put("nombreEstudiante", estudiante.getPersona().getNombres() + " " + estudiante.getPersona().getApellidos());
        result.put("idEstudiante", estudiante.getIdEstudiante()); result.put("carnet", estudiante.getCarnet()); result.put("notaActual", cal.getNota());
        return result;
    }

    private boolean inscripcionActiva(Inscripcion i) {
        String estadoMateria = i.getEstadoMateria() == null ? "" : i.getEstadoMateria().getEstadoMateria();
        String estado = i.getEstadoInscripcion() == null ? "" : i.getEstadoInscripcion().getEstadoInscripcion();
        return !Set.of("RETIRADA", "RETIRADO", "CANCELADA", "CANCELADO", "RECHAZADA", "RECHAZADO")
                .contains(estadoMateria.toUpperCase(Locale.ROOT))
                && !Set.of("RETIRADA", "RETIRADO", "CANCELADA", "CANCELADO", "RECHAZADA", "RECHAZADO")
                .contains(estado.toUpperCase(Locale.ROOT));
    }

    private void validarCicloEditable(Ciclo ciclo) {
        if (ciclo.getEstadoCiclo().getIdEstadoCiclo() == 3L) throw new IllegalArgumentException("No se puede cambiar un ciclo finalizado.");
    }
}
