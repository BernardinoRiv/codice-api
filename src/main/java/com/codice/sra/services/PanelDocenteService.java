package com.codice.sra.services;

import com.codice.sra.dtos.DocenteDashboardResponseDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PanelDocenteService {

    private final DocenteRepository docenteRepository;
    private final CicloRepository cicloRepository;
    private final GrupoRepository grupoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final HorarioRepository horarioRepository;
    private final EvaluacionRepository evaluacionRepository;

    public PanelDocenteService(DocenteRepository docenteRepository,
                               CicloRepository cicloRepository,
                               GrupoRepository grupoRepository,
                               InscripcionRepository inscripcionRepository,
                               HorarioRepository horarioRepository,
                               EvaluacionRepository evaluacionRepository) {
        this.docenteRepository = docenteRepository;
        this.cicloRepository = cicloRepository;
        this.grupoRepository = grupoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.horarioRepository = horarioRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    @Transactional(readOnly = true)
    public DocenteDashboardResponseDTO obtenerPanelPrincipal(Long idUsuario) {
        Docente docente = docenteRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado para este usuario."));

        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("No hay un ciclo académico activo."));

        List<Grupo> gruposDocente = grupoRepository.findByDocenteIdDocenteAndCicloIdCiclo(docente.getIdDocente(), cicloActivo.getIdCiclo());

        DocenteDashboardResponseDTO dashboard = new DocenteDashboardResponseDTO();
        List<DocenteDashboardResponseDTO.GrupoAsignadoDTO> gruposDTO = new ArrayList<>();

        String diaActualStr = obtenerDiaActualEnEspanol();
        int contadorClasesHoy = 0;
        int contadorEvaluacionesActivas = 0;
        LocalDateTime ahora = LocalDateTime.now();

        LocalDateTime fechaMasCercana = null;
        Evaluacion proximaEvaluacion = null;

        for (Grupo grupo : gruposDocente) {
            DocenteDashboardResponseDTO.GrupoAsignadoDTO grupoDTO = new DocenteDashboardResponseDTO.GrupoAsignadoDTO();
            grupoDTO.setIdGrupo(grupo.getIdGrupo());
            grupoDTO.setCodigoGrupo(grupo.getCodigoGrupo());
            grupoDTO.setMateria(grupo.getMateria().getNombreMateria());

            long totalInscritos = inscripcionRepository.countByGrupoIdGrupo(grupo.getIdGrupo());
            grupoDTO.setInscritos(totalInscritos);

            List<Horario> horarios = horarioRepository.findByGrupoIdGrupo(grupo.getIdGrupo());
            List<DocenteDashboardResponseDTO.HorarioGrupoDTO> horariosDTO = new ArrayList<>();

            for (Horario h : horarios) {
                DocenteDashboardResponseDTO.HorarioGrupoDTO hdto = new DocenteDashboardResponseDTO.HorarioGrupoDTO();
                hdto.setDia(h.getDia().getDia());
                hdto.setHoraInicio(h.getHoraInicio().toString());
                hdto.setHoraFin(h.getHoraFin().toString());
                hdto.setAula(h.getAula() != null ? h.getAula().getCodigoAula() : "Virtual");
                horariosDTO.add(hdto);

                if (h.getDia().getDia().equalsIgnoreCase(diaActualStr)) {
                    contadorClasesHoy++;
                }
            }
            grupoDTO.setHorarios(horariosDTO);
            gruposDTO.add(grupoDTO);

            List<Evaluacion> evaluaciones = evaluacionRepository.findByGrupoIdGrupo(grupo.getIdGrupo());
            for (Evaluacion ev : evaluaciones) {
                if (!ahora.isBefore(ev.getFechaInicio()) && !ahora.isAfter(ev.getFechaFin())) {
                    contadorEvaluacionesActivas++;
                } else if (ahora.isBefore(ev.getFechaInicio())) {
                    // Buscar la evaluación futura más cercana
                    if (fechaMasCercana == null || ev.getFechaInicio().isBefore(fechaMasCercana)) {
                        fechaMasCercana = ev.getFechaInicio();
                        proximaEvaluacion = ev;
                    }
                }
            }
        }

        dashboard.setGruposAsignados(gruposDTO);
        dashboard.setClasesHoy(contadorClasesHoy);
        dashboard.setEvaluacionesPendientes(contadorEvaluacionesActivas);

        if (contadorEvaluacionesActivas == 0 && proximaEvaluacion != null) {
            String tipo = proximaEvaluacion.getTipoEvaluacion().getTipoEvaluacion();
            String titulo = (tipo.equals("LABORATORIO") ? "LAB" : "PARCIAL") + " " + proximaEvaluacion.getNumeroEvaluacion();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", new Locale("es", "ES"));

            dashboard.setProximaEvaluacionTitulo(titulo);
            dashboard.setProximaEvaluacionFecha(proximaEvaluacion.getFechaInicio().format(formatter));
        }

        return dashboard;
    }

    private String obtenerDiaActualEnEspanol() {
        return switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }
}