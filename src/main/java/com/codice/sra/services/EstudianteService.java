package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.exceptions.ResourceNotFoundException;
import com.codice.sra.models.Calificacion;
import com.codice.sra.models.Asistencia;
import com.codice.sra.models.Clase;
import com.codice.sra.models.Ciclo;
import com.codice.sra.models.CargoEstudiante;
import com.codice.sra.models.Evaluacion;
import com.codice.sra.models.EstadoEstudiante;
import com.codice.sra.models.Estudiante;
import com.codice.sra.models.Inscripcion;
import com.codice.sra.models.Matricula;
import com.codice.sra.models.Horario;
import com.codice.sra.models.EstudianteCarrera;
import com.codice.sra.models.PensumMateria;
import com.codice.sra.models.PeriodoEvaluacion;
import com.codice.sra.models.PeriodoTipoEvaluacion;
import com.codice.sra.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.Locale;
import java.text.Normalizer;
import java.util.stream.Collectors;

@Service
public class EstudianteService {

    private final CicloRepository cicloRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CalificacionRepository calificacionRepository;
    private final PeriodoEvaluacionRepository periodoEvaluacionRepository;
    private final PeriodoTipoEvaluacionRepository periodoTipoEvaluacionRepository;
    private final HorarioRepository horarioRepository; // <-- NUEVO REPOSITORIO INYECTADO
    private final EstudianteRepository estudianteRepository;
    private final EstadoEstudianteRepository estadoEstudianteRepository;
    private final PersonaService personaService;
    private final AuditoriaService auditoriaService;
    private final EstudianteCarreraRepository estudianteCarreraRepository;
    private final MatriculaRepository matriculaRepository;
    private final PensumMateriaRepository pensumMateriaRepository;
    private final UsuarioService usuarioService;
    private final AsistenciaRepository asistenciaRepository;
    private final ClaseRepository claseRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final CargoEstudianteRepository cargoEstudianteRepository;

    public EstudianteService(CicloRepository cicloRepository,
                             InscripcionRepository inscripcionRepository,
                             CalificacionRepository calificacionRepository,
                             PeriodoEvaluacionRepository periodoEvaluacionRepository,
                             PeriodoTipoEvaluacionRepository periodoTipoEvaluacionRepository,
                             HorarioRepository horarioRepository,
                             EstudianteRepository estudianteRepository,
                             EstadoEstudianteRepository estadoEstudianteRepository,
                             PersonaService personaService,
                             AuditoriaService auditoriaService,
                             EstudianteCarreraRepository estudianteCarreraRepository,
                             MatriculaRepository matriculaRepository,
                             PensumMateriaRepository pensumMateriaRepository,
                             UsuarioService usuarioService,
                             AsistenciaRepository asistenciaRepository,
                             ClaseRepository claseRepository,
                             EvaluacionRepository evaluacionRepository,
                             CargoEstudianteRepository cargoEstudianteRepository) {
        this.cicloRepository = cicloRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.calificacionRepository = calificacionRepository;
        this.periodoEvaluacionRepository = periodoEvaluacionRepository;
        this.periodoTipoEvaluacionRepository = periodoTipoEvaluacionRepository;
        this.horarioRepository = horarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.estadoEstudianteRepository = estadoEstudianteRepository;
        this.personaService = personaService;
        this.auditoriaService = auditoriaService;
        this.estudianteCarreraRepository = estudianteCarreraRepository;
        this.matriculaRepository = matriculaRepository;
        this.pensumMateriaRepository = pensumMateriaRepository;
        this.usuarioService = usuarioService;
        this.asistenciaRepository = asistenciaRepository;
        this.claseRepository = claseRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.cargoEstudianteRepository = cargoEstudianteRepository;
    }

    @Transactional(readOnly = true)
    public NotasEstudianteResponseDTO obtenerNotasCicloActual(Long idEstudiante) {
        Ciclo cicloActivo = cicloRepository.findCicloPorEstadoActivo()
                .orElseThrow(() -> new RuntimeException("No hay ciclo activo"));

        Map<String, BigDecimal> config = cargarConfiguracionEvaluacion(cicloActivo.getIdCiclo());

        List<Inscripcion> inscripciones = inscripcionRepository
                .findByEstudianteAndCiclo(idEstudiante, cicloActivo.getIdCiclo())
                .stream()
                .filter(insc -> insc.getEstadoMateria() == null ||
                        !insc.getEstadoMateria().getEstadoMateria().equalsIgnoreCase("RETIRADA"))
                .collect(Collectors.toList());

        List<MateriaNotasDTO> materiasDTO = inscripciones.stream()
                .map(inscripcion -> {
                    MateriaNotasDTO materiaDTO = new MateriaNotasDTO();
                    materiaDTO.setCodigo(inscripcion.getGrupo().getMateria().getCodigoMateria());
                    materiaDTO.setNombre(inscripcion.getGrupo().getMateria().getNombreMateria());

                    NotasEvaluacionesDTO notas = obtenerNotasPorInscripcion(inscripcion.getIdInscripcion());
                    materiaDTO.setNotas(notas);

                    BigDecimal promedioSinRedondear = calcularNotaFinalDinamica(notas, config);
                    materiaDTO.setPromedioSinRedondear(promedioSinRedondear);

                    BigDecimal promedioOficial = promedioSinRedondear.setScale(1, RoundingMode.HALF_DOWN);
                    materiaDTO.setPromedioOficial(promedioOficial);

                    String estado = determinarEstado(promedioOficial, inscripcion, cicloActivo);
                    materiaDTO.setEstado(estado);

                    // =====================================================================
                    // EXTRACCIÓN DINÁMICA DE HORARIOS (Usando el repositorio)
                    // =====================================================================
                    List<HorarioDTO> horariosDeLaMateria = new ArrayList<>();
                    // Buscamos los horarios directamente por el ID del grupo
                    List<com.codice.sra.models.Horario> horariosEntidad = horarioRepository.findByGrupo_IdGrupo(inscripcion.getGrupo().getIdGrupo());

                    for (com.codice.sra.models.Horario h : horariosEntidad) {
                        HorarioDTO dto = new HorarioDTO();
                        dto.setDia(h.getDia().getDia()); // Asumiendo que getDia() devuelve un objeto con getDia() String
                        dto.setHoraInicio(h.getHoraInicio());
                        dto.setHoraFin(h.getHoraFin());
                        dto.setAula(h.getAula() != null ? h.getAula().getCodigoAula() : "Virtual"); // Asumiendo que Aula tiene getCodigoAula()
                        // dto.setModalidad(h.getModalidad()); // <-- Descomenta si tu entidad Horario tiene este campo
                        horariosDeLaMateria.add(dto);
                    }
                    materiaDTO.setHorarios(horariosDeLaMateria);
                    // =====================================================================

                    return materiaDTO;
                })
                .collect(Collectors.toList());

        return new NotasEstudianteResponseDTO(cicloActivo.getCodigoCiclo(), materiasDTO);
    }

    private Map<String, BigDecimal> cargarConfiguracionEvaluacion(Long idCiclo) {
        Map<String, BigDecimal> config = new HashMap<>();

        List<PeriodoEvaluacion> periodos = periodoEvaluacionRepository.findByCicloIdCiclo(idCiclo);

        for (PeriodoEvaluacion p : periodos) {
            String keyPeriodo = "P" + p.getPeriodo();
            BigDecimal porcentajePeriodo = p.getPorcentaje().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            config.put(keyPeriodo, porcentajePeriodo);

            List<PeriodoTipoEvaluacion> tipos = periodoTipoEvaluacionRepository
                    .findByPeriodoEvaluacionIdPeriodoEvaluacion(p.getIdPeriodoEvaluacion());

            for (PeriodoTipoEvaluacion t : tipos) {
                String tipoEval = t.getTipoEvaluacion().getTipoEvaluacion();
                BigDecimal porcentajeTipo = t.getPorcentaje().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                String key = keyPeriodo + "_" + tipoEval;
                config.put(key, porcentajeTipo);
            }
        }

        return config;
    }

    private NotasEvaluacionesDTO obtenerNotasPorInscripcion(Long idInscripcion) {
        List<Calificacion> calificaciones = calificacionRepository.findByInscripcionIdInscripcion(idInscripcion);
        NotasEvaluacionesDTO notas = new NotasEvaluacionesDTO();

        for (Calificacion calif : calificaciones) {
            if ("PUBLICADA".equals(calif.getEstadoCalificacion().getEstadoCalificacion())) {
                String tipo = calif.getEvaluacion().getTipoEvaluacion().getTipoEvaluacion();
                int numero = calif.getEvaluacion().getNumeroEvaluacion();

                if ("LABORATORIO".equals(tipo)) {
                    if (numero == 1) notas.setLab1(calif.getNota());
                    else if (numero == 2) notas.setLab2(calif.getNota());
                    else if (numero == 3) notas.setLab3(calif.getNota());
                } else if ("PARCIAL".equals(tipo)) {
                    if (numero == 1) notas.setPar1(calif.getNota());
                    else if (numero == 2) notas.setPar2(calif.getNota());
                    else if (numero == 3) notas.setPar3(calif.getNota());
                }
            }
        }

        return notas;
    }

    private BigDecimal calcularNotaFinalDinamica(NotasEvaluacionesDTO notas, Map<String, BigDecimal> config) {
        BigDecimal notaFinal = BigDecimal.ZERO;

        BigDecimal pctP1 = config.getOrDefault("P1", BigDecimal.valueOf(0.30));
        BigDecimal pctP2 = config.getOrDefault("P2", BigDecimal.valueOf(0.30));
        BigDecimal pctP3 = config.getOrDefault("P3", BigDecimal.valueOf(0.40));

        BigDecimal notaP1 = calcularNotaPeriodo(notas.getLab1(), notas.getPar1(), config, 1);
        if (notaP1 != null) {
            notaFinal = notaFinal.add(notaP1.multiply(pctP1));
        }

        BigDecimal notaP2 = calcularNotaPeriodo(notas.getLab2(), notas.getPar2(), config, 2);
        if (notaP2 != null) {
            notaFinal = notaFinal.add(notaP2.multiply(pctP2));
        }

        BigDecimal notaP3 = calcularNotaPeriodo(notas.getLab3(), notas.getPar3(), config, 3);
        if (notaP3 != null) {
            notaFinal = notaFinal.add(notaP3.multiply(pctP3));
        }

        return notaFinal;
    }

    private BigDecimal calcularNotaPeriodo(BigDecimal lab, BigDecimal par, Map<String, BigDecimal> config, int numeroPeriodo) {
        if (lab == null && par == null) {
            return null;
        }

        String keyPeriodo = "P" + numeroPeriodo;
        BigDecimal pctLab = config.getOrDefault(keyPeriodo + "_LABORATORIO", BigDecimal.valueOf(0.40));
        BigDecimal pctPar = config.getOrDefault(keyPeriodo + "_PARCIAL", BigDecimal.valueOf(0.60));

        BigDecimal notaLab = lab != null ? lab : BigDecimal.ZERO;
        BigDecimal notaPar = par != null ? par : BigDecimal.ZERO;

        return notaLab.multiply(pctLab).add(notaPar.multiply(pctPar));
    }

    private String determinarEstado(BigDecimal promedioOficial, Inscripcion inscripcion, Ciclo ciclo) {
        Long estadoMateriaId = inscripcion.getEstadoMateria() == null ? null : inscripcion.getEstadoMateria().getIdEstadoMateria();
        if (estadoMateriaId != null && estadoMateriaId == 5L) return "Retirada";
        if (estadoMateriaId != null && estadoMateriaId == 6L) return "Retiro pendiente";
        if (estadoMateriaId != null && estadoMateriaId == 2L) return "Aprobada";
        if (estadoMateriaId != null && estadoMateriaId == 3L) return "Reprobada";
        Long estadoCicloId = ciclo.getEstadoCiclo() == null ? null : ciclo.getEstadoCiclo().getIdEstadoCiclo();
        if (!Objects.equals(estadoCicloId, 3L)) {
            return Objects.equals(estadoCicloId, 1L) || Objects.equals(estadoCicloId, 4L) ? "Inscrita" : "Cursando";
        }
        return promedioOficial.compareTo(BigDecimal.valueOf(6.0)) >= 0 ? "Aprobada" : "Reprobada";
    }

    @Transactional(readOnly = true)
    public HistorialAcademicoDTO obtenerHistorialAcademico(Long idEstudiante) {
        List<Inscripcion> inscripciones = inscripcionRepository.findAllByEstudianteId(idEstudiante);

        String carreraEstudiante = "No asignada";
        if (!inscripciones.isEmpty()) {
            carreraEstudiante = inscripciones.get(0).getMatricula().getEstudianteCarrera().getCarreraSede().getCarrera().getNombreCarrera();
        }

        Map<Ciclo, List<Inscripcion>> inscripcionesPorCiclo = inscripciones.stream()
                .collect(Collectors.groupingBy(i -> i.getGrupo().getCiclo()));

        List<MateriaCicloDTO> materiasPorCiclo = new ArrayList<>();
        BigDecimal sumaNotasPonderadas = BigDecimal.ZERO;
        int totalUV = 0;
        int totalAprobadas = 0;
        int totalReprobadas = 0;

        for (Map.Entry<Ciclo, List<Inscripcion>> entry : inscripcionesPorCiclo.entrySet()) {
            Ciclo ciclo = entry.getKey();
            List<Inscripcion> inscripcionesCiclo = entry.getValue();
            Map<String, BigDecimal> config = cargarConfiguracionEvaluacion(ciclo.getIdCiclo());
            Long estadoCicloId = ciclo.getEstadoCiclo() == null ? null : ciclo.getEstadoCiclo().getIdEstadoCiclo();
            boolean cicloFinalizado = Objects.equals(estadoCicloId, 3L);

            List<MateriaHistorialDTO> materiasDTO = new ArrayList<>();

            for (Inscripcion inscripcion : inscripcionesCiclo) {
                NotasEvaluacionesDTO notas = obtenerNotasPorInscripcion(inscripcion.getIdInscripcion());
                BigDecimal notaFinal = calcularNotaFinalDinamica(notas, config).setScale(1, RoundingMode.HALF_DOWN);

                String estado;
                Long estadoMateriaId = inscripcion.getEstadoMateria() == null ? null : inscripcion.getEstadoMateria().getIdEstadoMateria();
                if (Objects.equals(estadoMateriaId, 5L)) {
                    estado = "Retirada";
                } else if (Objects.equals(estadoMateriaId, 6L)) {
                    estado = "Retiro pendiente";
                } else if (Objects.equals(estadoMateriaId, 2L)) {
                    estado = "Aprobada";
                } else if (Objects.equals(estadoMateriaId, 3L)) {
                    estado = "Reprobada";
                } else if (!cicloFinalizado) {
                    estado = Objects.equals(estadoCicloId, 1L) || Objects.equals(estadoCicloId, 4L) ? "Inscrita" : "Cursando";
                } else {
                    estado = notaFinal.compareTo(BigDecimal.valueOf(6.0)) >= 0 ? "Aprobada" : "Reprobada";
                }

                if (!estado.equals("Retirada") && !estado.equals("Retiro pendiente")) {
                    if (notaFinal.compareTo(BigDecimal.valueOf(6.0)) >= 0) {
                        int uv = inscripcion.getGrupo().getMateria().getUnidadesValorativas().intValue();
                        sumaNotasPonderadas = sumaNotasPonderadas.add(notaFinal.multiply(BigDecimal.valueOf(uv)));
                        totalUV += uv;

                        if (cicloFinalizado) {
                            totalAprobadas++;
                        }
                    } else if (cicloFinalizado) {
                        totalReprobadas++;
                    }
                }

                materiasDTO.add(new MateriaHistorialDTO(
                        inscripcion.getGrupo().getMateria().getCodigoMateria(),
                        inscripcion.getGrupo().getMateria().getNombreMateria(),
                        inscripcion.getGrupo().getMateria().getUnidadesValorativas().intValue(),
                        notaFinal,
                        estado
                ));
            }

            materiasDTO.sort(Comparator.comparing(MateriaHistorialDTO::getCodigoMateria));

            materiasPorCiclo.add(new MateriaCicloDTO(
                    ciclo.getCodigoCiclo(),
                    ciclo.getAnio(),
                    ciclo.getNumeroCiclo(),
                    ciclo.getEstadoCiclo().getEstadoCiclo(),
                    materiasDTO
            ));
        }

        materiasPorCiclo.sort(Comparator.comparing(MateriaCicloDTO::getAnio).reversed()
                .thenComparing(Comparator.comparing(MateriaCicloDTO::getNumeroCiclo).reversed()));

        BigDecimal cum = totalUV > 0
                ? sumaNotasPonderadas.divide(BigDecimal.valueOf(totalUV), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new HistorialAcademicoDTO(cum, totalUV, totalAprobadas, totalReprobadas, materiasPorCiclo, carreraEstudiante);
    }

    @Transactional(readOnly = true)
    public EstudianteDashboardDTO obtenerDashboard(Long idEstudiante) {
        ZoneId zona = ZoneId.of("America/El_Salvador");
        LocalDateTime ahora = LocalDateTime.now(zona);
        LocalDate hoy = ahora.toLocalDate();
        LocalTime horaActual = ahora.toLocalTime();
        Ciclo cicloActivo = cicloRepository.findCicloPorEstadoActivo().orElse(null);
        List<Inscripcion> inscripciones = inscripcionRepository.findAllByEstudianteId(idEstudiante);
        List<Matricula> matriculas = matriculaRepository.findByEstudiante_IdEstudiante(idEstudiante);

        Map<Long, Ciclo> ciclos = new HashMap<>();
        Map<Long, List<Inscripcion>> inscripcionesPorCiclo = new HashMap<>();
        for (Matricula matricula : matriculas) ciclos.put(matricula.getCiclo().getIdCiclo(), matricula.getCiclo());
        for (Inscripcion inscripcion : inscripciones) {
            Ciclo ciclo = inscripcion.getGrupo().getCiclo();
            ciclos.put(ciclo.getIdCiclo(), ciclo);
            inscripcionesPorCiclo.computeIfAbsent(ciclo.getIdCiclo(), ignored -> new ArrayList<>()).add(inscripcion);
        }

        List<Inscripcion> inscripcionesActivas = cicloActivo == null ? List.of() : inscripciones.stream()
                .filter(i -> Objects.equals(i.getGrupo().getCiclo().getIdCiclo(), cicloActivo.getIdCiclo()))
                .filter(i -> !esRetirada(i))
                .toList();
        List<EstudianteDashboardDTO.ClaseHoy> clasesHoy = new ArrayList<>();
        List<EstudianteDashboardDTO.EvaluacionProxima> evaluacionesProximas = new ArrayList<>();
        String diaActual = normalizar(ahora.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-SV")));
        Map<Long, Evaluacion> evaluacionesUnicas = new HashMap<>();
        for (Inscripcion inscripcion : inscripcionesActivas) {
            Long idGrupo = inscripcion.getGrupo().getIdGrupo();
            for (Horario horario : horarioRepository.findByGrupo_IdGrupo(idGrupo)) {
                if (!normalizar(horario.getDia().getDia()).equals(diaActual)) continue;
                clasesHoy.add(new EstudianteDashboardDTO.ClaseHoy(idGrupo,
                        inscripcion.getGrupo().getMateria().getCodigoMateria(),
                        inscripcion.getGrupo().getMateria().getNombreMateria(), inscripcion.getGrupo().getCodigoGrupo(),
                        horario.getHoraInicio(), horario.getHoraFin(),
                        horario.getAula() == null ? "Virtual" : horario.getAula().getCodigoAula(),
                        !horaActual.isBefore(horario.getHoraInicio().minusMinutes(15)) && !horaActual.isAfter(horario.getHoraFin())));
            }
            for (Evaluacion evaluacion : evaluacionRepository.findByGrupoIdGrupo(idGrupo)) {
                if (evaluacion.getFechaFin() == null || evaluacion.getFechaInicio() == null
                        || evaluacion.getFechaFin().isBefore(ahora) || evaluacion.getFechaInicio().isAfter(ahora.plusDays(7))) continue;
                evaluacionesUnicas.put(evaluacion.getIdEvaluacion(), evaluacion);
            }
        }
        clasesHoy.sort(Comparator.comparing(EstudianteDashboardDTO.ClaseHoy::horaInicio));
        for (Evaluacion evaluacion : evaluacionesUnicas.values()) {
            evaluacionesProximas.add(new EstudianteDashboardDTO.EvaluacionProxima(evaluacion.getIdEvaluacion(),
                    evaluacion.getGrupo().getMateria().getCodigoMateria(), evaluacion.getGrupo().getMateria().getNombreMateria(),
                    evaluacion.getTipoEvaluacion().getTipoEvaluacion(), evaluacion.getNumeroEvaluacion(),
                    evaluacion.getFechaInicio(), evaluacion.getFechaFin(),
                    !ahora.isBefore(evaluacion.getFechaInicio()) && !ahora.isAfter(evaluacion.getFechaFin()),
                    ChronoUnit.DAYS.between(hoy, evaluacion.getFechaFin().toLocalDate())));
        }
        evaluacionesProximas.sort(Comparator.comparing(EstudianteDashboardDTO.EvaluacionProxima::fechaFin));

        List<Ciclo> ciclosFinalizados = ciclos.values().stream()
                .filter(c -> c.getEstadoCiclo() != null && Objects.equals(c.getEstadoCiclo().getIdEstadoCiclo(), 3L))
                .filter(c -> !inscripcionesPorCiclo.getOrDefault(c.getIdCiclo(), List.of()).isEmpty())
                .sorted(Comparator.comparing(Ciclo::getAnio).thenComparing(Ciclo::getNumeroCiclo)).toList();
        List<EstudianteDashboardDTO.EvolucionCum> evolucionCum = new ArrayList<>();
        for (Ciclo ciclo : ciclosFinalizados) {
            List<Inscripcion> delCiclo = inscripcionesPorCiclo.getOrDefault(ciclo.getIdCiclo(), List.of());
            BigDecimal sumaCiclo = BigDecimal.ZERO;
            int uvCiclo = 0;
            int aprobadasCiclo = 0;
            for (Inscripcion i : delCiclo) {
                if (esExcluidaCum(i)) continue;
                BigDecimal nota = notaFinalHistorial(i, ciclo);
                int uv = i.getGrupo().getMateria().getUnidadesValorativas().intValue();
                sumaCiclo = sumaCiclo.add(nota.multiply(BigDecimal.valueOf(uv)));
                uvCiclo += uv;
                if (nota.compareTo(new BigDecimal("6.0")) >= 0) aprobadasCiclo++;
            }
            BigDecimal promedioCiclo = uvCiclo == 0 ? BigDecimal.ZERO.setScale(2)
                    : sumaCiclo.divide(BigDecimal.valueOf(uvCiclo), 2, RoundingMode.HALF_UP);
            BigDecimal cumAlCierre = calcularCumAlCierre(ciclosFinalizados, inscripcionesPorCiclo, ciclo);
            evolucionCum.add(new EstudianteDashboardDTO.EvolucionCum(ciclo.getCodigoCiclo(), ciclo.getAnio(),
                    ciclo.getNumeroCiclo(), promedioCiclo, cumAlCierre, aprobadasCiclo));
        }

        List<Ciclo> ciclosConActividad = new ArrayList<>(ciclos.values());
        ciclosConActividad.sort(Comparator.comparing(Ciclo::getAnio).thenComparing(Ciclo::getNumeroCiclo).reversed());
        List<EstudianteDashboardDTO.AsistenciaCiclo> asistenciasPorCiclo = new ArrayList<>();
        int sesionesActivas = 0;
        int inasistenciasActivas = 0;
        for (Ciclo ciclo : ciclosConActividad) {
            List<EstudianteDashboardDTO.AsistenciaMateria> materias = new ArrayList<>();
            for (Inscripcion inscripcion : inscripcionesPorCiclo.getOrDefault(ciclo.getIdCiclo(), List.of())) {
                Long grupoId = inscripcion.getGrupo().getIdGrupo();
                int sesiones = claseRepository.findByGrupo_IdGrupo(grupoId).size();
                List<Asistencia> registros = asistenciaRepository.findByGrupoIdGrupo(grupoId).stream()
                        .filter(a -> Objects.equals(a.getInscripcion().getIdInscripcion(), inscripcion.getIdInscripcion())).toList();
                int presentes = 0, tardanzas = 0, justificadas = 0, ausencias = 0;
                for (Asistencia asistencia : registros) {
                    String estado = normalizar(asistencia.getEstadoAsistencia().getEstadoAsistencia());
                    if (estado.contains("PRESENTE") || estado.contains("ASISTIO")) presentes++;
                    else if (estado.contains("TARDANZA") || estado.contains("TARDE")) tardanzas++;
                    else if (estado.contains("JUSTIFICADA")) justificadas++;
                    else if (estado.contains("AUSENTE") || estado.contains("INASISTENCIA") || estado.contains("FALTA")) ausencias++;
                }
                BigDecimal porcentaje = sesiones == 0 ? new BigDecimal("100.0")
                        : BigDecimal.valueOf(sesiones - ausencias).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(sesiones), 1, RoundingMode.HALF_UP);
                String alerta = sesiones > 0 && porcentaje.compareTo(new BigDecimal("80.0")) < 0 ? "EN_RIESGO"
                        : sesiones > 0 && porcentaje.compareTo(new BigDecimal("85.0")) < 0 ? "PREVENTIVO" : "OPTIMO";
                materias.add(new EstudianteDashboardDTO.AsistenciaMateria(
                        inscripcion.getGrupo().getMateria().getCodigoMateria(), inscripcion.getGrupo().getMateria().getNombreMateria(),
                        inscripcion.getGrupo().getCodigoGrupo(), sesiones, presentes, tardanzas, justificadas, ausencias, porcentaje, alerta));
                if (cicloActivo != null && Objects.equals(ciclo.getIdCiclo(), cicloActivo.getIdCiclo())
                        && !esRetirada(inscripcion)) {
                    sesionesActivas += sesiones;
                    inasistenciasActivas += ausencias;
                }
            }
            materias.sort(Comparator.comparingInt(EstudianteDashboardDTO.AsistenciaMateria::inasistencias).reversed()
                    .thenComparing(EstudianteDashboardDTO.AsistenciaMateria::porcentajeAsistencia));
            asistenciasPorCiclo.add(new EstudianteDashboardDTO.AsistenciaCiclo(ciclo.getIdCiclo(), ciclo.getCodigoCiclo(),
                    ciclo.getEstadoCiclo().getEstadoCiclo(), materias));
        }

        BigDecimal asistenciaGlobal = sesionesActivas == 0 ? new BigDecimal("100.0")
                : BigDecimal.valueOf(sesionesActivas - inasistenciasActivas).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sesionesActivas), 1, RoundingMode.HALF_UP);
        String estadoAsistencia = asistenciaGlobal.compareTo(new BigDecimal("85.0")) >= 0 ? "OPTIMO"
                : asistenciaGlobal.compareTo(new BigDecimal("80.0")) >= 0 ? "PREVENTIVO" : "EN_RIESGO";

        HistorialAcademicoDTO historial = obtenerHistorialAcademico(idEstudiante);
        BigDecimal cumActual = historial.getCumAcumulado() == null ? BigDecimal.ZERO.setScale(2) : historial.getCumAcumulado().setScale(2, RoundingMode.HALF_UP);
        BigDecimal variacionCum = evolucionCum.size() < 2 ? BigDecimal.ZERO.setScale(2)
                : cumActual.subtract(evolucionCum.get(evolucionCum.size() - 2).cumAcumulado()).setScale(2, RoundingMode.HALF_UP);
        int totalMateriasPensum = estudianteCarreraRepository.findFirstByEstudiante_IdEstudianteOrderByFechaInicioDesc(idEstudiante)
                .map(ec -> pensumMateriaRepository.findByPensumIdOrderByCicloRecomendado(ec.getPensum().getIdPensum()).size()).orElse(0);
        int materiasAprobadas = historial.getTotalMateriasAprobadas();
        BigDecimal avance = totalMateriasPensum == 0 ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(materiasAprobadas).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalMateriasPensum), 1, RoundingMode.HALF_UP);
        int uvAprobadas = calcularUvAprobadas(ciclosFinalizados, inscripcionesPorCiclo);

        BigDecimal saldoPendiente = BigDecimal.ZERO;
        BigDecimal montoMoraTotal = BigDecimal.ZERO;
        int cuotasPendientes = 0;
        int cuotasEnMora = 0;
        List<CargoEstudiante> cargosPendientes = new ArrayList<>();
        if (cicloActivo != null) {
            Matricula matriculaActiva = matriculas.stream().filter(m -> Objects.equals(m.getCiclo().getIdCiclo(), cicloActivo.getIdCiclo()))
                    .max(Comparator.comparing(Matricula::getFechaMatricula)).orElse(null);
            if (matriculaActiva != null) cargosPendientes = cargoEstudianteRepository.findByMatricula_IdMatricula(matriculaActiva.getIdMatricula()).stream()
                    .filter(c -> {
                        String estado = normalizar(c.getEstadoCargo().getEstadoCargo());
                        return !estado.equals("PAGADO") && !estado.equals("ANULADO");
                    })
                    .sorted(Comparator.comparing(CargoEstudiante::getFechaVencimiento,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(CargoEstudiante::getNumeroCuota,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            for (CargoEstudiante cargo : cargosPendientes) {
                String estado = normalizar(cargo.getEstadoCargo().getEstadoCargo());
                String tipo = normalizar(cargo.getConceptoCobro().getTipoCobro().getTipoCobro());
                boolean vencido = cargo.getFechaVencimiento() != null && cargo.getFechaVencimiento().isBefore(hoy);
                if (vencido) cuotasEnMora++;
                cuotasPendientes++;
                BigDecimal neto = cargo.getMontoBase().subtract(cargo.getMontoDescuento());
                BigDecimal totalGuardado = cargo.getMontoTotal() == null ? neto : cargo.getMontoTotal();
                BigDecimal mora = vencido && tipo.contains("CUOTA") ? new BigDecimal("5.00") : BigDecimal.ZERO;
                BigDecimal totalReal = mora.signum() > 0 ? totalGuardado.max(neto.add(mora)) : totalGuardado;
                saldoPendiente = saldoPendiente.add(totalReal);
                montoMoraTotal = montoMoraTotal.add(mora);
            }
        }
        CargoEstudiante proximoPago = cargosPendientes.isEmpty() ? null : cargosPendientes.get(0);
        String proximoPagoConcepto = proximoPago == null ? null : proximoPago.getConceptoCobro().getTipoCobro().getTipoCobro();
        BigDecimal proximoPagoMonto = proximoPago == null ? null : montoPendienteConMora(proximoPago, hoy);
        LocalDate fechaProximoPago = proximoPago == null ? null : proximoPago.getFechaVencimiento();
        boolean proximoPagoVencido = fechaProximoPago != null && fechaProximoPago.isBefore(hoy);
        boolean solvente = cuotasPendientes == 0 && saldoPendiente.compareTo(BigDecimal.ZERO) == 0;
        EstudianteDashboardDTO.Kpis kpis = new EstudianteDashboardDTO.Kpis(cumActual, variacionCum, materiasAprobadas,
                totalMateriasPensum, avance, uvAprobadas, asistenciaGlobal, estadoAsistencia, cuotasPendientes,
                cuotasEnMora, cuotasEnMora > 0, montoMoraTotal.setScale(2, RoundingMode.HALF_UP),
                saldoPendiente.setScale(2, RoundingMode.HALF_UP), solvente, proximoPagoConcepto,
                proximoPagoMonto == null ? null : proximoPagoMonto.setScale(2, RoundingMode.HALF_UP),
                fechaProximoPago, proximoPagoVencido);
        return new EstudianteDashboardDTO(kpis, clasesHoy, evaluacionesProximas, evolucionCum,
                asistenciasPorCiclo, cicloActivo == null ? null : cicloActivo.getIdCiclo());
    }

    private BigDecimal notaFinalHistorial(Inscripcion inscripcion, Ciclo ciclo) {
        return calcularNotaFinalDinamica(obtenerNotasPorInscripcion(inscripcion.getIdInscripcion()),
                cargarConfiguracionEvaluacion(ciclo.getIdCiclo())).setScale(1, RoundingMode.HALF_DOWN);
    }

    private BigDecimal calcularCumAlCierre(List<Ciclo> ciclosFinalizados, Map<Long, List<Inscripcion>> porCiclo, Ciclo hasta) {
        BigDecimal suma = BigDecimal.ZERO;
        int uvTotal = 0;
        for (Ciclo ciclo : ciclosFinalizados) {
            if (compararCiclos(ciclo, hasta) > 0) continue;
            for (Inscripcion inscripcion : porCiclo.getOrDefault(ciclo.getIdCiclo(), List.of())) {
                if (esExcluidaCum(inscripcion)) continue;
                BigDecimal nota = notaFinalHistorial(inscripcion, ciclo);
                if (nota.compareTo(new BigDecimal("6.0")) < 0) continue;
                int uv = inscripcion.getGrupo().getMateria().getUnidadesValorativas().intValue();
                suma = suma.add(nota.multiply(BigDecimal.valueOf(uv)));
                uvTotal += uv;
            }
        }
        return uvTotal == 0 ? BigDecimal.ZERO.setScale(2) : suma.divide(BigDecimal.valueOf(uvTotal), 2, RoundingMode.HALF_UP);
    }

    private int calcularUvAprobadas(List<Ciclo> ciclosFinalizados, Map<Long, List<Inscripcion>> porCiclo) {
        int total = 0;
        for (Ciclo ciclo : ciclosFinalizados) for (Inscripcion i : porCiclo.getOrDefault(ciclo.getIdCiclo(), List.of())) {
            if (!esExcluidaCum(i) && notaFinalHistorial(i, ciclo).compareTo(new BigDecimal("6.0")) >= 0)
                total += i.getGrupo().getMateria().getUnidadesValorativas().intValue();
        }
        return total;
    }

    private int compararCiclos(Ciclo a, Ciclo b) {
        int porAnio = a.getAnio().compareTo(b.getAnio());
        return porAnio == 0 ? a.getNumeroCiclo().compareTo(b.getNumeroCiclo()) : porAnio;
    }

    private boolean esRetirada(Inscripcion inscripcion) {
        if (inscripcion.getEstadoMateria() == null) return false;
        return Objects.equals(inscripcion.getEstadoMateria().getIdEstadoMateria(), 5L)
                || "RETIRADA".equals(normalizar(inscripcion.getEstadoMateria().getEstadoMateria()));
    }

    private boolean esExcluidaCum(Inscripcion inscripcion) {
        if (inscripcion.getEstadoMateria() == null) return false;
        Long estadoId = inscripcion.getEstadoMateria().getIdEstadoMateria();
        String estado = normalizar(inscripcion.getEstadoMateria().getEstadoMateria());
        return Objects.equals(estadoId, 5L) || Objects.equals(estadoId, 6L) || estado.contains("RETIR");
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal montoPendienteConMora(CargoEstudiante cargo, LocalDate hoy) {
        BigDecimal neto = cargo.getMontoBase().subtract(cargo.getMontoDescuento());
        BigDecimal total = cargo.getMontoTotal() == null ? neto : cargo.getMontoTotal();
        String tipo = normalizar(cargo.getConceptoCobro().getTipoCobro().getTipoCobro());
        boolean vencido = cargo.getFechaVencimiento() != null && cargo.getFechaVencimiento().isBefore(hoy);
        if (vencido && tipo.contains("CUOTA")) return total.max(neto.add(new BigDecimal("5.00")));
        return total;
    }

    @Transactional(readOnly = true)
    public List<EstudianteListaResponseDTO> obtenerTodosLosEstudiantes() {

        Map<Long, List<EstudianteCarreraListaDTO>> carrerasPorEstudiante = estudianteCarreraRepository.findAllConCarreraSedeYEstado().stream()
                .collect(Collectors.groupingBy(
                        ec -> ec.getEstudiante().getIdEstudiante(),
                        Collectors.mapping(ec -> new EstudianteCarreraListaDTO(
                                ec.getIdEstudianteCarrera(),
                                ec.getCarreraSede().getCarrera().getNombreCarrera(),
                                ec.getCarreraSede().getSede().getNombreSede(),
                                ec.getEstadoTrayectoria().getEstadoTrayectoria(),
                                ec.getFechaInicio(),
                                ec.getFechaFin()
                        ), Collectors.toList())
                ));

        return estudianteRepository.findAllBy().stream()
                .map(estudiante -> new EstudianteListaResponseDTO(
                        estudiante.getIdEstudiante(),
                        estudiante.getUsuario() != null ? estudiante.getUsuario().getIdUsuario() : null,
                        estudiante.getCarnet(),
                        estudiante.getPersona().getNombres(),
                        estudiante.getPersona().getApellidos(),
                        estudiante.getPersona().getNumeroDocumento(),
                        estudiante.getPersona().getCorreoPersonal(),
                        estudiante.getUsuario() != null ? estudiante.getUsuario().getCorreoInstitucional() : null,
                        estudiante.getPersona().getTelefono(),
                        estudiante.getPersona().getDireccion(),
                        estudiante.getPersona().getDistrito() != null ? estudiante.getPersona().getDistrito().getIdDistrito() : null,
                        estudiante.getEstadoEstudiante().getEstadoEstudiante(),
                        estudiante.getUsuario() != null && estudiante.getUsuario().getEstadoUsuario() != null
                                ? estudiante.getUsuario().getEstadoUsuario().getEstadoUsuario() : "SIN USUARIO",
                        estudiante.getFechaIngreso(),
                        carrerasPorEstudiante.getOrDefault(estudiante.getIdEstudiante(), List.of()),
                        estudiante.getPersona().getFechaNacimiento(),
                        estudiante.getPersona().getSexo(),
                        estudiante.getPersona().getDistrito() != null
                                ? estudiante.getPersona().getDistrito().getDepartamento().getIdDepartamento() : null,

                        estudiante.getPersona().getDistrito() != null
                                ? estudiante.getPersona().getDistrito().getDepartamento().getNombre() : null,

                        estudiante.getPersona().getDistrito() != null
                                ? estudiante.getPersona().getDistrito().getNombre() : null
                ))
                .toList();
    }

    @Transactional
    public void actualizarEstudiante(Long idEstudiante, PersonaContactoEdicionDTO request) {
        Estudiante estudiante = buscarEstudianteParaActualizar(idEstudiante);
        if (personaService.actualizarDatosContacto(estudiante.getPersona().getIdPersona(), request)) {
            auditoriaService.registrarEvento("ACTUALIZACION_ESTUDIANTE", "estudiantes", idEstudiante,
                    "Se actualizaron los datos de contacto del estudiante.");
        }
    }

    @Transactional
    public void cambiarEstadoEstudiante(Long idEstudiante, EstudianteEstadoRequestDTO request) {
        Estudiante estudiante = buscarEstudianteParaActualizar(idEstudiante);
        EstadoEstudiante nuevoEstado = estadoEstudianteRepository.findById(request.getIdEstadoEstudiante())
                .filter(estado -> Boolean.TRUE.equals(estado.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El estado de estudiante no existe o está dado de baja: " + request.getIdEstadoEstudiante()));

        EstadoEstudiante estadoAnterior = estudiante.getEstadoEstudiante();
        if (estadoAnterior.getIdEstadoEstudiante().equals(nuevoEstado.getIdEstadoEstudiante())) {
            throw new IllegalArgumentException("El estudiante ya posee el estado '" + nuevoEstado.getEstadoEstudiante() + "'");
        }

        estudiante.setEstadoEstudiante(nuevoEstado);
        auditoriaService.registrarEvento("CAMBIO_ESTADO_ESTUDIANTE", "estudiantes", idEstudiante,
                "Cambio de '%s' a '%s'. Motivo: %s".formatted(estadoAnterior.getEstadoEstudiante(),
                        nuevoEstado.getEstadoEstudiante(), request.getMotivo().trim()));
    }

    private Estudiante buscarEstudianteParaActualizar(Long idEstudiante) {
        if (idEstudiante == null || idEstudiante <= 0) {
            throw new IllegalArgumentException("El ID del estudiante debe ser positivo");
        }
        return estudianteRepository.findForUpdateByIdEstudiante(idEstudiante)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con ID: " + idEstudiante));
    }

    @Transactional(readOnly = true)
    public EstudianteListaResponseDTO obtenerEstudiantePorId(Long idEstudiante) {
        if (idEstudiante == null || idEstudiante <= 0) {
            throw new IllegalArgumentException("El ID del estudiante debe ser positivo");
        }

        EstudianteListaResponseDTO respuesta = obtenerTodosLosEstudiantes().stream()
                .filter(estudiante -> estudiante.getIdEstudiante().equals(idEstudiante))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con ID: " + idEstudiante));

        Map<Long, EstudianteCarrera> trayectorias = estudianteCarreraRepository.findByEstudianteIdConPensum(idEstudiante)
                .stream().collect(Collectors.toMap(EstudianteCarrera::getIdEstudianteCarrera, ec -> ec));
        for (EstudianteCarreraListaDTO carrera : respuesta.getCarreras()) {
            EstudianteCarrera trayectoria = trayectorias.get(carrera.getIdEstudianteCarrera());
            if (trayectoria != null) completarIndicadoresAcademicos(carrera, trayectoria);
        }
        return respuesta;
    }

    private void completarIndicadoresAcademicos(EstudianteCarreraListaDTO dto, EstudianteCarrera trayectoria) {
        if (trayectoria.getPensum() == null) return;
        dto.setVersionPensum(trayectoria.getPensum().getVersion());
        List<PensumMateria> pensum = pensumMateriaRepository.findByPensumIdOrderByCicloRecomendado(trayectoria.getPensum().getIdPensum());
        Set<Long> materiasObligatorias = pensum.stream().filter(pm -> Boolean.TRUE.equals(pm.getObligatoria()))
                .map(pm -> pm.getMateria().getIdMateria()).collect(Collectors.toSet());
        dto.setTotalMaterias(materiasObligatorias.size());
        dto.setTotalCiclosPensum(pensum.stream().map(PensumMateria::getCicloRecomendado)
                .filter(n -> n != null).max(Integer::compareTo).orElse(0));

        List<Matricula> matriculas = matriculaRepository.findByEstudianteCarreraId(trayectoria.getIdEstudianteCarrera()).stream()
                .filter(m -> "MATRICULADA".equalsIgnoreCase(m.getEstadoMatricula().getEstadoMatricula())).toList();
        Set<Long> ciclosMatriculados = matriculas.stream().map(m -> m.getCiclo().getIdCiclo()).collect(Collectors.toSet());
        dto.setCiclosMatriculados(ciclosMatriculados.size());

        List<Inscripcion> inscripciones = inscripcionRepository.findByEstudianteCarreraId(trayectoria.getIdEstudianteCarrera());
        Set<Long> aprobadas = new HashSet<>();
        Set<Long> reprobadas = new HashSet<>();
        Set<Long> cursando = new HashSet<>();
        Set<Long> ciclosCursados = new HashSet<>();
        int retiradas = 0;
        Map<Long, Map<String, BigDecimal>> configuraciones = new HashMap<>();
        LocalDate hoy = LocalDate.now();

        for (Inscripcion i : inscripciones) {
            if (!"MATRICULADA".equalsIgnoreCase(i.getMatricula().getEstadoMatricula().getEstadoMatricula())) continue;
            String estadoInscripcion = i.getEstadoInscripcion() == null ? "" : i.getEstadoInscripcion().getEstadoInscripcion();
            if ("CANCELADA".equalsIgnoreCase(estadoInscripcion)) continue;
            String estadoMateria = i.getEstadoMateria() == null ? "" : i.getEstadoMateria().getEstadoMateria();
            if ("RETIRADA".equalsIgnoreCase(estadoMateria) || "RETIRADA".equalsIgnoreCase(estadoInscripcion)) {
                retiradas++;
                continue;
            }

            Ciclo ciclo = i.getMatricula().getCiclo();
            Long idMateria = i.getGrupo().getMateria().getIdMateria();
            boolean finalizado = ciclo.getFechaFin() != null && ciclo.getFechaFin().isBefore(hoy);
            if (!finalizado) {
                cursando.add(idMateria);
                continue;
            }
            ciclosCursados.add(ciclo.getIdCiclo());
            Map<String, BigDecimal> config = configuraciones.computeIfAbsent(ciclo.getIdCiclo(), this::cargarConfiguracionEvaluacion);
            BigDecimal nota = calcularNotaFinalDinamica(obtenerNotasPorInscripcion(i.getIdInscripcion()), config)
                    .setScale(1, RoundingMode.HALF_DOWN);
            if (nota.compareTo(BigDecimal.valueOf(6.0)) >= 0) aprobadas.add(idMateria);
            else reprobadas.add(idMateria);
        }

        reprobadas.removeAll(aprobadas);
        cursando.removeAll(aprobadas);
        int aprobadasPensum = (int) aprobadas.stream().filter(materiasObligatorias::contains).count();
        dto.setMateriasAprobadas(aprobadasPensum);
        dto.setMateriasReprobadas(reprobadas.size());
        dto.setMateriasCursando(cursando.size());
        dto.setMateriasRetiradas(retiradas);
        dto.setCiclosCursados(ciclosCursados.size());
        dto.setPorcentajeAvance(materiasObligatorias.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(aprobadasPensum * 100L).divide(BigDecimal.valueOf(materiasObligatorias.size()), 2, RoundingMode.HALF_UP));

        // Durante el inicio de la carrera usamos la duración oficial del nivel académico.
        // Con dos ciclos efectivamente cursados, usamos el ritmo histórico de aprobaciones.
        Integer anioBase = null;
        if (trayectoria.getFechaInicio() != null && trayectoria.getCarreraSede() != null
                && trayectoria.getCarreraSede().getCarrera() != null
                && trayectoria.getCarreraSede().getCarrera().getNivel() != null) {
            String nivel = trayectoria.getCarreraSede().getCarrera().getNivel().getNivel();
            if (nivel != null) {
                String nombreNivel = java.text.Normalizer.normalize(nivel, java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").toUpperCase(java.util.Locale.ROOT);
                int duracion = nombreNivel.contains("LICENCIATURA") ? 5
                        : (nombreNivel.contains("TECNIC") || nombreNivel.contains("MAESTRIA") ? 2 : 0);
                if (duracion > 0) anioBase = trayectoria.getFechaInicio().getYear() + duracion;
            }
        }

        int pendientes = materiasObligatorias.size() - aprobadasPensum;
        if (trayectoria.getFechaFin() != null && pendientes == 0) {
            dto.setAnioFinalizacionEstimado(trayectoria.getFechaFin().getYear());
        } else if (pendientes == 0 && !materiasObligatorias.isEmpty()) {
            dto.setAnioFinalizacionEstimado(hoy.getYear());
        } else if (ciclosCursados.size() >= 2 && aprobadasPensum > 0) {
            double promedio = (double) aprobadasPensum / ciclosCursados.size();
            int ciclosRestantes = (int) Math.ceil(pendientes / promedio);
            dto.setAnioFinalizacionEstimado(hoy.getYear() + (int) Math.ceil(ciclosRestantes / 2.0));
        } else {
            dto.setAnioFinalizacionEstimado(anioBase);
        }
    }


    @Transactional
    public void bloquearUsuario(Long idEstudiante, EstudianteBloqueoUsuarioDTO request) {
        Estudiante estudiante = buscarEstudianteParaActualizar(idEstudiante);

        if (estudiante.getUsuario() == null) {
            throw new IllegalArgumentException("El estudiante no tiene una cuenta institucional asignada.");
        }

        usuarioService.bloquearUsuarioEstudiante(estudiante.getUsuario().getIdUsuario(), request.getMotivo());
    }

    @Transactional
    public void desbloquearUsuario(Long idEstudiante, EstudianteBloqueoUsuarioDTO request) {
        Estudiante estudiante = buscarEstudianteParaActualizar(idEstudiante);

        if (estudiante.getUsuario() == null) {
            throw new IllegalArgumentException("El estudiante no tiene una cuenta institucional asignada.");
        }

        usuarioService.desbloquearUsuarioEstudiante(estudiante.getUsuario().getIdUsuario(), request.getMotivo());
    }

    @Transactional
    public void actualizarDatosPersonales(Long idEstudiante, EstudianteDatosPersonalesDTO request) {
        Estudiante estudiante = buscarEstudianteParaActualizar(idEstudiante);

        personaService.actualizarDatosPersonalesEstudiante(
                estudiante.getPersona().getIdPersona(),
                request
        );
    }

    @Transactional
    public void cambiarDocumento(Long idEstudiante, EstudianteCambioDocumentoDTO request) {
        Estudiante estudiante = buscarEstudianteParaActualizar(idEstudiante);

        personaService.cambiarDocumentoEstudiante(
                estudiante.getPersona().getIdPersona(),
                request
        );
    }

}
