package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.exceptions.GrupoException;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrupoService {

    // --- Repositorios existentes (Trabajo previo de consulta) ---
    private final InscripcionRepository inscripcionRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final CalificacionRepository calificacionRepository;

    // --- Repositorios requeridos para la apertura de secciones y validaciones ---
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;
    private final PlantillaHorarioRepository plantillaHorarioRepository;
    private final SedeRepository sedeRepository;
    private final DocenteRepository docenteRepository;
    private final CicloRepository cicloRepository;
    private final MateriaRepository materiaRepository;
    private final ModalidadRepository modalidadRepository;
    private final AulaRepository aulaRepository;
    private final EstadoGrupoRepository estadoGrupoRepository;

    // =========================================================================
    // MÉTODOS DE CONSULTA EXISTENTES
    // =========================================================================

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
                    ev.getFechaFin(),
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

    // =========================================================================
    // LÓGICA DE NEGOCIO: APERTURA DE SECCIÓN POR PLANTILLA INSTITUCIONAL
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AulaResponseDTO> obtenerAulasPorSede(Long idSede) {
        return aulaRepository.findAulasDisponiblesPorSede(idSede).stream()
                .map(a -> new AulaResponseDTO(
                        a.getIdAula(),
                        a.getCodigoAula(),
                        a.getEdificio() != null ? a.getEdificio().getNombreEdificio() : "Sin Edificio",
                        a.getCapacidad()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MateriaPensumResponseDTO> obtenerMateriasPorCarrera(Long idCarrera) {
        return materiaRepository.findMateriasActivasPorCarrera(idCarrera);
    }

    @Transactional(readOnly = true)
    public List<DocenteSeleccionDTO> obtenerDocentesParaSeleccion() {
        return docenteRepository.findDocentesParaSeleccion();
    }

    @Transactional(readOnly = true)
    public List<PlantillaHorarioResponseDTO> obtenerPlantillasActivas() {
        return plantillaHorarioRepository.findAllActivasConDetalles().stream()
                .map(p -> {
                    List<PlantillaHorarioResponseDTO.DetallePlantillaDTO> detalles = p.getDetalles().stream()
                            .sorted(Comparator.comparing(d -> d.getDia().getIdDia()))
                            .map(d -> new PlantillaHorarioResponseDTO.DetallePlantillaDTO(
                                    d.getDia().getDia(),
                                    d.getHoraInicio(),
                                    d.getHoraFin()
                            ))
                            .toList();

                    return new PlantillaHorarioResponseDTO(
                            p.getIdPlantilla(),
                            p.getCodigoPlantilla(),
                            p.getDescripcion(),
                            detalles
                    );
                })
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public SeccionResponseDTO aperturarSeccionPorPlantilla(AperturaSeccionRequestDTO request) {

        // 1. Verificación de existencia de catálogos base
        Ciclo ciclo = cicloRepository.findById(request.getIdCiclo())
                .orElseThrow(() -> GrupoException.noEncontrado("El ciclo lectivo especificado no existe."));

        Sede sede = sedeRepository.findById(request.getIdSede())
                .orElseThrow(() -> GrupoException.noEncontrado("La sede especificada no existe."));

        Materia materia = materiaRepository.findById(request.getIdMateria())
                .orElseThrow(() -> GrupoException.noEncontrado("La materia especificada no existe."));

        if (!Boolean.TRUE.equals(materia.getEstadoMateria())) {
            throw GrupoException.reglaNegocio("La materia seleccionada no está activa en el pensum.");
        }

        Docente docente = docenteRepository.findByIdConContratacion(request.getIdDocente())
                .orElseThrow(() -> GrupoException.noEncontrado("El docente especificado no existe."));

        Modalidad modalidad = modalidadRepository.findById(request.getIdModalidad())
                .orElseThrow(() -> GrupoException.noEncontrado("La modalidad especificada no existe."));

        PlantillaHorario plantilla = plantillaHorarioRepository.findByIdConDetalles(request.getIdPlantilla())
                .orElseThrow(() -> GrupoException.noEncontrado("La plantilla horaria seleccionada no existe o no está activa."));

        // Verificación defensiva explícita de plantilla activa
        if (!Boolean.TRUE.equals(plantilla.getActiva()) || plantilla.getDetalles().isEmpty()) {
            throw GrupoException.reglaNegocio("La plantilla horaria no está habilitada o no tiene bloques configurados.");
        }

        // 2. Control preliminar en Java (falla rápido antes de tocar la BD)
        boolean existeGrupo = grupoRepository.existsByCiclo_IdCicloAndMateria_IdMateriaAndSede_IdSedeAndCodigoGrupoIgnoreCase(
                ciclo.getIdCiclo(), materia.getIdMateria(), sede.getIdSede(), plantilla.getCodigoPlantilla()
        );
        if (existeGrupo) {
            throw GrupoException.reglaNegocio("Ya existe una sección con el código '" + plantilla.getCodigoPlantilla() +
                    "' aperturada para esta materia en el ciclo actual.");
        }

        // 3. Validación de límite de contratación docente
        long materiasAsignadas = grupoRepository.countGruposActivosPorDocenteYCiclo(docente.getIdDocente(), ciclo.getIdCiclo());
        int maximoPermitido = docente.getTipoContratacion().getMaximoMaterias();
        if (materiasAsignadas >= maximoPermitido) {
            throw GrupoException.reglaNegocio("El docente ha alcanzado su límite de contratación (" +
                    materiasAsignadas + "/" + maximoPermitido + " materias).");
        }

        // 4. Validación de modalidad y recursos (Switch exhaustivo)
        Aula aulaFisica = null;
        Integer cupoFinal;
        String modalidadKey = modalidad.getModalidad().toUpperCase().trim();

        switch (modalidadKey) {
            case "PRESENCIAL", "SEMIPRESENCIAL" -> {
                if (request.getIdAula() == null) {
                    throw GrupoException.reglaNegocio("Debe asignar un aula física para modalidades presenciales o semipresenciales.");
                }
                aulaFisica = aulaRepository.findById(request.getIdAula())
                        .orElseThrow(() -> GrupoException.noEncontrado("El aula física especificada no existe."));

                // Validación territorial: el aula debe pertenecer a la misma sede
                if (!aulaFisica.getEdificio().getSede().getIdSede().equals(sede.getIdSede())) {
                    throw GrupoException.reglaNegocio("El aula física seleccionada no pertenece al campus de la sede indicada.");
                }

                cupoFinal = aulaFisica.getCapacidad();
            }
            case "VIRTUAL" -> {
                if (request.getEnlaceVirtual() == null || request.getEnlaceVirtual().isBlank()) {
                    throw GrupoException.reglaNegocio("Debe ingresar la URL de la sesión para la modalidad virtual.");
                }
                if (request.getCupoVirtual() == null || request.getCupoVirtual() <= 0) {
                    throw GrupoException.reglaNegocio("Debe definir un cupo estimado mayor a cero para la modalidad virtual.");
                }
                cupoFinal = request.getCupoVirtual();
            }
            default -> throw GrupoException.reglaNegocio("Modalidad '" + modalidad.getModalidad() + "' no soportada para apertura directa.");
        }

        // 5. Verificación de traslapes en cada bloque temporal de la plantilla
        for (PlantillaHorarioDetalle bloque : plantilla.getDetalles()) {
            boolean docenteOcupado = horarioRepository.existeTraslapeDocente(
                    ciclo.getIdCiclo(), docente.getIdDocente(), bloque.getDia().getIdDia(),
                    bloque.getHoraInicio(), bloque.getHoraFin()
            );
            if (docenteOcupado) {
                throw GrupoException.conflicto("El docente " + docente.getPersona().getNombres() + " " + docente.getPersona().getApellidos() +
                        " ya tiene una clase asignada el día " + bloque.getDia().getDia() +
                        " entre " + bloque.getHoraInicio() + " y " + bloque.getHoraFin() + ".");
            }

            if (aulaFisica != null) {
                boolean aulaOcupada = horarioRepository.existeTraslapeAula(
                        ciclo.getIdCiclo(), aulaFisica.getIdAula(), bloque.getDia().getIdDia(),
                        bloque.getHoraInicio(), bloque.getHoraFin()
                );
                if (aulaOcupada) {
                    throw GrupoException.conflicto("El aula " + aulaFisica.getCodigoAula() +
                            " ya se encuentra ocupada el día " + bloque.getDia().getDia() +
                            " entre " + bloque.getHoraInicio() + " y " + bloque.getHoraFin() + ".");
                }
            }
        }

        // 6. Inserción de cabecera con persistencia explícita del cupo máximo
        EstadoGrupo estadoAbierto = estadoGrupoRepository.findByEstadoGrupoIgnoreCase("ABIERTO")
                .orElseThrow(() -> GrupoException.noEncontrado("El estado de grupo 'ABIERTO' no está configurado en el sistema."));

        Grupo grupo = Grupo.builder()
                .ciclo(ciclo)
                .sede(sede)
                .materia(materia)
                .docente(docente)
                .estadoGrupo(estadoAbierto)
                .codigoGrupo(plantilla.getCodigoPlantilla())
                .plantilla(plantilla)
                .cupoMaximo(cupoFinal) // <-- Persistencia garantizada
                .build();

        Grupo grupoPersistido = grupoRepository.save(grupo);

        // 7. Inserción de detalle de horarios clonando la plantilla
        List<FranjaHorariaResponseDTO> franjasResponse = new ArrayList<>();

        for (PlantillaHorarioDetalle bloque : plantilla.getDetalles()) {
            Horario horario = Horario.builder()
                    .grupo(grupoPersistido)
                    .dia(bloque.getDia())
                    .modalidad(modalidad)
                    .aula(aulaFisica)
                    .horaInicio(bloque.getHoraInicio())
                    .horaFin(bloque.getHoraFin())
                    .enlaceVirtual(aulaFisica == null ? request.getEnlaceVirtual() : null)
                    .build();

            Horario horarioPersistido = horarioRepository.save(horario);

            franjasResponse.add(FranjaHorariaResponseDTO.builder()
                    .idHorario(horarioPersistido.getIdHorario())
                    .dia(bloque.getDia().getDia())
                    .horaInicio(horarioPersistido.getHoraInicio())
                    .horaFin(horarioPersistido.getHoraFin())
                    .modalidad(modalidad.getModalidad())
                    .aula(aulaFisica != null ? aulaFisica.getCodigoAula() : "Virtual")
                    .enlaceVirtual(horarioPersistido.getEnlaceVirtual())
                    .build());
        }

        return SeccionResponseDTO.builder()
                .idGrupo(grupoPersistido.getIdGrupo())
                .codigoGrupo(grupoPersistido.getCodigoGrupo())
                .materia(materia.getNombreMateria())
                .docente(docente.getPersona().getNombres() + " " + docente.getPersona().getApellidos())
                .sede(sede.getNombreSede())
                .estado(estadoAbierto.getEstadoGrupo())
                .cupoMaximo(cupoFinal)
                .horarios(franjasResponse)
                .build();
    }
}