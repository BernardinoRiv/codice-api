package com.codice.sra.services;

import com.codice.sra.dtos.HistorialAcademicoDTO;
import com.codice.sra.dtos.MateriaCicloDTO;
import com.codice.sra.dtos.MateriaHistorialDTO;
import com.codice.sra.dtos.MateriaNotasDTO;
import com.codice.sra.dtos.NotasEstudianteResponseDTO;
import com.codice.sra.dtos.NotasEvaluacionesDTO;
import com.codice.sra.models.Calificacion;
import com.codice.sra.models.Ciclo;
import com.codice.sra.models.Inscripcion;
import com.codice.sra.models.PeriodoEvaluacion;
import com.codice.sra.models.PeriodoTipoEvaluacion;
import com.codice.sra.repositories.CalificacionRepository;
import com.codice.sra.repositories.CicloRepository;
import com.codice.sra.repositories.InscripcionRepository;
import com.codice.sra.repositories.PeriodoEvaluacionRepository;
import com.codice.sra.repositories.PeriodoTipoEvaluacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EstudianteService {

    private final CicloRepository cicloRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CalificacionRepository calificacionRepository;
    private final PeriodoEvaluacionRepository periodoEvaluacionRepository;
    private final PeriodoTipoEvaluacionRepository periodoTipoEvaluacionRepository;

    public EstudianteService(CicloRepository cicloRepository,
                             InscripcionRepository inscripcionRepository,
                             CalificacionRepository calificacionRepository,
                             PeriodoEvaluacionRepository periodoEvaluacionRepository,
                             PeriodoTipoEvaluacionRepository periodoTipoEvaluacionRepository) {
        this.cicloRepository = cicloRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.calificacionRepository = calificacionRepository;
        this.periodoEvaluacionRepository = periodoEvaluacionRepository;
        this.periodoTipoEvaluacionRepository = periodoTipoEvaluacionRepository;
    }

    @Transactional(readOnly = true)
    public NotasEstudianteResponseDTO obtenerNotasCicloActual(Long idEstudiante) {
        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("No hay ciclo activo"));

        boolean esCicloActivo = esCicloActivo(cicloActivo);

        Map<String, BigDecimal> config = cargarConfiguracionEvaluacion(cicloActivo.getIdCiclo());

        List<Inscripcion> inscripciones = inscripcionRepository
                .findByEstudianteAndCiclo(idEstudiante, cicloActivo.getIdCiclo());

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

                    String estado = determinarEstado(promedioOficial, esCicloActivo);
                    materiaDTO.setEstado(estado);

                    return materiaDTO;
                })
                .collect(Collectors.toList());

        return new NotasEstudianteResponseDTO(cicloActivo.getCodigoCiclo(), materiasDTO);
    }

    private boolean esCicloActivo(Ciclo ciclo) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaFin = ciclo.getFechaFin();
        return hoy.isBefore(fechaFin) || hoy.isEqual(fechaFin);
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

    private String determinarEstado(BigDecimal promedioOficial, boolean esCicloActivo) {
        if (promedioOficial.compareTo(BigDecimal.ZERO) == 0) {
            return "En curso";
        }

        if (!esCicloActivo) {
            return promedioOficial.compareTo(BigDecimal.valueOf(6.0)) >= 0 ? "Aprobada" : "Reprobada";
        }

        return promedioOficial.compareTo(BigDecimal.valueOf(6.0)) >= 0 ? "Aprobada (Parcial)" : "Reprobada (Parcial)";
    }

    @Transactional(readOnly = true)
    public HistorialAcademicoDTO obtenerHistorialAcademico(Long idEstudiante) {
        List<Inscripcion> inscripciones = inscripcionRepository.findAllByEstudianteId(idEstudiante);

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
            boolean esCicloActivo = "ACTIVO".equalsIgnoreCase(ciclo.getEstadoCiclo().getEstadoCiclo());

            List<MateriaHistorialDTO> materiasDTO = new ArrayList<>();

            for (Inscripcion inscripcion : inscripcionesCiclo) {
                NotasEvaluacionesDTO notas = obtenerNotasPorInscripcion(inscripcion.getIdInscripcion());
                BigDecimal notaFinal = calcularNotaFinalDinamica(notas, config).setScale(1, RoundingMode.HALF_DOWN);

                String estado;
                if (esCicloActivo) {
                    estado = "Cursando";
                } else {
                    estado = notaFinal.compareTo(BigDecimal.valueOf(6.0)) >= 0 ? "Aprobada" : "Reprobada";
                }

                if (notaFinal.compareTo(BigDecimal.valueOf(6.0)) >= 0) {
                    int uv = inscripcion.getGrupo().getMateria().getUnidadesValorativas().intValue();
                    sumaNotasPonderadas = sumaNotasPonderadas.add(notaFinal.multiply(BigDecimal.valueOf(uv)));
                    totalUV += uv;

                    if (!esCicloActivo) {
                        totalAprobadas++;
                    }
                } else if (!esCicloActivo) {
                    totalReprobadas++;
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

        return new HistorialAcademicoDTO(cum, totalUV, totalAprobadas, totalReprobadas, materiasPorCiclo);
    }
}