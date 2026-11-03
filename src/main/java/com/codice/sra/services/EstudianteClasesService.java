package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EstudianteClasesService {

    private final InscripcionRepository inscripcionRepo;
    private final PensumMateriaRepository pensumMateriaRepo;
    private final HorarioRepository horarioRepo;
    private final CicloRepository cicloRepository;

    public EstudianteClasesService(InscripcionRepository inscripcionRepo,
                                   PensumMateriaRepository pensumMateriaRepo,
                                   HorarioRepository horarioRepo,
                                   CicloRepository cicloRepository) {
        this.inscripcionRepo = inscripcionRepo;
        this.pensumMateriaRepo = pensumMateriaRepo;
        this.horarioRepo = horarioRepo;
        this.cicloRepository = cicloRepository;
    }

    @Transactional(readOnly = true)
    public List<ClaseInscritaDTO> obtenerClasesCicloActivo(Long idEstudiante) {
        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("No hay ciclo activo"));

        // Obtenemos todas las inscripciones del estudiante para el ciclo activo actual.
        // Asegúrate de que este método en inscripcionRepo haga la búsqueda cruzando Estudiante.id = idEstudiante y Ciclo.id = idCiclo
        List<Inscripcion> inscripciones = inscripcionRepo.findByEstudianteAndCiclo(idEstudiante, cicloActivo.getIdCiclo());

        return inscripciones.stream().map(insc -> {
            ClaseInscritaDTO dto = new ClaseInscritaDTO();
            Grupo grupo = insc.getGrupo();
            Materia materia = grupo.getMateria();

            dto.setCodigoMateria(materia.getCodigoMateria());
            dto.setNombreMateria(materia.getNombreMateria());
            dto.setGrupo(grupo.getCodigoGrupo());
            dto.setUvs(materia.getUnidadesValorativas());
            dto.setNombreDocente(grupo.getDocente().getPersona().getNombres() + " " + grupo.getDocente().getPersona().getApellidos());

            List<Horario> horarios = horarioRepo.findByGrupo_IdGrupo(grupo.getIdGrupo());
            dto.setHorarios(horarios.stream().map(h -> {
                HorarioDTO hd = new HorarioDTO();
                hd.setDia(h.getDia().getDia());
                hd.setHoraInicio(h.getHoraInicio());
                hd.setHoraFin(h.getHoraFin());
                hd.setAula(h.getAula() != null ? h.getAula().getCodigoAula() : "Virtual");
                return hd;
            }).collect(Collectors.toList()));

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PensumEstudianteDTO obtenerPensum(Long idEstudiante) {
        List<Inscripcion> historial = inscripcionRepo.findAllByEstudianteId(idEstudiante);
        if (historial.isEmpty()) {
            throw new RuntimeException("El estudiante no tiene historial académico asignado.");
        }

        Long idPensum = historial.get(0).getMatricula().getEstudianteCarrera().getPensum().getIdPensum();
        List<PensumMateria> estructuraPensum = pensumMateriaRepo.findByPensumIdOrderByCicloRecomendado(idPensum);

        Ciclo cicloActivo = cicloRepository.findCicloActivo().orElse(null);
        Long idCicloActivo = (cicloActivo != null) ? cicloActivo.getIdCiclo() : -1L;

        // Agrupamos las inscripciones por el ID de la materia para facilitar la búsqueda
        Map<Long, List<Inscripcion>> inscripcionesPorMateria = historial.stream()
                .collect(Collectors.groupingBy(i -> i.getGrupo().getMateria().getIdMateria()));

        PensumEstudianteDTO respuesta = new PensumEstudianteDTO();
        Map<Integer, List<MateriaPensumDTO>> agrupadoPorCiclo = new HashMap<>();

        int aprobadas = 0;
        int cursando = 0;
        int pendientes = 0;

        for (PensumMateria pm : estructuraPensum) {
            MateriaPensumDTO dto = new MateriaPensumDTO();
            dto.setCodigo(pm.getMateria().getCodigoMateria());
            dto.setNombre(pm.getMateria().getNombreMateria());
            dto.setUvs(pm.getMateria().getUnidadesValorativas());
            dto.setCicloRecomendado(pm.getCicloRecomendado());

            // Buscamos si el estudiante tiene alguna inscripción (histórica o actual) para esta materia del pensum
            List<Inscripcion> inscripcionesMateria = inscripcionesPorMateria.getOrDefault(pm.getMateria().getIdMateria(), new ArrayList<>());

            if (inscripcionesMateria.isEmpty()) {
                // Si no hay ninguna inscripción, la materia está pendiente.
                dto.setEstado("PENDIENTE");
                pendientes++;
            } else {
                boolean estaAprobada = false;
                boolean estaCursando = false;

                // Revisamos todas las inscripciones que tiene el estudiante para esta materia
                for (Inscripcion insc : inscripcionesMateria) {

                    // Condición 1: Si la inscripción pertenece al ciclo activo (el actual), entonces la está cursando.
                    if (insc.getGrupo().getCiclo().getIdCiclo().equals(idCicloActivo)) {
                        estaCursando = true;
                    }

                    // Condición 2: Si el estado de la materia en esta inscripción es "APROBADA"
                    if (insc.getEstadoMateria() != null && "APROBADA".equalsIgnoreCase(insc.getEstadoMateria().getEstadoMateria())) {
                        estaAprobada = true;
                    }
                }

                // Jerarquía de estados: Aprobada tiene prioridad sobre cursando, y cursando sobre pendiente.
                if (estaAprobada) {
                    dto.setEstado("APROBADA");
                    aprobadas++;
                } else if (estaCursando) {
                    dto.setEstado("CURSANDO");
                    cursando++;
                } else {
                    // Si la cursó antes (REPROBADA, RETIRADA, etc.) y no la está cursando ahora ni la aprobó, sigue pendiente.
                    dto.setEstado("PENDIENTE");
                    pendientes++;
                }
            }
            agrupadoPorCiclo.computeIfAbsent(pm.getCicloRecomendado(), k -> new ArrayList<>()).add(dto);
        }

        respuesta.setMateriasPorCiclo(agrupadoPorCiclo);
        respuesta.setMateriasAprobadas(aprobadas);
        respuesta.setMateriasCursando(cursando);
        respuesta.setMateriasPendientes(pendientes);

        return respuesta;
    }
}