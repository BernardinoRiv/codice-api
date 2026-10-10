package com.codice.sra.services;

import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.dtos.EstadisticasAsistenciaDTO;
import com.codice.sra.dtos.FinalizarAsistenciaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final ClaseRepository claseRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EstadoAsistenciaRepository estadoAsistenciaRepository;
    private final InscripcionRepository inscripcionRepository;

    @Transactional
    public Long crearNuevaClase(Long idGrupo) {
        LocalDate hoy = LocalDate.now();
        Optional<Clase> claseExistente = claseRepository.findByGrupoAndFecha(idGrupo, hoy);

        if (claseExistente.isPresent()) {
            return claseExistente.get().getIdClase();
        }

        Grupo grupo = new Grupo();
        grupo.setIdGrupo(idGrupo);

        Clase clase = new Clase();
        clase.setGrupo(grupo);
        clase.setFechaClase(hoy);

        return claseRepository.save(clase).getIdClase();
    }

    @Transactional
    public void guardarAsistenciaMasiva(Long idClase, Map<Long, LocalDateTime> estudiantesPresentesMap, List<FinalizarAsistenciaDTO.ActualizacionManualDTO> manuales) {
        Clase clase = claseRepository.findById(idClase)
                .orElseThrow(() -> new RuntimeException("Clase no encontrada"));

        EstadoAsistencia estadoPresente = estadoAsistenciaRepository.findByEstadoAsistencia("PRESENTE")
                .orElseThrow(() -> new RuntimeException("Estado PRESENTE no encontrado en BD"));
        EstadoAsistencia estadoAusente = estadoAsistenciaRepository.findByEstadoAsistencia("AUSENTE")
                .orElseThrow(() -> new RuntimeException("Estado AUSENTE no encontrado en BD"));
        EstadoAsistencia estadoTarde = estadoAsistenciaRepository.findByEstadoAsistencia("TARDE")
                .orElseThrow(() -> new RuntimeException("Estado TARDE no encontrado en BD"));
        EstadoAsistencia estadoJustificada = estadoAsistenciaRepository.findByEstadoAsistencia("JUSTIFICADA")
                .orElseThrow(() -> new RuntimeException("Estado JUSTIFICADA no encontrado en BD"));

        List<Inscripcion> inscripciones = inscripcionRepository.findByGrupoIdGrupo(clase.getGrupo().getIdGrupo());

        for (Inscripcion inscripcion : inscripciones) {
            Long idEstudiante = inscripcion.getMatricula().getEstudiante().getIdEstudiante();

            Asistencia asistencia = asistenciaRepository
                    .findByClaseIdClaseAndInscripcionIdInscripcion(idClase, inscripcion.getIdInscripcion())
                    .orElseGet(() -> {
                        Asistencia nueva = new Asistencia();
                        nueva.setClase(clase);
                        nueva.setInscripcion(inscripcion);
                        return nueva;
                    });

            if (estudiantesPresentesMap.containsKey(idEstudiante)) {
                asistencia.setEstadoAsistencia(estadoPresente);
                asistencia.setFechaRegistro(estudiantesPresentesMap.get(idEstudiante));
            } else {
                // SOLUCIÓN: Buscar por idInscripcion y proteger contra NullPointerException
                String estadoManual = manuales != null ? manuales.stream()
                        .filter(m -> m.getIdInscripcion() != null && m.getIdInscripcion().equals(inscripcion.getIdInscripcion()))
                        .map(FinalizarAsistenciaDTO.ActualizacionManualDTO::getEstado)
                        .findFirst()
                        .orElse("AUSENTE") : "AUSENTE";

                switch (estadoManual) {
                    case "TARDE":
                        asistencia.setEstadoAsistencia(estadoTarde);
                        break;
                    case "JUSTIFICADA":
                        asistencia.setEstadoAsistencia(estadoJustificada);
                        break;
                    default:
                        asistencia.setEstadoAsistencia(estadoAusente);
                        break;
                }
                asistencia.setFechaRegistro(LocalDateTime.now());
            }

            asistenciaRepository.save(asistencia);
        }
    }
    @Transactional(readOnly = true)
    public List<Asistencia> obtenerHistorialAsistencias(Long idGrupo) {
        return asistenciaRepository.findByGrupoIdGrupo(idGrupo);
    }

    @Transactional(readOnly = true)
    public List<Asistencia> obtenerHistorialPorGrupoYFecha(Long idGrupo, LocalDate fecha) {
        return asistenciaRepository.findByGrupoIdGrupoAndFecha(idGrupo, fecha);
    }

    @Transactional(readOnly = true)
    public EstadisticasAsistenciaDTO obtenerEstadisticasEstudiante(Long idGrupo, Long idEstudiante) {
        List<Asistencia> asistencias = asistenciaRepository.findByGrupoAndEstudiante(idGrupo, idEstudiante);

        long totalClases = asistencias.size();
        long presentes = asistencias.stream().filter(a -> "PRESENTE".equals(a.getEstadoAsistencia().getEstadoAsistencia())).count();
        long tardes = asistencias.stream().filter(a -> "TARDE".equals(a.getEstadoAsistencia().getEstadoAsistencia())).count();
        long ausentes = asistencias.stream().filter(a -> "AUSENTE".equals(a.getEstadoAsistencia().getEstadoAsistencia())).count();

        double porcentajeAsistencia = totalClases > 0 ? (presentes * 100.0) / totalClases : 0;

        return new EstadisticasAsistenciaDTO(totalClases, presentes, tardes, ausentes, porcentajeAsistencia);
    }

    @Transactional(readOnly = true)
    public boolean estudiantePerteneceAClase(Long idClase, Long idEstudiante) {
        Clase clase = claseRepository.findById(idClase)
                .orElseThrow(() -> new RuntimeException("Clase no encontrada"));

        // Traemos todos los inscritos de ese grupo
        List<Inscripcion> inscripciones = inscripcionRepository.findByGrupoIdGrupo(clase.getGrupo().getIdGrupo());

        // Verificamos si el ID del estudiante que escanea está en la lista oficial
        return inscripciones.stream()
                .anyMatch(i -> i.getMatricula().getEstudiante().getIdEstudiante().equals(idEstudiante));
    }

    @Transactional
    public void actualizarEstadoAsistencia(Long idAsistencia, String nuevoEstado) {
        Asistencia asistencia = asistenciaRepository.findById(idAsistencia)
                .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado"));

        EstadoAsistencia estado = estadoAsistenciaRepository.findByEstadoAsistencia(nuevoEstado.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Estado no válido en la BD"));

        asistencia.setEstadoAsistencia(estado);
        asistencia.setFechaRegistro(LocalDateTime.now()); // Actualiza la hora a la llegada real
        asistenciaRepository.save(asistencia);
    }

}