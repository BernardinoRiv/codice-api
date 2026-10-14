package com.codice.sra.services;

import com.codice.sra.models.Calificacion;
import com.codice.sra.models.EstadoCalificacion;
import com.codice.sra.models.Evaluacion;
import com.codice.sra.repositories.CalificacionRepository;
import com.codice.sra.repositories.EstadoCalificacionRepository;
import com.codice.sra.repositories.EvaluacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EvaluacionService {

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private CalificacionRepository calificacionRepository;

    @Autowired
    private EstadoCalificacionRepository estadoCalificacionRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public void publicarEvaluacion(Long idEvaluacion) {
        Evaluacion evaluacion = evaluacionRepository.findById(idEvaluacion)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));

        EstadoCalificacion estadoPublicada = estadoCalificacionRepository.findByEstadoCalificacion("PUBLICADA")
                .orElseThrow(() -> new RuntimeException("Estado PUBLICADA no encontrado"));

        List<Calificacion> calificaciones = calificacionRepository.findByEvaluacionIdEvaluacion(idEvaluacion);

        LocalDateTime ahora = LocalDateTime.now();

        for (Calificacion calificacion : calificaciones) {
            calificacion.setEstadoCalificacion(estadoPublicada);
            calificacion.setFechaPublicacion(ahora);

            String correo = calificacion.getInscripcion().getMatricula().getEstudiante().getPersona().getCorreoPersonal();

            if (correo != null && !correo.trim().isEmpty()) {
                String nombreEstudiante = calificacion.getInscripcion().getMatricula().getEstudiante().getPersona().getNombres();
                String materia = evaluacion.getGrupo().getMateria().getNombreMateria();
                String nombreEval = evaluacion.getTipoEvaluacion().getTipoEvaluacion() + " " + evaluacion.getNumeroEvaluacion();
                String notaDura = calificacion.getNota() != null ? calificacion.getNota().toString() : "0.0";
                emailService.enviarNotificacionNota(correo, nombreEstudiante, materia, nombreEval, notaDura);
            }
        }

        calificacionRepository.saveAll(calificaciones);
    }
}