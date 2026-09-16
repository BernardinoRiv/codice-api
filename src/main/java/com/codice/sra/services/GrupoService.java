package com.codice.sra.services;

import com.codice.sra.dtos.CalificacionResponseDTO;
import com.codice.sra.dtos.EvaluacionResponseDTO;
import com.codice.sra.dtos.InscripcionResponseDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GrupoService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private CalificacionRepository calificacionRepository;

    public List<InscripcionResponseDTO> obtenerInscripcionesConEstudiantes(Long idGrupo) {
        return inscripcionRepository.findByGrupoIdGrupo(idGrupo).stream().map(inscripcion -> {
            Estudiante estudiante = inscripcion.getMatricula().getEstudiante();
            Persona persona = estudiante.getPersona();
            return new InscripcionResponseDTO(
                    inscripcion.getIdInscripcion(),
                    estudiante.getCarnet(),
                    persona.getNombres(),
                    persona.getApellidos()
            );
        }).collect(Collectors.toList());
    }

    public List<EvaluacionResponseDTO> obtenerEvaluacionesPorGrupo(Long idGrupo) {
        return evaluacionRepository.findByGrupoIdGrupo(idGrupo).stream().map(ev -> {
            Integer periodo = (ev.getPeriodoEvaluacion() != null) ? ev.getPeriodoEvaluacion().getPeriodo() : null;
            return new EvaluacionResponseDTO(
                    ev.getIdEvaluacion(),
                    ev.getTipoEvaluacion().getTipoEvaluacion(),
                    ev.getNumeroEvaluacion(),
                    ev.getFechaInicio(),
                    periodo
            );
        }).collect(Collectors.toList());
    }

    public List<CalificacionResponseDTO> obtenerCalificacionesPorGrupo(Long idGrupo) {
        return calificacionRepository.findByEvaluacionGrupoIdGrupo(idGrupo).stream().map(calif -> {
            return new CalificacionResponseDTO(
                    calif.getIdCalificacion(),
                    calif.getInscripcion().getIdInscripcion(),
                    calif.getEvaluacion().getIdEvaluacion(),
                    calif.getNota(),
                    calif.getEstadoCalificacion().getEstadoCalificacion(),
                    calif.getFechaRegistro(),
                    calif.getFechaPublicacion(),
                    calif.getFechaModificacion()
            );
        }).collect(Collectors.toList());
    }
}