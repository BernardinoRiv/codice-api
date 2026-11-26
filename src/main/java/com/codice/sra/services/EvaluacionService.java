package com.codice.sra.services;

import com.codice.sra.models.Calificacion;
import com.codice.sra.models.EstadoCalificacion;
import com.codice.sra.models.Evaluacion;
import com.codice.sra.repositories.CalificacionRepository;
import com.codice.sra.repositories.EstadoCalificacionRepository;
import com.codice.sra.repositories.EvaluacionRepository;
import com.codice.sra.repositories.SolicitudCalificacionRepository;
import com.codice.sra.utils.SecurityUtils;
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

    @Autowired
    private SolicitudCalificacionRepository solicitudCalificacionRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private AuditoriaService auditoriaService;

    @Transactional
    public void publicarEvaluacion(Long idEvaluacion) {
        Evaluacion evaluacion = evaluacionRepository.findById(idEvaluacion)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));

        Long idUsuario = securityUtils.obtenerIdUsuarioAutenticado();
        if (evaluacion.getGrupo().getDocente().getUsuario() == null
                || !idUsuario.equals(evaluacion.getGrupo().getDocente().getUsuario().getIdUsuario()))
            throw new RuntimeException("No tiene autorización para publicar notas de este grupo.");

        EstadoCalificacion estadoPublicada = estadoCalificacionRepository.findByEstadoCalificacion("PUBLICADA")
                .orElseThrow(() -> new RuntimeException("Estado PUBLICADA no encontrado"));

        List<Calificacion> calificaciones = calificacionRepository.findByEvaluacionIdEvaluacion(idEvaluacion);

        LocalDateTime ahora = LocalDateTime.now();

        boolean enVentana = evaluacion.getFechaInicio() != null && evaluacion.getFechaFin() != null
                && !ahora.isBefore(evaluacion.getFechaInicio()) && !ahora.isAfter(evaluacion.getFechaFin());
        List<Calificacion> borradoresConNota = calificaciones.stream()
                .filter(c -> c.getNota() != null)
                .filter(c -> c.getEstadoCalificacion() == null
                        || !"PUBLICADA".equalsIgnoreCase(c.getEstadoCalificacion().getEstadoCalificacion()))
                .toList();

        List<Calificacion> elegibles = enVentana ? borradoresConNota : borradoresConNota.stream()
                .filter(c -> solicitudAprobadaVigente(c, ahora))
                .toList();
        if (!enVentana && elegibles.isEmpty())
            throw new IllegalStateException("No hay calificaciones en borrador con una solicitud aprobada vigente (48h) para publicar.");

        for (Calificacion calificacion : elegibles) {
            calificacion.setEstadoCalificacion(estadoPublicada);
            calificacion.setFechaPublicacion(ahora);

            if (!enVentana) {
                auditoriaService.registrarEvento("PUBLICACION_NOTA_SOLICITUD_APROBADA", "calificaciones",
                        calificacion.getIdCalificacion(), "Calificación publicada fuera de la ventana ordinaria mediante solicitud aprobada vigente.");
            }

            String correo = calificacion.getInscripcion().getMatricula().getEstudiante().getPersona().getCorreoPersonal();

            if (correo != null && !correo.trim().isEmpty()) {
                String nombreEstudiante = calificacion.getInscripcion().getMatricula().getEstudiante().getPersona().getNombres();
                String materia = evaluacion.getGrupo().getMateria().getNombreMateria();
                String nombreEval = evaluacion.getTipoEvaluacion().getTipoEvaluacion() + " " + evaluacion.getNumeroEvaluacion();
                String notaDura = calificacion.getNota() != null ? calificacion.getNota().toString() : "0.0";
                emailService.enviarNotificacionNota(correo, nombreEstudiante, materia, nombreEval, notaDura);
            }
        }

        calificacionRepository.saveAll(elegibles);
    }

    private boolean solicitudAprobadaVigente(Calificacion calificacion, LocalDateTime ahora) {
        return solicitudCalificacionRepository
                .findByCalificacion_IdCalificacionAndEstadoSolicitud_EstadoSolicitudIgnoreCaseOrderByFechaRespuestaDesc(
                        calificacion.getIdCalificacion(), "APROBADA")
                .stream()
                .anyMatch(s -> s.getFechaRespuesta() != null
                        && s.getFechaRespuesta().plusHours(48).isAfter(ahora)
                        && !("PUBLICADA".equalsIgnoreCase(calificacion.getEstadoCalificacion().getEstadoCalificacion())
                        && calificacion.getFechaPublicacion() != null
                        && !calificacion.getFechaPublicacion().isBefore(s.getFechaRespuesta())));
    }
}
