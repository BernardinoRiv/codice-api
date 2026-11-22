package com.codice.sra.services;

import com.codice.sra.dtos.ComprobantePagoDTO;
import com.codice.sra.dtos.InscripcionContinuaResponseDTO;
import com.codice.sra.dtos.InscribirMateriasRequestDTO;
import com.codice.sra.dtos.OfertaMatriculaEstudianteDTO;
import com.codice.sra.exceptions.GrupoException;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatriculaIngresoContinuoService {
    private static final long CURSANDO = 1L;
    private static final long APROBADA = 2L;
    private static final long RETIRADA = 5L;
    private static final long RETIRO_PENDIENTE = 6L;

    private final EstudianteRepository estudianteRepository;
    private final EstudianteCarreraRepository trayectoriaRepository;
    private final CicloMatriculaValidatorService cicloValidator;
    private final CicloRepository cicloRepository;
    private final PensumMateriaRepository pensumMateriaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final EstadoMateriaEstudianteRepository estadoMateriaRepository;
    private final EstadoInscripcionRepository estadoInscripcionRepository;
    private final EstadoMatriculaRepository estadoMatriculaRepository;
    private final CargoEstudianteRepository cargoEstudianteRepository;
    private final ConceptoCobroRepository conceptoCobroRepository;
    private final EstadoCargoRepository estadoCargoRepository;
    private final PagoRepository pagoRepository;
    private final PagoCargoRepository pagoCargoRepository;
    private final FacturaRepository facturaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final CanalPagoRepository canalPagoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final EstadoFacturaRepository estadoFacturaRepository;
    private final ComprobantePdfService comprobantePdfService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public OfertaMatriculaEstudianteDTO obtenerOferta(Long idUsuario) {
        Estudiante estudiante = obtenerEstudiante(idUsuario);
        EstudianteCarrera trayectoria = obtenerTrayectoria(estudiante);
        Ciclo ciclo = cicloValidator.resolverCicloParaIngresoContinuo().ciclo();
        OfertaFinanciera ofertaFinanciera = obtenerOfertaFinanciera(estudiante, ciclo);
        List<Inscripcion> historial = inscripcionRepository.findAllByEstudianteId(estudiante.getIdEstudiante());
        Map<Long, List<Inscripcion>> porMateria = historial.stream().collect(Collectors.groupingBy(
                i -> i.getGrupo().getMateria().getIdMateria()));
        Set<Long> aprobadas = historial.stream().filter(this::esAprobada)
                .map(i -> i.getGrupo().getMateria().getIdMateria()).collect(Collectors.toSet());
        Set<Long> yaEnDestino = historial.stream()
                .filter(i -> Objects.equals(i.getGrupo().getCiclo().getIdCiclo(), ciclo.getIdCiclo()))
                .map(i -> i.getGrupo().getMateria().getIdMateria()).collect(Collectors.toSet());
        Long cicloActivo = cicloRepository.findCicloPorEstadoActivo().map(Ciclo::getIdCiclo).orElse(null);

        List<PensumMateria> materias = pensumMateriaRepository.findDetalleByPensumId(trayectoria.getPensum().getIdPensum()).stream()
                .sorted(Comparator.comparing(PensumMateria::getCicloRecomendado,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(pm -> pm.getMateria().getCodigoMateria(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<OfertaMatriculaEstudianteDTO.MateriaDTO> resultado = new ArrayList<>();
        for (PensumMateria pm : materias) {
            Materia materia = pm.getMateria();
            List<String> motivos = new ArrayList<>();
            List<Inscripcion> historialMateria = porMateria.getOrDefault(materia.getIdMateria(), List.of());
            if (aprobadas.contains(materia.getIdMateria())) motivos.add("Materia ya aprobada");
            if (yaEnDestino.contains(materia.getIdMateria())) motivos.add("Ya se encuentra inscrita en este ciclo");
            if (cicloActivo != null && historialMateria.stream().anyMatch(i ->
                    Objects.equals(i.getGrupo().getCiclo().getIdCiclo(), cicloActivo)
                            && (estadoId(i) == CURSANDO || estadoId(i) == RETIRO_PENDIENTE))) {
                motivos.add("Actualmente cursando en el ciclo vigente");
            }
            if (materia.getPrerrequisitos() != null) {
                for (Materia prereq : materia.getPrerrequisitos()) {
                    if (!aprobadas.contains(prereq.getIdMateria())) {
                        motivos.add("Requiere aprobar previamente: [" + prereq.getCodigoMateria() + "] - " + prereq.getNombreMateria());
                    }
                }
            }

            List<Grupo> grupos = grupoRepository.findGruposParaOfertaAcademica(
                    List.of(materia.getIdMateria()), trayectoria.getCarreraSede().getSede().getIdSede(), ciclo.getIdCiclo());
            List<OfertaMatriculaEstudianteDTO.GrupoDTO> gruposDto = grupos.stream()
                    .filter(g -> g.getEstadoGrupo() != null && g.getEstadoGrupo().getIdEstadoGrupo() == 1L)
                    .map(g -> mapearGrupo(g, inscripcionRepository.countInscripcionesActivasPorGrupo(g.getIdGrupo())))
                    .filter(g -> g.cuposDisponibles() > 0)
                    .toList();
            if (motivos.isEmpty() && gruposDto.isEmpty()) {
                motivos.add("No hay grupos abiertos con cupo disponible en tu sede para este ciclo");
            }
            resultado.add(new OfertaMatriculaEstudianteDTO.MateriaDTO(materia.getIdMateria(),
                    materia.getCodigoMateria(), materia.getNombreMateria(), pm.getCicloRecomendado(),
                    materia.getUnidadesValorativas(), motivos.isEmpty(),
                    List.copyOf(motivos), gruposDto));
        }
        return new OfertaMatriculaEstudianteDTO(ciclo.getIdCiclo(), ciclo.getCodigoCiclo(),
                ofertaFinanciera.montoMatricula(), "Matrícula",
                ofertaFinanciera.montoPrimeraCuota(), "Cuota 1 / Primera Mensualidad",
                ofertaFinanciera.montoMatricula().add(ofertaFinanciera.montoPrimeraCuota()),
                ofertaFinanciera.pagada(),
                ofertaFinanciera.factura() == null ? null : ofertaFinanciera.factura().getNumeroFactura(),
                ofertaFinanciera.pago() == null ? null : ofertaFinanciera.pago().getIdPago(), resultado);
    }

    @Transactional(rollbackFor = Exception.class)
    public InscripcionContinuaResponseDTO inscribirMaterias(Long idUsuario, List<Long> idsGrupos,
                                                              InscribirMateriasRequestDTO pagoRequest,
                                                              String idempotencyKey) {
        if (idsGrupos == null || idsGrupos.isEmpty()) throw new IllegalArgumentException("Debe seleccionar al menos un grupo.");
        if (new HashSet<>(idsGrupos).size() != idsGrupos.size()) throw new IllegalArgumentException("La selección contiene grupos duplicados.");
        if (idsGrupos.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Los identificadores de grupo no pueden ser nulos.");
        Estudiante estudiante = obtenerEstudiante(idUsuario);
        EstudianteCarrera trayectoria = obtenerTrayectoria(estudiante);
        Ciclo ciclo = cicloValidator.resolverCicloParaIngresoContinuo().ciclo();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Pago> pagoPrevio = pagoRepository.findByReferencia(idempotencyKey);
            if (pagoPrevio.isPresent()) return responderReintentoPago(pagoPrevio.get(), estudiante, ciclo, idsGrupos);
        }

        Map<Long, Grupo> gruposPorId = new LinkedHashMap<>();
        for (Long id : idsGrupos.stream().sorted().toList()) {
            Grupo grupo = grupoRepository.findByIdWithLock(id)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el grupo " + id + "."));
            if (!Objects.equals(grupo.getCiclo().getIdCiclo(), ciclo.getIdCiclo()))
                throw GrupoException.reglaNegocio("El grupo " + grupo.getCodigoGrupo() + " no pertenece al ciclo de matrícula habilitado " + ciclo.getCodigoCiclo() + ".");
            if (!Objects.equals(grupo.getSede().getIdSede(), trayectoria.getCarreraSede().getSede().getIdSede()))
                throw GrupoException.reglaNegocio("El grupo " + grupo.getCodigoGrupo() + " no pertenece a tu sede.");
            if (grupo.getEstadoGrupo() == null || grupo.getEstadoGrupo().getIdEstadoGrupo() != 1L)
                throw GrupoException.reglaNegocio("El grupo " + grupo.getCodigoGrupo() + " no está ABIERTO.");
            gruposPorId.put(id, grupo);
        }

        List<Grupo> seleccionados = idsGrupos.stream().map(gruposPorId::get).toList();
        Set<Long> materiasSeleccionadas = new HashSet<>();
        Set<Long> idsPensum = pensumMateriaRepository.findDetalleByPensumId(trayectoria.getPensum().getIdPensum())
                .stream().map(pm -> pm.getMateria().getIdMateria()).collect(Collectors.toSet());
        List<Inscripcion> historial = inscripcionRepository.findAllByEstudianteId(estudiante.getIdEstudiante());
        Set<Long> aprobadas = historial.stream().filter(this::esAprobada).map(i -> i.getGrupo().getMateria().getIdMateria()).collect(Collectors.toSet());
        Set<Long> materiasDestino = historial.stream().filter(i -> Objects.equals(i.getGrupo().getCiclo().getIdCiclo(), ciclo.getIdCiclo()))
                .map(i -> i.getGrupo().getMateria().getIdMateria()).collect(Collectors.toSet());
        Long cicloActivo = cicloRepository.findCicloPorEstadoActivo().map(Ciclo::getIdCiclo).orElse(null);

        for (Grupo grupo : seleccionados) {
            Materia materia = grupo.getMateria();
            Long idMateria = materia.getIdMateria();
            if (!idsPensum.contains(idMateria)) throw GrupoException.reglaNegocio("La materia " + materia.getCodigoMateria() + " no pertenece a tu pensum activo.");
            if (!materiasSeleccionadas.add(idMateria)) throw GrupoException.reglaNegocio("Seleccionaste más de un grupo para la materia " + materia.getCodigoMateria() + ".");
            if (aprobadas.contains(idMateria)) throw GrupoException.reglaNegocio("La materia " + materia.getCodigoMateria() + " ya está aprobada.");
            if (materiasDestino.contains(idMateria)) throw GrupoException.reglaNegocio("Ya tienes inscrita la materia " + materia.getCodigoMateria() + " en este ciclo.");
            if (cicloActivo != null && historial.stream().anyMatch(i -> i.getGrupo().getMateria().getIdMateria().equals(idMateria)
                    && Objects.equals(i.getGrupo().getCiclo().getIdCiclo(), cicloActivo)
                    && (estadoId(i) == CURSANDO || estadoId(i) == RETIRO_PENDIENTE)))
                throw GrupoException.reglaNegocio("La materia " + materia.getCodigoMateria() + " ya la estás cursando en el ciclo vigente.");
            for (Materia prereq : materia.getPrerrequisitos() == null ? List.<Materia>of() : materia.getPrerrequisitos()) {
                if (!aprobadas.contains(prereq.getIdMateria())) throw GrupoException.reglaNegocio(
                        "No puedes inscribir " + materia.getCodigoMateria() + ": requiere aprobar previamente [" + prereq.getCodigoMateria() + "] - " + prereq.getNombreMateria() + ".");
            }
        }

        validarChoques(estudiante.getIdEstudiante(), ciclo.getIdCiclo(), seleccionados);
        for (Grupo grupo : seleccionados) {
            long ocupados = inscripcionRepository.countInscripcionesActivasPorGrupo(grupo.getIdGrupo());
            if (ocupados >= grupo.getCupoMaximo()) throw GrupoException.reglaNegocio("El grupo " + grupo.getCodigoGrupo() + " ya no tiene cupos disponibles.");
        }

        Optional<Matricula> matriculaExistente = matriculaRepository.findByEstudianteIdEstudianteAndCicloIdCiclo(
                estudiante.getIdEstudiante(), ciclo.getIdCiclo());
        Matricula matricula = matriculaExistente.orElseGet(() -> {
                    Matricula nueva = new Matricula();
                    nueva.setEstudiante(estudiante);
                    nueva.setEstudianteCarrera(trayectoria);
                    nueva.setCiclo(ciclo);
                    nueva.setFechaMatricula(java.time.LocalDateTime.now());
                    nueva.setEstadoMatricula(estadoMatriculaRepository.findById(1L).orElseThrow());
                    return matriculaRepository.save(nueva);
                });
        ConceptoCobro conceptoMatricula = conceptoMatricula(ciclo);
        asegurarCargosIniciales(matricula, ciclo, conceptoMatricula);
        CargoEstudiante cargoMatricula = cargoEstudianteRepository
                .findByMatricula_IdMatriculaAndConceptoCobro_TipoCobro_IdTipoCobro(matricula.getIdMatricula(), 1L)
                .orElseThrow(() -> GrupoException.reglaNegocio("No se pudo generar el cargo de matrícula del ciclo."));
        CargoEstudiante cargoPrimeraCuota = cargoEstudianteRepository
                .findByMatricula_IdMatriculaAndConceptoCobro_TipoCobro_IdTipoCobroAndNumeroCuota(matricula.getIdMatricula(), 2L, 1)
                .orElseThrow(() -> GrupoException.reglaNegocio("No se pudo generar el cargo de la primera cuota del ciclo."));

        boolean matriculaYaPagada = esPagado(cargoMatricula) && esPagado(cargoPrimeraCuota);
        Pago pagoRealizado = null;
        Factura factura = null;
        String referenciaPago = null;
        BigDecimal montoPagado = BigDecimal.ZERO;
        if (!matriculaYaPagada) {
            validarDatosTarjeta(pagoRequest);
            if (idempotencyKey == null || idempotencyKey.isBlank())
                throw new IllegalArgumentException("La cabecera 'Idempotency-Key' es requerida para procesar el pago.");
            List<CargoEstudiante> cargosInicialesPendientes = new ArrayList<>();
            if (!esPagado(cargoMatricula)) cargosInicialesPendientes.add(cargoMatricula);
            if (!esPagado(cargoPrimeraCuota)) cargosInicialesPendientes.add(cargoPrimeraCuota);
            if (cargosInicialesPendientes.stream().anyMatch(c -> c.getEstadoCargo() == null
                    || !"PENDIENTE".equalsIgnoreCase(c.getEstadoCargo().getEstadoCargo())))
                throw GrupoException.reglaNegocio("Los cargos de matrícula y primera cuota deben estar pendientes de pago.");
            pagoRealizado = registrarPagoMatricula(estudiante, matricula, cargosInicialesPendientes, pagoRequest, idempotencyKey);
            factura = facturaRepository.findByPago_IdPago(pagoRealizado.getIdPago())
                    .orElseThrow(() -> GrupoException.reglaNegocio("No se pudo emitir la factura del pago."));
            referenciaPago = pagoRealizado.getReferencia();
            montoPagado = pagoRealizado.getMontoPagado();
        } else {
            List<PagoCargo> pagosAplicados = new ArrayList<>(pagoCargoRepository.findByCargoIdCargoEstudiante(cargoMatricula.getIdCargoEstudiante()));
            pagosAplicados.addAll(pagoCargoRepository.findByCargoIdCargoEstudiante(cargoPrimeraCuota.getIdCargoEstudiante()));
            PagoCargo aplicado = pagosAplicados.stream().max(Comparator.comparing(pc -> pc.getPago().getFechaPago())).orElse(null);
            if (aplicado != null) {
                pagoRealizado = aplicado.getPago();
                factura = facturaRepository.findByPago_IdPago(pagoRealizado.getIdPago()).orElse(null);
                referenciaPago = pagoRealizado.getReferencia();
            }
        }
        EstadoInscripcion estadoInscripcion = estadoInscripcionRepository.findByEstadoInscripcion("ACTIVA")
                .orElseGet(() -> estadoInscripcionRepository.findById(1L).orElseThrow());
        EstadoMateriaEstudiante cursando = estadoMateriaRepository.findById(CURSANDO).orElseThrow();
        List<InscripcionContinuaResponseDTO.MateriaInscritaDTO> creadas = new ArrayList<>();
        for (Grupo grupo : seleccionados) {
            Inscripcion inscripcion = new Inscripcion();
            inscripcion.setMatricula(matricula);
            inscripcion.setGrupo(grupo);
            inscripcion.setFechaInscripcion(java.time.LocalDateTime.now());
            inscripcion.setEstadoInscripcion(estadoInscripcion);
            inscripcion.setEstadoMateria(cursando);
            Inscripcion guardada = inscripcionRepository.save(inscripcion);
            creadas.add(new InscripcionContinuaResponseDTO.MateriaInscritaDTO(guardada.getIdInscripcion(),
                    grupo.getIdGrupo(), grupo.getCodigoGrupo(), grupo.getMateria().getCodigoMateria(),
                    grupo.getMateria().getNombreMateria(), "CURSANDO"));
        }
        return new InscripcionContinuaResponseDTO(matricula.getIdMatricula(), ciclo.getCodigoCiclo(), creadas,
                !matriculaYaPagada, pagoRealizado == null ? null : pagoRealizado.getIdPago(),
                factura == null ? null : factura.getNumeroFactura(), referenciaPago, montoPagado,
                matriculaYaPagada ? "La matrícula ya estaba pagada. Las nuevas materias quedaron inscritas sin generar otro cobro."
                        : "Pago de matrícula procesado y materias inscritas correctamente.");
    }

    private void validarChoques(Long idEstudiante, Long idCiclo, List<Grupo> nuevos) {
        List<Inscripcion> existentes = inscripcionRepository.findByEstudianteAndCiclo(idEstudiante, idCiclo).stream()
                .filter(i -> estadoId(i) != RETIRADA).toList();
        List<Grupo> grupos = new ArrayList<>(nuevos);
        grupos.addAll(existentes.stream().map(Inscripcion::getGrupo).toList());
        for (int i = 0; i < grupos.size(); i++) {
            Grupo a = grupos.get(i);
            for (int j = i + 1; j < grupos.size(); j++) {
                Grupo b = grupos.get(j);
                if (a.getIdGrupo().equals(b.getIdGrupo())) continue;
                for (Horario ha : horarioRepository.findByGrupo_IdGrupo(a.getIdGrupo())) {
                    for (Horario hb : horarioRepository.findByGrupo_IdGrupo(b.getIdGrupo())) {
                        if (Objects.equals(ha.getDia().getIdDia(), hb.getDia().getIdDia())
                                && ha.getHoraInicio().isBefore(hb.getHoraFin())
                                && hb.getHoraInicio().isBefore(ha.getHoraFin())) {
                            throw GrupoException.reglaNegocio("Choque de horario: " + a.getMateria().getNombreMateria() + " (" + a.getCodigoGrupo() + ") y "
                                    + b.getMateria().getNombreMateria() + " (" + b.getCodigoGrupo() + ") el " + ha.getDia().getDia()
                                    + " de " + ha.getHoraInicio() + " a " + ha.getHoraFin() + ".");
                        }
                    }
                }
            }
        }
    }

    private OfertaMatriculaEstudianteDTO.GrupoDTO mapearGrupo(Grupo grupo, long ocupados) {
        List<OfertaMatriculaEstudianteDTO.HorarioDTO> horarios = horarioRepository.findByGrupo_IdGrupo(grupo.getIdGrupo()).stream()
                .map(h -> new OfertaMatriculaEstudianteDTO.HorarioDTO(h.getDia().getDia(), h.getHoraInicio(), h.getHoraFin(),
                        h.getAula() == null ? "Virtual" : h.getAula().getCodigoAula())).toList();
        String docente = grupo.getDocente() == null || grupo.getDocente().getPersona() == null ? "Sin asignar"
                : grupo.getDocente().getPersona().getNombres() + " " + grupo.getDocente().getPersona().getApellidos();
        return new OfertaMatriculaEstudianteDTO.GrupoDTO(grupo.getIdGrupo(), grupo.getCodigoGrupo(), docente,
                grupo.getCupoMaximo(), (int)Math.max(0, grupo.getCupoMaximo() - ocupados), horarios);
    }

    private Estudiante obtenerEstudiante(Long idUsuario) {
        return estudianteRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> GrupoException.noEncontrado("No existe un expediente de estudiante asociado al usuario autenticado."));
    }

    private OfertaFinanciera obtenerOfertaFinanciera(Estudiante estudiante, Ciclo ciclo) {
        ConceptoCobro conceptoMatricula = conceptoMatricula(ciclo);
        ConceptoCobro conceptoCuota = conceptoCobroRepository
                .findByCiclo_IdCicloAndTipoCobro_IdTipoCobroAndActivoTrue(ciclo.getIdCiclo(), 2L)
                .orElseThrow(() -> GrupoException.reglaNegocio("No está configurado el arancel de cuota mensual para el ciclo " + ciclo.getCodigoCiclo() + "."));
        Matricula matricula = matriculaRepository.findByEstudianteIdEstudianteAndCicloIdCiclo(
                estudiante.getIdEstudiante(), ciclo.getIdCiclo()).orElse(null);
        CargoEstudiante cargoMatricula = matricula == null ? null : cargoEstudianteRepository
                .findByMatricula_IdMatriculaAndConceptoCobro_TipoCobro_IdTipoCobro(matricula.getIdMatricula(), 1L).orElse(null);
        CargoEstudiante cargoCuota = matricula == null ? null : cargoEstudianteRepository
                .findByMatricula_IdMatriculaAndConceptoCobro_TipoCobro_IdTipoCobroAndNumeroCuota(matricula.getIdMatricula(), 2L, 1).orElse(null);
        Pago pago = null;
        Factura factura = null;
        if (cargoMatricula != null && cargoCuota != null && esPagado(cargoMatricula) && esPagado(cargoCuota)) {
            List<PagoCargo> aplicados = new ArrayList<>(pagoCargoRepository.findByCargoIdCargoEstudiante(cargoMatricula.getIdCargoEstudiante()));
            aplicados.addAll(pagoCargoRepository.findByCargoIdCargoEstudiante(cargoCuota.getIdCargoEstudiante()));
            PagoCargo aplicado = aplicados.stream().max(Comparator.comparing(pc -> pc.getPago().getFechaPago())).orElse(null);
            if (aplicado != null) {
                pago = aplicado.getPago();
                factura = facturaRepository.findByPago_IdPago(pago.getIdPago()).orElse(null);
            }
        }
        return new OfertaFinanciera(
                cargoMatricula == null ? conceptoMatricula.getMontoBase() : cargoMatricula.getMontoTotal(),
                cargoCuota == null ? conceptoCuota.getMontoBase() : cargoCuota.getMontoTotal(),
                pago, factura, cargoMatricula != null && cargoCuota != null && esPagado(cargoMatricula) && esPagado(cargoCuota));
    }

    private ConceptoCobro conceptoMatricula(Ciclo ciclo) {
        return conceptoCobroRepository
                .findByCiclo_IdCicloAndTipoCobro_IdTipoCobroAndActivoTrue(ciclo.getIdCiclo(), 1L)
                .orElseThrow(() -> GrupoException.reglaNegocio("No está configurado el arancel de matrícula para el ciclo " + ciclo.getCodigoCiclo() + "."));
    }

    private void asegurarCargosIniciales(Matricula matricula, Ciclo ciclo, ConceptoCobro conceptoMatricula) {
        ConceptoCobro conceptoCuota = conceptoCobroRepository
                .findByCiclo_IdCicloAndTipoCobro_IdTipoCobroAndActivoTrue(ciclo.getIdCiclo(), 2L)
                .orElseThrow(() -> GrupoException.reglaNegocio("No está configurado el arancel de cuota mensual para el ciclo " + ciclo.getCodigoCiclo() + "."));
        EstadoCargo pendiente = estadoCargoRepository.findById(1L)
                .orElseThrow(() -> GrupoException.reglaNegocio("No está configurado el estado de cargo pendiente."));
        LocalDate hoy = LocalDate.now();
        if (!cargoEstudianteRepository.existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobro(
                matricula.getIdMatricula(), conceptoMatricula.getIdConceptoCobro())) {
            crearCargo(matricula, conceptoMatricula, pendiente, 1, hoy.plusDays(3));
        }
        int mesInicio = ciclo.getNumeroCiclo() != null && ciclo.getNumeroCiclo() == 2 ? 7 : 1;
        int anio = ciclo.getAnio() == null ? hoy.getYear() : ciclo.getAnio();
        for (int cuota = 1; cuota <= 6; cuota++) {
            if (!cargoEstudianteRepository.existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobroAndNumeroCuota(
                    matricula.getIdMatricula(), conceptoCuota.getIdConceptoCobro(), cuota)) {
                crearCargo(matricula, conceptoCuota, pendiente, cuota,
                        LocalDate.of(anio, mesInicio + cuota - 1, 15));
            }
        }
    }

    private Pago registrarPagoMatricula(Estudiante estudiante, Matricula matricula, List<CargoEstudiante> cargos,
                                       InscribirMateriasRequestDTO request, String idempotencyKey) {
        BigDecimal total = cargos.stream().map(CargoEstudiante::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Pago pago = new Pago();
        pago.setEstudiante(estudiante);
        pago.setMetodoPago(metodoPagoRepository.findByMetodoPago("TARJETA")
                .orElseGet(() -> metodoPagoRepository.findById(2L).orElseThrow()));
        pago.setCanalPago(canalPagoRepository.findByCanalPago("PORTAL")
                .orElseGet(() -> canalPagoRepository.findById(2L).orElseThrow()));
        pago.setEstadoPago(estadoPagoRepository.findByEstadoPago("APROBADO")
                .orElseGet(() -> estadoPagoRepository.findById(1L).orElseThrow()));
        pago.setMontoPagado(total);
        pago.setFechaPago(LocalDateTime.now());
        pago.setNumeroAutorizacion("AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        pago.setReferencia(idempotencyKey);
        pago = pagoRepository.save(pago);

        EstadoCargo pagado = estadoCargoRepository.findByEstadoCargo("PAGADO")
                .orElseGet(() -> estadoCargoRepository.findById(2L).orElseThrow());
        for (CargoEstudiante cargo : cargos) {
            cargo.setEstadoCargo(pagado);
            cargoEstudianteRepository.save(cargo);
            PagoCargo detalle = new PagoCargo();
            detalle.setPago(pago);
            detalle.setCargo(cargo);
            detalle.setMontoAplicado(cargo.getMontoTotal());
            pagoCargoRepository.save(detalle);
        }

        Persona persona = estudiante.getPersona();
        Factura factura = new Factura();
        factura.setPago(pago);
        factura.setNumeroFactura("FAC-WEB-" + System.currentTimeMillis());
        factura.setFechaEmision(LocalDateTime.now());
        factura.setNombreReceptor(persona.getNombres() + " " + persona.getApellidos());
        factura.setDocumentoReceptor(estudiante.getCarnet() != null ? estudiante.getCarnet() : persona.getNumeroDocumento());
        factura.setCorreoReceptor(persona.getCorreoPersonal());
        factura.setDireccionReceptor(persona.getDireccion() == null || persona.getDireccion().isBlank()
                ? "Santa Ana, El Salvador" : persona.getDireccion());
        factura.setSubtotal(total);
        factura.setDescuento(BigDecimal.ZERO);
        factura.setRecargo(BigDecimal.ZERO);
        factura.setTotal(total);
        factura.setEstadoFactura(estadoFacturaRepository.findByEstadoFactura("EMITIDA")
                .orElseGet(() -> estadoFacturaRepository.findById(1L).orElseThrow()));
        facturaRepository.save(factura);

        EstadoMatricula matriculada = estadoMatriculaRepository.findByEstadoMatricula("MATRICULADA")
                .or(() -> estadoMatriculaRepository.findByEstadoMatricula("INSCRITA"))
                .or(() -> estadoMatriculaRepository.findById(2L))
                .orElseThrow(() -> GrupoException.reglaNegocio("No está configurado el estado de matrícula pagada."));
        matricula.setEstadoMatricula(matriculada);
        matriculaRepository.save(matricula);
        programarEnvioComprobante(estudiante, cargos, factura, total);
        return pago;
    }

    private void validarDatosTarjeta(InscribirMateriasRequestDTO request) {
        if (request == null || request.numeroTarjeta() == null || !request.numeroTarjeta().matches("^[0-9]{13,19}$"))
            throw new IllegalArgumentException("El número de tarjeta es obligatorio y debe contener de 13 a 19 dígitos.");
        if (request.titularTarjeta() == null || request.titularTarjeta().isBlank() || request.titularTarjeta().length() > 150)
            throw new IllegalArgumentException("El titular de la tarjeta es obligatorio y no debe exceder 150 caracteres.");
        if (request.fechaExpiracion() == null || !request.fechaExpiracion().matches("^(0[1-9]|1[0-2])/[0-9]{2}$"))
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria y debe usar el formato MM/YY.");
        if (request.cvv() == null || !request.cvv().matches("^[0-9]{3,4}$"))
            throw new IllegalArgumentException("El CVV debe contener 3 o 4 dígitos.");
    }

    private void programarEnvioComprobante(Estudiante estudiante, List<CargoEstudiante> cargos, Factura factura, BigDecimal total) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        Persona persona = estudiante.getPersona();
        String correo = persona.getCorreoPersonal();
        if (correo == null || correo.isBlank()) return;
        String nombre = persona.getNombres() + " " + persona.getApellidos();
        ComprobantePagoDTO comprobante = ComprobantePagoDTO.builder()
                .numeroFactura(factura.getNumeroFactura())
                .fechaEmision(factura.getFechaEmision())
                .nombreCompleto(nombre)
                .carnetEstudiante(estudiante.getCarnet())
                .correoEstudiante(correo)
                .totalCobrado(total)
                .cajeroResponsable("PAGO EN LÍNEA (PORTAL)")
                .detalles(cargos.stream().map(cargo -> ComprobantePagoDTO.DetalleComprobanteDTO.builder()
                        .concepto(cargo.getConceptoCobro().getTipoCobro().getTipoCobro())
                        .montoTotal(cargo.getMontoTotal()).build()).toList())
                .build();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                CompletableFuture.runAsync(() -> {
                    try {
                        byte[] pdf = comprobantePdfService.generarPdf(comprobante);
                        emailService.enviarComprobantePago(correo, nombre, factura.getNumeroFactura(), pdf);
                    } catch (Exception e) {
                        log.error("No se pudo enviar la factura {}: {}", factura.getNumeroFactura(), e.getMessage(), e);
                    }
                });
            }
        });
    }

    private InscripcionContinuaResponseDTO responderReintentoPago(Pago pago, Estudiante estudiante, Ciclo ciclo,
                                                                   List<Long> idsGrupos) {
        if (!pago.getEstudiante().getIdEstudiante().equals(estudiante.getIdEstudiante()))
            throw GrupoException.reglaNegocio("La clave Idempotency-Key ya fue utilizada en otra transacción.");
        List<PagoCargo> detalles = pagoCargoRepository.findByPago_IdPago(pago.getIdPago());
        PagoCargo cargoMatricula = detalles.stream().filter(pc -> pc.getCargo().getConceptoCobro().getTipoCobro().getIdTipoCobro() == 1L)
                .findFirst().orElseThrow(() -> GrupoException.reglaNegocio("La clave Idempotency-Key no corresponde a un pago de matrícula."));
        Matricula matricula = cargoMatricula.getCargo().getMatricula();
        if (!Objects.equals(matricula.getCiclo().getIdCiclo(), ciclo.getIdCiclo()))
            throw GrupoException.reglaNegocio("La clave Idempotency-Key ya fue utilizada en otro ciclo.");
        Map<Long, Inscripcion> inscritas = inscripcionRepository.findByMatricula_IdMatricula(matricula.getIdMatricula()).stream()
                .collect(Collectors.toMap(i -> i.getGrupo().getIdGrupo(), i -> i, (a, b) -> a));
        if (!inscritas.keySet().containsAll(idsGrupos))
            throw GrupoException.reglaNegocio("La clave Idempotency-Key ya fue procesada con otra selección de grupos.");
        List<InscripcionContinuaResponseDTO.MateriaInscritaDTO> materias = idsGrupos.stream().map(inscritas::get)
                .map(i -> new InscripcionContinuaResponseDTO.MateriaInscritaDTO(i.getIdInscripcion(), i.getGrupo().getIdGrupo(),
                        i.getGrupo().getCodigoGrupo(), i.getGrupo().getMateria().getCodigoMateria(),
                        i.getGrupo().getMateria().getNombreMateria(), i.getEstadoMateria().getEstadoMateria())).toList();
        Factura factura = facturaRepository.findByPago_IdPago(pago.getIdPago()).orElse(null);
        return new InscripcionContinuaResponseDTO(matricula.getIdMatricula(), ciclo.getCodigoCiclo(), materias,
                false, pago.getIdPago(), factura == null ? null : factura.getNumeroFactura(), pago.getReferencia(),
                BigDecimal.ZERO, "La solicitud ya fue procesada; no se generó un segundo cobro.");
    }

    private boolean esPagado(CargoEstudiante cargo) {
        return cargo.getEstadoCargo() != null && (cargo.getEstadoCargo().getIdEstadoCargo() == 2L
                || "PAGADO".equalsIgnoreCase(cargo.getEstadoCargo().getEstadoCargo()));
    }

    private record OfertaFinanciera(BigDecimal montoMatricula, BigDecimal montoPrimeraCuota,
                                    Pago pago, Factura factura, boolean pagada) {}

    private void crearCargo(Matricula matricula, ConceptoCobro concepto, EstadoCargo estado,
                            int numeroCuota, LocalDate vencimiento) {
        BigDecimal monto = concepto.getMontoBase();
        CargoEstudiante cargo = new CargoEstudiante();
        cargo.setMatricula(matricula);
        cargo.setConceptoCobro(concepto);
        cargo.setEstadoCargo(estado);
        cargo.setFechaGeneracion(java.time.LocalDateTime.now());
        cargo.setFechaVencimiento(vencimiento);
        cargo.setNumeroCuota(numeroCuota);
        cargo.setMontoBase(monto);
        cargo.setMontoDescuento(BigDecimal.ZERO);
        cargo.setMontoRecargo(BigDecimal.ZERO);
        cargo.setMontoTotal(monto);
        cargoEstudianteRepository.save(cargo);
    }

    private EstudianteCarrera obtenerTrayectoria(Estudiante estudiante) {
        return trayectoriaRepository.findFirstByEstudiante_IdEstudianteOrderByFechaInicioDesc(estudiante.getIdEstudiante())
                .orElseThrow(() -> GrupoException.reglaNegocio("El estudiante no tiene una trayectoria académica activa."));
    }

    private boolean esAprobada(Inscripcion i) { return estadoId(i) == APROBADA; }
    private long estadoId(Inscripcion i) { return i.getEstadoMateria() == null ? -1L : i.getEstadoMateria().getIdEstadoMateria(); }

}
