package com.codice.sra.services;

import com.codice.sra.dtos.MateriaNotasDTO;
import com.codice.sra.dtos.NotasEstudianteResponseDTO;
import com.codice.sra.dtos.NotasEvaluacionesDTO;
import com.codice.sra.models.Calificacion;
import com.codice.sra.models.Ciclo;
import com.codice.sra.models.Inscripcion;
import com.codice.sra.repositories.CalificacionRepository;
import com.codice.sra.repositories.CicloRepository;
import com.codice.sra.repositories.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstudianteService {

    private final CicloRepository cicloRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CalificacionRepository calificacionRepository;

    public EstudianteService(CicloRepository cicloRepository,
                             InscripcionRepository inscripcionRepository,
                             CalificacionRepository calificacionRepository) {
        this.cicloRepository = cicloRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.calificacionRepository = calificacionRepository;
    }

    @Transactional(readOnly = true)
    public NotasEstudianteResponseDTO obtenerNotasCicloActual(Long idEstudiante) {
        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("No hay ciclo activo"));

        boolean esCicloActivo = esCicloActivo(cicloActivo);

        List<Inscripcion> inscripciones = inscripcionRepository
                .findByEstudianteAndCiclo(idEstudiante, cicloActivo.getIdCiclo());

        List<MateriaNotasDTO> materiasDTO = inscripciones.stream()
                .map(inscripcion -> {
                    MateriaNotasDTO materiaDTO = new MateriaNotasDTO();
                    materiaDTO.setCodigo(inscripcion.getGrupo().getMateria().getCodigoMateria());
                    materiaDTO.setNombre(inscripcion.getGrupo().getMateria().getNombreMateria());

                    NotasEvaluacionesDTO notas = obtenerNotasPorInscripcion(inscripcion.getIdInscripcion());
                    materiaDTO.setNotas(notas);

                    // Calcular promedio sin redondear (precisión completa)
                    BigDecimal promedioSinRedondear = calcularNotaFinalParcial(notas);
                    materiaDTO.setPromedioSinRedondear(promedioSinRedondear);

                    // Redondear a 1 decimal usando HALF_DOWN (solo sube si es mayor a 5)
                    BigDecimal promedioOficial = promedioSinRedondear.setScale(1, RoundingMode.HALF_DOWN);
                    materiaDTO.setPromedioOficial(promedioOficial);

                    // Determinar estado (usar el promedio oficial)
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

    private BigDecimal calcularNotaFinalParcial(NotasEvaluacionesDTO notas) {
        BigDecimal notaFinal = BigDecimal.ZERO;

        // Período 1 (30% del total)
        if (notas.getLab1() != null || notas.getPar1() != null) {
            BigDecimal notaP1 = calcularNotaPeriodo(notas.getLab1(), notas.getPar1());
            if (notaP1 != null) {
                notaFinal = notaFinal.add(notaP1.multiply(new BigDecimal("0.30")));
            }
        }

        // Período 2 (30% del total)
        if (notas.getLab2() != null || notas.getPar2() != null) {
            BigDecimal notaP2 = calcularNotaPeriodo(notas.getLab2(), notas.getPar2());
            if (notaP2 != null) {
                notaFinal = notaFinal.add(notaP2.multiply(new BigDecimal("0.30")));
            }
        }

        // Período 3 (40% del total)
        if (notas.getLab3() != null || notas.getPar3() != null) {
            BigDecimal notaP3 = calcularNotaPeriodo(notas.getLab3(), notas.getPar3());
            if (notaP3 != null) {
                notaFinal = notaFinal.add(notaP3.multiply(new BigDecimal("0.40")));
            }
        }

        // NO redondear aquí, mantener precisión completa
        return notaFinal;
    }

    private BigDecimal calcularNotaPeriodo(BigDecimal lab, BigDecimal par) {
        if (lab == null && par == null) return null;

        BigDecimal notaLab = lab != null ? lab : BigDecimal.ZERO;
        BigDecimal notaPar = par != null ? par : BigDecimal.ZERO;

        // Lab 40% + Parcial 60%
        return notaLab.multiply(new BigDecimal("0.40"))
                .add(notaPar.multiply(new BigDecimal("0.60")));
    }

    private String determinarEstado(BigDecimal promedioOficial, boolean esCicloActivo) {
        if (promedioOficial.compareTo(BigDecimal.ZERO) == 0) {
            return "En curso";
        }

        if (!esCicloActivo) {
            if (promedioOficial.compareTo(new BigDecimal("6.0")) >= 0) {
                return "Aprobada";
            } else {
                return "Reprobada";
            }
        }

        if (promedioOficial.compareTo(new BigDecimal("6.0")) >= 0) {
            return "Aprobada (Parcial)";
        } else {
            return "Reprobada (Parcial)";
        }
    }
}