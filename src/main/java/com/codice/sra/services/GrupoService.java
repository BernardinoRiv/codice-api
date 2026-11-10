package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.exceptions.GrupoException;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrupoService {

    private final InscripcionRepository inscripcionRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final CalificacionRepository calificacionRepository;
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
    private final JwtService jwtService;

    public List<InscripcionResponseDTO> obtenerInscripcionesConEstudiantes(Long idGrupo) {
        return inscripcionRepository.findByGrupoIdGrupo(idGrupo).stream()
                .filter(inscripcion -> inscripcion.getEstadoMateria() == null ||
                        !inscripcion.getEstadoMateria().getEstadoMateria().equalsIgnoreCase("RETIRADA"))
                .map(inscripcion -> {
                    Estudiante estudiante = inscripcion.getMatricula().getEstudiante();
                    Persona persona = estudiante.getPersona();

                    String estadoMateria = (inscripcion.getEstadoMateria() != null)
                            ? inscripcion.getEstadoMateria().getEstadoMateria()
                            : "CURSANDO";

                    // SOLUCIÓN: Extraemos correctamente el ID y lo pasamos en orden
                    return new InscripcionResponseDTO(
                            inscripcion.getIdInscripcion(),
                            estudiante.getCarnet(),
                            persona.getNombres(),
                            persona.getApellidos(),
                            estadoMateria,
                            estudiante.getIdEstudiante() // <-- AQUÍ PASAMOS EL ID
                    );
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResumenCarreraOfertaDTO> obtenerConteoPorCarrera(Long idCiclo) {
        return grupoRepository.contarSeccionesPorCarreraEnCiclo(idCiclo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminarGrupo(Long idGrupo) {
        Grupo grupo = grupoRepository.findById(idGrupo)
                .orElseThrow(() -> GrupoException.noEncontrado("La sección especificada no existe."));

        String estadoCiclo = grupo.getCiclo().getEstadoCiclo().getEstadoCiclo().trim().toUpperCase();
        if (!"PLANIFICACION".equals(estadoCiclo)) {
            throw GrupoException.reglaNegocio(String.format(
                    "Operación denegada: No se puede eliminar la sección '%s' porque su ciclo ('%s') tiene estado '%s'.",
                    grupo.getCodigoGrupo(), grupo.getCiclo().getCodigoCiclo(), estadoCiclo));
        }

        if (inscripcionRepository.existsByGrupoIdGrupo(idGrupo)) {
            throw GrupoException.reglaNegocio("No es posible eliminar la sección porque ya cuenta con alumnos inscritos.");
        }

        horarioRepository.deleteByGrupo_IdGrupo(idGrupo);
        grupoRepository.delete(grupo);
        log.info("Sección [{}] eliminada satisfactoriamente del ciclo [{}]",
                grupo.getCodigoGrupo(), grupo.getCiclo().getCodigoCiclo());
    }

    @Transactional(readOnly = true)
    public List<GrupoDetalleResponseDTO> listarGruposPorCiclo(Long idCiclo) {
        return grupoRepository.findByCiclo_IdCiclo(idCiclo).stream()
                .map(g -> {
                    List<FranjaHorariaResponseDTO> franjas = horarioRepository.findByGrupoIdGrupo(g.getIdGrupo()).stream()
                            .map(h -> FranjaHorariaResponseDTO.builder()
                                    .idHorario(h.getIdHorario())
                                    .dia(h.getDia().getDia())
                                    .horaInicio(h.getHoraInicio())
                                    .horaFin(h.getHoraFin())
                                    .modalidad(h.getModalidad().getModalidad())
                                    .aula(h.getAula() != null ? h.getAula().getCodigoAula() : "Virtual")
                                    .enlaceVirtual(h.getEnlaceVirtual())
                                    .build())
                            .toList();

                    String espacio = !franjas.isEmpty() && franjas.get(0).getAula() != null
                            ? franjas.get(0).getAula()
                            : (!franjas.isEmpty() ? franjas.get(0).getEnlaceVirtual() : "N/A");

                    String carreraNombre = materiaRepository.findCarrerasVigentesPorMateria(g.getMateria().getIdMateria())
                            .stream()
                            .findFirst()
                            .orElse("Carrera General");

                    return new GrupoDetalleResponseDTO(
                            g.getIdGrupo(),
                            g.getCodigoGrupo(),
                            g.getMateria().getIdMateria(),
                            g.getMateria().getCodigoMateria(),
                            g.getMateria().getNombreMateria(),
                            carreraNombre,
                            g.getDocente().getIdDocente(),
                            g.getDocente().getPersona().getNombres() + " " + g.getDocente().getPersona().getApellidos(),
                            g.getSede().getIdSede(),
                            g.getSede().getNombreSede(),
                            !franjas.isEmpty() ? franjas.get(0).getModalidad() : "PRESENCIAL",
                            espacio,
                            g.getCupoMaximo(),
                            g.getEstadoGrupo().getEstadoGrupo(),
                            franjas
                    );
                })
                .toList();
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
    public List<DocenteSeleccionDTO> obtenerDocentesParaSeleccion(Long idSede) {
        if (idSede != null && idSede > 0) {
            return docenteRepository.findDocentesParaSeleccionPorSede(idSede);
        }
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

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest httpRequest = attrs.getRequest();
            String authHeader = httpRequest.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                String rol = jwtService.extractRol(token);
                Long idSedeToken = jwtService.extractIdSede(token);

                if (!"ADMINISTRADOR".equalsIgnoreCase(rol)) {
                    if (idSedeToken == null) {
                        throw GrupoException.reglaNegocio("El usuario no tiene una sede operativa asignada.");
                    }
                    if (!idSedeToken.equals(request.getIdSede())) {
                        throw GrupoException.reglaNegocio(String.format(
                                "Operación denegada: Solo tiene autorización para aperturar en la sede ID %d.",
                                idSedeToken
                        ));
                    }
                }
            }
        }

        Ciclo ciclo = cicloRepository.findById(request.getIdCiclo())
                .orElseThrow(() -> GrupoException.noEncontrado("El ciclo lectivo especificado no existe."));

        String estadoCiclo = ciclo.getEstadoCiclo().getEstadoCiclo().trim().toUpperCase();

        if (!"PLANIFICACION".equals(estadoCiclo)) {
            throw GrupoException.reglaNegocio(String.format(
                    "Operación rechazada: No se pueden aperturar secciones en el ciclo '%s' porque su estado es '%s'. " +
                            "La oferta académica únicamente puede aperturarse sobre ciclos en estado 'PLANIFICACION'.",
                    ciclo.getCodigoCiclo(), estadoCiclo
            ));
        }

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

        if (!Boolean.TRUE.equals(plantilla.getActiva()) || plantilla.getDetalles().isEmpty()) {
            throw GrupoException.reglaNegocio("La plantilla horaria no está habilitada o no tiene bloques configurados.");
        }

        boolean existeGrupo = grupoRepository.existsByCiclo_IdCicloAndMateria_IdMateriaAndSede_IdSedeAndCodigoGrupoIgnoreCase(
                ciclo.getIdCiclo(), materia.getIdMateria(), sede.getIdSede(), plantilla.getCodigoPlantilla()
        );
        if (existeGrupo) {
            throw GrupoException.reglaNegocio("Ya existe una sección con el código '" + plantilla.getCodigoPlantilla() +
                    "' aperturada para esta materia en el ciclo actual.");
        }

        long materiasAsignadas = grupoRepository.countGruposActivosPorDocenteYCiclo(docente.getIdDocente(), ciclo.getIdCiclo());
        int maximoPermitido = docente.getTipoContratacion().getMaximoMaterias();
        if (materiasAsignadas >= maximoPermitido) {
            throw GrupoException.reglaNegocio("El docente ha alcanzado su límite de contratación (" +
                    materiasAsignadas + "/" + maximoPermitido + " materias).");
        }

        Aula aulaFisica = null;
        Integer cupoFinal;
        String modalidadKey = modalidad.getModalidad().toUpperCase().trim();

        switch (modalidadKey) {
            case "PRESENCIAL", "HIBRIDA" -> {
                if (request.getIdAula() == null) {
                    throw GrupoException.reglaNegocio("Debe asignar un aula física para modalidades presenciales o semipresenciales.");
                }
                aulaFisica = aulaRepository.findById(request.getIdAula())
                        .orElseThrow(() -> GrupoException.noEncontrado("El aula física especificada no existe."));

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
                .cupoMaximo(cupoFinal)
                .build();

        Grupo grupoPersistido = grupoRepository.save(grupo);

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

    @Transactional(rollbackFor = Exception.class)
    public SeccionResponseDTO editarGrupo(Long idGrupo, EditarGrupoRequestDTO request) {

        Grupo grupo = grupoRepository.findById(idGrupo)
                .orElseThrow(() -> GrupoException.noEncontrado("La sección especificada no existe."));

        String estadoCiclo = grupo.getCiclo().getEstadoCiclo().getEstadoCiclo().trim().toUpperCase();
        if (!"PLANIFICACION".equals(estadoCiclo)) {
            throw GrupoException.reglaNegocio(String.format(
                    "Operación denegada: La sección pertenece al ciclo '%s' con estado '%s'. Solo los ciclos en PLANIFICACIÓN admiten edición.",
                    grupo.getCiclo().getCodigoCiclo(), estadoCiclo));
        }

        Docente nuevoDocente = docenteRepository.findByIdConContratacion(request.idDocente())
                .orElseThrow(() -> GrupoException.noEncontrado("El docente especificado no existe o no está activo."));

        boolean esCambioDocente = !grupo.getDocente().getIdDocente().equals(nuevoDocente.getIdDocente());
        if (esCambioDocente) {
            long gruposAsignados = grupoRepository.countGruposActivosPorDocenteYCiclo(
                    nuevoDocente.getIdDocente(), grupo.getCiclo().getIdCiclo());
            int maximoPermitido = nuevoDocente.getTipoContratacion().getMaximoMaterias();

            if (gruposAsignados >= maximoPermitido) {
                throw GrupoException.reglaNegocio(String.format(
                        "El docente %s %s ha alcanzado su límite de contratación (%d/%d materias).",
                        nuevoDocente.getPersona().getNombres(), nuevoDocente.getPersona().getApellidos(),
                        gruposAsignados, maximoPermitido));
            }
        }

        List<Horario> horarios = horarioRepository.findByGrupoIdGrupo(idGrupo);
        if (horarios.isEmpty()) {
            throw GrupoException.reglaNegocio("La sección no posee franjas horarias configuradas.");
        }

        String modalidadKey = horarios.get(0).getModalidad().getModalidad().trim().toUpperCase();
        boolean esVirtual = modalidadKey.contains("VIRTUAL");

        Aula nuevaAula = null;
        Integer cupoFinal = request.cupoMaximo();

        if (esVirtual) {
            if (request.enlaceVirtual() == null || request.enlaceVirtual().isBlank()) {
                throw GrupoException.reglaNegocio("Debe ingresar la URL de la sesión para la modalidad virtual.");
            }
        } else {
            if (request.idAula() == null) {
                throw GrupoException.reglaNegocio("Debe asignar un aula física para modalidades presenciales o semipresenciales.");
            }
            nuevaAula = aulaRepository.findById(request.idAula())
                    .orElseThrow(() -> GrupoException.noEncontrado("El aula física especificada no existe."));

            if (!nuevaAula.getEdificio().getSede().getIdSede().equals(grupo.getSede().getIdSede())) {
                throw GrupoException.reglaNegocio("El aula física seleccionada no pertenece al campus de la sede de la sección.");
            }

            if (cupoFinal > nuevaAula.getCapacidad()) {
                throw GrupoException.reglaNegocio(String.format(
                        "El cupo solicitado (%d) excede la capacidad física del aula %s (%d asientos).",
                        cupoFinal, nuevaAula.getCodigoAula(), nuevaAula.getCapacidad()));
            }
        }

        Long idCiclo = grupo.getCiclo().getIdCiclo();

        for (Horario h : horarios) {
            Long idDia = h.getDia().getIdDia();
            LocalTime inicio = h.getHoraInicio();
            LocalTime fin = h.getHoraFin();

            if (esCambioDocente) {
                boolean choqueDocente = horarioRepository.existeTraslapeDocenteEnOtroGrupo(
                        idCiclo, nuevoDocente.getIdDocente(), idDia, idGrupo, inicio, fin);

                if (choqueDocente) {
                    throw GrupoException.conflicto(String.format(
                            "Conflicto de horario: El docente %s %s ya tiene una clase asignada el día %s entre %s y %s.",
                            nuevoDocente.getPersona().getNombres(), nuevoDocente.getPersona().getApellidos(),
                            h.getDia().getDia(), inicio, fin));
                }
            }

            if (nuevaAula != null) {
                boolean choqueAula = horarioRepository.existeTraslapeAulaEnOtroGrupo(
                        idCiclo, nuevaAula.getIdAula(), idDia, idGrupo, inicio, fin);

                if (choqueAula) {
                    throw GrupoException.conflicto(String.format(
                            "Conflicto de espacio: El aula %s ya está reservada el día %s entre %s y %s.",
                            nuevaAula.getCodigoAula(), h.getDia().getDia(), inicio, fin));
                }
            }

            h.setAula(nuevaAula);
            h.setEnlaceVirtual(esVirtual ? request.enlaceVirtual() : null);
        }

        grupo.setDocente(nuevoDocente);
        grupo.setCupoMaximo(cupoFinal);

        grupoRepository.save(grupo);
        horarioRepository.saveAll(horarios);

        return mapearASeccionResponseDTO(grupo, horarios);
    }

    private SeccionResponseDTO mapearASeccionResponseDTO(Grupo grupo, List<Horario> horarios) {
        List<FranjaHorariaResponseDTO> franjas = horarios.stream()
                .map(h -> FranjaHorariaResponseDTO.builder()
                        .idHorario(h.getIdHorario())
                        .dia(h.getDia().getDia())
                        .horaInicio(h.getHoraInicio())
                        .horaFin(h.getHoraFin())
                        .modalidad(h.getModalidad().getModalidad())
                        .aula(h.getAula() != null ? h.getAula().getCodigoAula() : "Virtual")
                        .enlaceVirtual(h.getEnlaceVirtual())
                        .build())
                .toList();

        return SeccionResponseDTO.builder()
                .idGrupo(grupo.getIdGrupo())
                .codigoGrupo(grupo.getCodigoGrupo())
                .materia(grupo.getMateria().getNombreMateria())
                .docente(grupo.getDocente().getPersona().getNombres() + " " + grupo.getDocente().getPersona().getApellidos())
                .sede(grupo.getSede().getNombreSede())
                .estado(grupo.getEstadoGrupo().getEstadoGrupo())
                .cupoMaximo(grupo.getCupoMaximo())
                .horarios(franjas)
                .build();
    }
}