package com.codice.sra.schedulers;

import com.codice.sra.models.Calificacion;
import com.codice.sra.models.Evaluacion;
import com.codice.sra.repositories.CalificacionRepository;
import com.codice.sra.repositories.EvaluacionRepository;
import com.codice.sra.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocenteNotificacionesScheduler {

    private final EvaluacionRepository evaluacionRepository;
    private final CalificacionRepository calificacionRepository;
    private final EmailService emailService;

    /**
     * Se ejecuta todos los días a las 07:00 AM
     */
    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional(readOnly = true)
    public void alertarDocentesCierreActas() {
        log.info("Iniciando escaneo de evaluaciones próximas a vencer (3 días)...");

        LocalDate fechaTarget = LocalDate.now().plusDays(3);
        List<Evaluacion> evaluacionesPorVencer = evaluacionRepository.findEvaluacionesPorVencerExactamente(fechaTarget);

        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        int alertasEnviadas = 0;

        for (Evaluacion evaluacion : evaluacionesPorVencer) {
            // Buscar todas las calificaciones de esta evaluación que aún estén en BORRADOR
            List<Calificacion> borradores = calificacionRepository.findByEvaluacionAndEstado(evaluacion, "BORRADOR");

            if (!borradores.isEmpty()) {
                String correoDocente = evaluacion.getGrupo().getDocente().getPersona().getCorreoPersonal();

                if (correoDocente != null && !correoDocente.trim().isEmpty()) {
                    String nombreDocente = evaluacion.getGrupo().getDocente().getPersona().getNombres() + " " + evaluacion.getGrupo().getDocente().getPersona().getApellidos();
                    String materia = evaluacion.getGrupo().getMateria().getNombreMateria();
                    String nombreEvaluacion = evaluacion.getTipoEvaluacion().getTipoEvaluacion() + " " + evaluacion.getNumeroEvaluacion();
                    String fechaLimiteStr = evaluacion.getFechaFin().format(formatoFecha);

                    emailService.enviarRecordatorioDocenteNotas(
                            correoDocente,
                            nombreDocente,
                            materia,
                            nombreEvaluacion,
                            fechaLimiteStr,
                            borradores.size()
                    );
                    alertasEnviadas++;
                }
            }
        }

        log.info("Proceso finalizado. Se enviaron {} alertas a docentes por notas pendientes.", alertasEnviadas);
    }
}