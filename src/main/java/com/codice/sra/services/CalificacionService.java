package com.codice.sra.services;

import com.codice.sra.dtos.CalificacionRegistroRequestDTO;
import com.codice.sra.dtos.CalificacionResponseDTO;
import com.codice.sra.models.Calificacion;
import com.codice.sra.models.EstadoCalificacion;
import com.codice.sra.models.Evaluacion;
import com.codice.sra.models.Inscripcion;
import com.codice.sra.repositories.CalificacionRepository;
import com.codice.sra.repositories.EstadoCalificacionRepository;
import com.codice.sra.repositories.EvaluacionRepository;
import com.codice.sra.repositories.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstadoCalificacionRepository estadoCalificacionRepository;

    public CalificacionService(
            CalificacionRepository calificacionRepository,
            EvaluacionRepository evaluacionRepository,
            InscripcionRepository inscripcionRepository,
            EstadoCalificacionRepository estadoCalificacionRepository) {
        this.calificacionRepository = calificacionRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.estadoCalificacionRepository = estadoCalificacionRepository;
    }

    @Transactional
    public CalificacionResponseDTO registrarCalificacion(CalificacionRegistroRequestDTO request, Long idUsuarioDocente) {

        validarNotaIndividual(request.getNota());

        Evaluacion evaluacion = evaluacionRepository.findById(request.getIdEvaluacion())
                .orElseThrow(() -> new RuntimeException("La evaluación no existe."));

        Inscripcion inscripcion = inscripcionRepository.findById(request.getIdInscripcion())
                .orElseThrow(() -> new RuntimeException("La inscripción no existe."));

        validarPropiedadGrupo(evaluacion, idUsuarioDocente);
        validarVentanaTiempo(evaluacion);

        EstadoCalificacion estadoBorrador = estadoCalificacionRepository.findByEstadoCalificacion("BORRADOR")
                .orElseThrow(() -> new RuntimeException("El estado BORRADOR no está configurado en la base de datos."));

        LocalDateTime ahora = LocalDateTime.now();
        Optional<Calificacion> calificacionExistente = calificacionRepository
                .findByInscripcionIdInscripcionAndEvaluacionIdEvaluacion(
                        request.getIdInscripcion(),
                        request.getIdEvaluacion()
                );

        Calificacion calificacion;

        if (calificacionExistente.isPresent()) {
            calificacion = calificacionExistente.get();
            calificacion.setNota(request.getNota());
            calificacion.setFechaModificacion(ahora);
            calificacion.setEstadoCalificacion(estadoBorrador);
        } else {
            calificacion = new Calificacion();
            calificacion.setInscripcion(inscripcion);
            calificacion.setEvaluacion(evaluacion);
            calificacion.setNota(request.getNota());
            calificacion.setEstadoCalificacion(estadoBorrador);
            calificacion.setFechaRegistro(ahora);
        }

        calificacion.setUsuario(evaluacion.getGrupo().getDocente().getUsuario());

        Calificacion calificacionGuardada = calificacionRepository.save(calificacion);

        return mapearAResponse(calificacionGuardada);
    }

    private void validarNotaIndividual(BigDecimal nota) {
        if (nota == null) {
            throw new IllegalArgumentException("La nota no puede ser nula.");
        }

        if (nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(new BigDecimal("10.00")) > 0) {
            throw new IllegalArgumentException("La nota debe estar en el rango de 0.00 a 10.00.");
        }

        if (nota.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("La nota no puede tener más de 2 decimales. Valor recibido: " + nota);
        }
    }

    private void validarPropiedadGrupo(Evaluacion evaluacion, Long idUsuarioDocente) {
        Long idUsuarioGrupo = evaluacion.getGrupo().getDocente().getUsuario().getIdUsuario();
        if (!idUsuarioGrupo.equals(idUsuarioDocente)) {
            throw new RuntimeException("El docente no tiene autorización para calificar este grupo.");
        }
    }

    private void validarVentanaTiempo(Evaluacion evaluacion) {
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(evaluacion.getFechaInicio())) {
            throw new RuntimeException("El período de calificación para esta evaluación aún no ha iniciado.");
        }
        if (ahora.isAfter(evaluacion.getFechaFin())) {
            throw new RuntimeException("El período de calificación para esta evaluación ha finalizado.");
        }
    }

    private CalificacionResponseDTO mapearAResponse(Calificacion calificacion) {
        CalificacionResponseDTO response = new CalificacionResponseDTO();
        response.setIdCalificacion(calificacion.getIdCalificacion());
        response.setIdInscripcion(calificacion.getInscripcion().getIdInscripcion());
        response.setIdEvaluacion(calificacion.getEvaluacion().getIdEvaluacion());
        response.setNota(calificacion.getNota());
        response.setEstadoCalificacion(calificacion.getEstadoCalificacion().getEstadoCalificacion());
        response.setFechaRegistro(calificacion.getFechaRegistro());
        response.setFechaPublicacion(calificacion.getFechaPublicacion());
        response.setFechaModificacion(calificacion.getFechaModificacion());
        return response;
    }
}