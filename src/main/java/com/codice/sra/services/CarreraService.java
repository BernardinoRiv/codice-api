package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.utils.CodigoCarreraUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarreraService {

    private final CarreraRepository carreraRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final FacultadRepository facultadRepository;
    private final EstadoCarreraRepository estadoCarreraRepository;
    private final AuditoriaService auditoriaService;
    private final SedeRepository sedeRepository;
    private final CarreraSedeRepository carreraSedeRepository;
    @Transactional(readOnly = true)
    public List<CarreraResponseDTO> listarCarrerasOfertables(Long idSede) {
        log.info("Ejecutando resolución de carreras ofertables para sede ID: [{}]", idSede);

        List<Carrera> carreras = (idSede != null && idSede > 0)
                ? carreraRepository.findCarrerasOfertablesPorSede(idSede)
                : carreraRepository.findAllCarrerasConPensumVigente();

        log.info("Total de carreras recuperadas para sede [{}]: {}", idSede, carreras.size());

        return carreras.stream()
                .map(this::mapearADTO)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Long crearCarrera(CarreraCreacionRequestDTO dto) {
        String nombreNormalizado = dto.getNombreCarrera().trim();

        // 1. Validar unicidad previa del nombre
        if (carreraRepository.existsByNombreCarreraIgnoreCase(nombreNormalizado)) {
            throw new IllegalArgumentException("El nombre de carrera '" + nombreNormalizado + "' ya se encuentra registrado");
        }

        // 2. Validar dependencias foráneas
        Facultad facultad = facultadRepository.findById(dto.getIdFacultad())
                .orElseThrow(() -> new IllegalArgumentException("Facultad no encontrada con ID: " + dto.getIdFacultad()));

        if (facultad.getEstadoFacultad() == null || facultad.getEstadoFacultad().getIdEstadoFacultad() != 1L) {
            throw new IllegalArgumentException("No se puede asociar una carrera a una facultad inactiva");
        }

        NivelAcademico nivel = nivelAcademicoRepository.findById(dto.getIdNivel())
                .orElseThrow(() -> new IllegalArgumentException("Nivel académico no encontrado con ID: " + dto.getIdNivel()));

        EstadoCarrera estadoInicial = estadoCarreraRepository.findByEstadoCarreraIgnoreCase("PLANIFICACION")
                .orElseThrow(() -> new IllegalStateException("El catálogo estados_carrera no contiene el estado 'PLANIFICACION'"));

        // 3. Validar sedes seleccionadas
        List<Long> idsSedes = dto.getIdsSedes();

        if (idsSedes == null || idsSedes.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una sede para la carrera");
        }

        if (idsSedes.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Los identificadores de las sedes deben ser números positivos");
        }

        Set<Long> idsUnicos = new HashSet<>(idsSedes);

        if (idsUnicos.size() != idsSedes.size()) {
            throw new IllegalArgumentException("No se permite seleccionar una misma sede más de una vez");
        }

        List<Sede> sedes = sedeRepository.findAllById(idsUnicos);

        if (sedes.size() != idsUnicos.size()) {
            Set<Long> idsEncontrados = sedes.stream().map(Sede::getIdSede).collect(Collectors.toSet());
            Set<Long> idsInexistentes = new HashSet<>(idsUnicos);
            idsInexistentes.removeAll(idsEncontrados);

            throw new IllegalArgumentException("Las siguientes sedes no existen: " + idsInexistentes);
        }

        for (Sede sede : sedes) {
            if (sede.getEstadoSede() == null || !Objects.equals(sede.getEstadoSede().getIdEstadoSede(), 1L)) {
                throw new IllegalArgumentException("La sede '" + sede.getNombreSede() + "' se encuentra inactiva");
            }
        }

        // 4. Generación automática del código
        String codigoGenerado = CodigoCarreraUtils.generarCodigoCarrera(
                nombreNormalizado,
                nivel.getIdNivel(),
                carreraRepository::existsByCodigoCarreraIgnoreCase
        );

        // 5. Crear y guardar carrera
        Carrera carrera = new Carrera();
        carrera.setCodigoCarrera(codigoGenerado);
        carrera.setNombreCarrera(nombreNormalizado);
        carrera.setFacultad(facultad);
        carrera.setNivel(nivel);
        carrera.setEstadoCarrera(estadoInicial);

        carreraRepository.save(carrera);

        // 6. Asociar carrera con las sedes seleccionadas
        List<CarreraSede> asociaciones = new ArrayList<>();

        for (Sede sede : sedes) {
            CarreraSede carreraSede = new CarreraSede();
            carreraSede.setCarrera(carrera);
            carreraSede.setSede(sede);
            asociaciones.add(carreraSede);
        }

        carreraSedeRepository.saveAll(asociaciones);

        // 7. Auditoría Forense obligatoria
        String sedesAsignadas = sedes.stream()
                .map(sede -> sede.getNombreSede() + " (ID: " + sede.getIdSede() + ")")
                .collect(Collectors.joining(", "));

        auditoriaService.registrarEvento(
                "CREACION_CARRERA",
                "carreras",
                carrera.getIdCarrera(),
                String.format("Alta de carrera: %s con código generado automático: %s. Sedes asignadas: %s",
                        nombreNormalizado, codigoGenerado, sedesAsignadas)
        );

        log.info("Carrera ID [{}] creada exitosamente con {} sedes asignadas.", carrera.getIdCarrera(), sedes.size());

        return carrera.getIdCarrera();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void actualizarCarrera(Long idCarrera, CarreraActualizacionRequestDTO dto) {
        if (idCarrera == null || idCarrera <= 0) throw new IllegalArgumentException("El ID de la carrera debe ser positivo");

        Carrera carrera = carreraRepository.findById(idCarrera)
                .orElseThrow(() -> new IllegalArgumentException("Carrera no encontrada con ID: " + idCarrera));
        String nombreNormalizado = dto.getNombreCarrera().trim();
        boolean nombreIgual = Objects.equals(carrera.getNombreCarrera(), nombreNormalizado);
        boolean facultadIgual = carrera.getFacultad() != null && Objects.equals(carrera.getFacultad().getIdFacultad(), dto.getIdFacultad());
        boolean nivelIgual = carrera.getNivel() != null && Objects.equals(carrera.getNivel().getIdNivel(), dto.getIdNivel());

        List<Long> idsSolicitados = dto.getIdsSedes();
        if (idsSolicitados == null || idsSolicitados.isEmpty() || idsSolicitados.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Debe seleccionar al menos una sede válida");
        }
        Set<Long> idsUnicos = new HashSet<>(idsSolicitados);
        if (idsUnicos.size() != idsSolicitados.size()) throw new IllegalArgumentException("No se permiten sedes duplicadas");

        List<CarreraSede> actuales = carreraSedeRepository.findByCarreraIdConSede(idCarrera);
        Set<Long> idsActuales = actuales.stream().map(cs -> cs.getSede().getIdSede()).collect(Collectors.toSet());
        Set<Long> idsAgregar = new HashSet<>(idsUnicos);
        idsAgregar.removeAll(idsActuales);
        List<CarreraSede> retirar = actuales.stream().filter(cs -> !idsUnicos.contains(cs.getSede().getIdSede())).toList();

        if (nombreIgual && facultadIgual && nivelIgual && idsAgregar.isEmpty() && retirar.isEmpty()) return;

        if (!nombreIgual && carreraRepository.existsByNombreCarreraIgnoreCaseAndIdCarreraNot(nombreNormalizado, idCarrera)) {
            throw new IllegalArgumentException("El nombre de carrera '" + nombreNormalizado + "' ya está registrado en otra carrera");
        }

        Facultad nuevaFacultad = null;
        if (!facultadIgual) {
            nuevaFacultad = facultadRepository.findByIdConEstado(dto.getIdFacultad())
                    .orElseThrow(() -> new IllegalArgumentException("Facultad no encontrada con ID: " + dto.getIdFacultad()));
            if (nuevaFacultad.getEstadoFacultad() == null || !Objects.equals(nuevaFacultad.getEstadoFacultad().getIdEstadoFacultad(), 1L)) {
                throw new IllegalArgumentException("No se puede vincular una carrera a una facultad inactiva");
            }
        }

        NivelAcademico nuevoNivel = null;
        if (!nivelIgual) {
            nuevoNivel = nivelAcademicoRepository.findById(dto.getIdNivel())
                    .orElseThrow(() -> new IllegalArgumentException("Nivel académico no encontrado con ID: " + dto.getIdNivel()));
        }

        List<Sede> sedesAgregar = idsAgregar.isEmpty() ? List.of() : sedeRepository.findAllById(idsAgregar);
        if (sedesAgregar.size() != idsAgregar.size()) throw new IllegalArgumentException("Una o más sedes seleccionadas no existen");
        for (Sede sede : sedesAgregar) {
            if (sede.getEstadoSede() == null || !Objects.equals(sede.getEstadoSede().getIdEstadoSede(), 1L)) {
                throw new IllegalArgumentException("La sede '" + sede.getNombreSede() + "' se encuentra inactiva");
            }
        }

        if (!retirar.isEmpty()) {
            for (CarreraSede cs : retirar) {
                if (carreraSedeRepository.contarEstudiantesAsociados(cs.getIdCarreraSede()) > 0) {
                    throw new IllegalArgumentException("No se puede retirar la sede '" + cs.getSede().getNombreSede() + "' porque tiene estudiantes asociados");
                }
                if (carreraSedeRepository.contarGruposPotencialmenteRelacionados(idCarrera, cs.getSede().getIdSede()) > 0) {
                    throw new IllegalArgumentException("No se puede retirar la sede '" + cs.getSede().getNombreSede() + "' porque existen grupos de materias de esta carrera en esa sede");
                }
            }
        }

        if (!facultadIgual) carrera.setFacultad(nuevaFacultad);
        if (!nivelIgual) carrera.setNivel(nuevoNivel);
        if (!nombreIgual) carrera.setNombreCarrera(nombreNormalizado);

        // Mantener sincronizada la colección gestionada: orphanRemoval elimina únicamente las asociaciones retiradas.
        for (CarreraSede cs : retirar) carrera.getCarrerasSedes().remove(cs);
        for (Sede sede : sedesAgregar) {
            CarreraSede asociacion = new CarreraSede();
            asociacion.setCarrera(carrera);
            asociacion.setSede(sede);
            carrera.getCarrerasSedes().add(asociacion);
        }

        auditoriaService.registrarEvento("ACTUALIZACION_CARRERA", "carreras", carrera.getIdCarrera(),
                String.format("Actualización de carrera [%s]. Nombre: '%s', Facultad ID: %d, Nivel ID: %d. Sedes agregadas: %s. Sedes retiradas: %s",
                        carrera.getCodigoCarrera(), nombreNormalizado, dto.getIdFacultad(), dto.getIdNivel(), idsAgregar,
                        retirar.stream().map(cs -> cs.getSede().getIdSede()).toList()));
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void desactivarCarrera(Long idCarrera, CarreraDesactivacionRequestDTO dto) {
        cambiarEstadoCarrera(idCarrera, dto, "INACTIVA", "DESACTIVACION_CARRERA", "Desactivación");
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void reactivarCarrera(Long idCarrera, CarreraDesactivacionRequestDTO dto) {
        cambiarEstadoCarrera(idCarrera, dto, "ACTIVA", "REACTIVACION_CARRERA", "Reactivación");
    }

    private void cambiarEstadoCarrera(Long idCarrera, CarreraDesactivacionRequestDTO dto, String nuevoEstado, String accionAuditoria, String descripcionAccion) {
        if (idCarrera == null || idCarrera <= 0) throw new IllegalArgumentException("El ID de la carrera debe ser un número positivo");
        if (dto == null || dto.getMotivo() == null || dto.getMotivo().trim().length() < 10 || dto.getMotivo().trim().length() > 250)
            throw new IllegalArgumentException("La justificación debe contener entre 10 y 250 caracteres");

        Carrera carrera = carreraRepository.findById(idCarrera)
                .orElseThrow(() -> new IllegalArgumentException("Carrera no encontrada con ID: " + idCarrera));
        EstadoCarrera estadoDestino = estadoCarreraRepository.findByEstadoCarreraIgnoreCase(nuevoEstado)
                .orElseThrow(() -> new IllegalStateException("El catálogo estados_carrera no contiene el estado '" + nuevoEstado + "'"));

        if (carrera.getEstadoCarrera() != null && Objects.equals(carrera.getEstadoCarrera().getIdEstadoCarrera(), estadoDestino.getIdEstadoCarrera())) {
            log.info("La carrera ID [{}] ya se encuentra en estado {}. No se ejecutan cambios.", idCarrera, nuevoEstado);
            return;
        }

        String estadoAnterior = carrera.getEstadoCarrera() != null ? carrera.getEstadoCarrera().getEstadoCarrera() : "SIN_ESTADO";
        carrera.setEstadoCarrera(estadoDestino);
        auditoriaService.registrarEvento(accionAuditoria, "carreras", carrera.getIdCarrera(),
                String.format("%s de carrera [%s - %s]. Estado: %s -> %s. Motivo: %s", descripcionAccion,
                        carrera.getCodigoCarrera(), carrera.getNombreCarrera(), estadoAnterior, nuevoEstado, dto.getMotivo().trim()));
        log.info("Carrera ID [{}]: estado actualizado de {} a {}.", idCarrera, estadoAnterior, nuevoEstado);
    }

/*
    private CarreraResponseDTO mapearADTO(Carrera c) {
        String facultadNombre = (c.getFacultad() != null)
                ? c.getFacultad().getNombreFacultad()
                : "Facultad Institucional";

        return new CarreraResponseDTO(
                c.getIdCarrera(),
                c.getCodigoCarrera(),
                c.getNombreCarrera(),
                facultadNombre,
                null,
                null
        );
    }*/

    private CarreraResponseDTO mapearADTO(Carrera c) {
        // Datos actuales que ya sacaba tu equipo
        String facultadNombre = (c.getFacultad() != null)
                ? c.getFacultad().getNombreFacultad()
                : "Facultad Institucional";

        // Tus nuevos datos extraídos de la entidad
        Long idFacultad = (c.getFacultad() != null) ? c.getFacultad().getIdFacultad() : null;
        Long idNivel = (c.getNivel() != null) ? c.getNivel().getIdNivel() : null;

        Long idEstado = (c.getEstadoCarrera() != null) ? c.getEstadoCarrera().getIdEstadoCarrera() : 3L; // 3L = ACTIVA por defecto
        String nombreEstado = (c.getEstadoCarrera() != null) ? c.getEstadoCarrera().getEstadoCarrera() : "ACTIVA";

        // Importante: El orden aquí debe coincidir exactamente con el record
        return new CarreraResponseDTO(
                c.getIdCarrera(),
                c.getCodigoCarrera(),
                c.getNombreCarrera(),
                facultadNombre,
                null, // idPensumVigente (Lo dejamos null como en tu código original)
                null, // versionPensum (Lo dejamos null como en tu código original)

                // Tus nuevos campos:
                idFacultad,
                idNivel,
                idEstado,
                nombreEstado
        );
    }

    @Transactional(readOnly = true)
    public List<CarreraAdministracionResponseDTO> listarCarrerasAdministracion() {
        return carreraRepository.findAllParaAdministracion().stream()
                .map(carrera -> new CarreraAdministracionResponseDTO(
                        carrera.getIdCarrera(),
                        carrera.getCodigoCarrera(),
                        carrera.getNombreCarrera(),
                        carrera.getFacultad().getIdFacultad(),
                        carrera.getFacultad().getNombreFacultad(),
                        carrera.getNivel().getIdNivel(),
                        carrera.getEstadoCarrera().getIdEstadoCarrera(),
                        carrera.getEstadoCarrera().getEstadoCarrera(),
                        carrera.getCarrerasSedes().stream()
                                .map(cs -> new CarreraAdministracionResponseDTO.SedeAsignadaDTO(
                                        cs.getSede().getIdSede(),
                                        cs.getSede().getNombreSede()
                                ))
                                .sorted(java.util.Comparator.comparing(CarreraAdministracionResponseDTO.SedeAsignadaDTO::idSede))
                                .toList()
                ))
                .toList();
    }
}