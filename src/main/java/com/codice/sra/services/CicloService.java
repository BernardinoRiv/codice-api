package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.exceptions.GrupoException;
import com.codice.sra.models.Ciclo;
import com.codice.sra.models.ConceptoCobro;
import com.codice.sra.models.EstadoCiclo;
import com.codice.sra.models.Grupo;
import com.codice.sra.repositories.*;
import com.codice.sra.utils.CicloCodigoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CicloService {

    private final CicloRepository cicloRepository;
    private final EstadoCicloRepository estadoCicloRepository;
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;
    private final FinanzasService finanzasService;
    private final ConceptoCobroRepository conceptoCobroRepository;

    /**
     * Resuelve el ciclo lectivo operativo:
     * - Prioridad 1: Ciclo en PLANIFICACIÓN (oferta académica futura).
     * - Prioridad 2: Ciclo ACTIVO (oferta regular en curso).
     */
    @Transactional(readOnly = true)
    public CicloOperativoDTO obtenerCicloParaOferta() {
        // 1. Priorizar el ciclo formalmente ACTIVO en clases.
        // Si no hay ninguno activo, se toma como alternativa el que esté en planificación.
        Ciclo ciclo = cicloRepository.findCicloActivo()
                .or(cicloRepository::findCicloEnPlanificacion)
                .orElseThrow(() -> GrupoException.reglaNegocio(
                        "No existe ningún ciclo en estado ACTIVO ni en PLANIFICACIÓN en el sistema."));

        String estado = (ciclo.getEstadoCiclo() != null && ciclo.getEstadoCiclo().getEstadoCiclo() != null)
                ? ciclo.getEstadoCiclo().getEstadoCiclo().trim().toUpperCase()
                : "PLANIFICACION";

        boolean esPlanificacion = "PLANIFICACION".equals(estado) || "PLANIFICADO".equals(estado);

        return new CicloOperativoDTO(
                ciclo.getIdCiclo(),
                ciclo.getCodigoCiclo(),
                ciclo.getAnio(),
                ciclo.getNumeroCiclo(),
                estado,
                ciclo.getFechaInicio(),
                ciclo.getFechaFin(),
                esPlanificacion
        );
    }

    @Transactional(readOnly = true)
    public SiguienteCicloSugeridoDTO sugerirSiguienteCiclo() {
        return cicloRepository.findUltimoCicloRegistrado()
                .map(ultimo -> {
                    int siguienteAnio = ultimo.getAnio();
                    int siguienteNumero = (ultimo.getNumeroCiclo() == 1) ? 2 : 1;

                    if (siguienteNumero == 1) {
                        siguienteAnio++;
                    }

                    String codigoSugerido = CicloCodigoUtil.generarCodigoCiclo(siguienteNumero, siguienteAnio);
                    String descripcion = String.format("Ciclo %02d - Año %d (Semestre %d)",
                            siguienteNumero, siguienteAnio, siguienteNumero);

                    return new SiguienteCicloSugeridoDTO(siguienteAnio, siguienteNumero, codigoSugerido, descripcion);
                })
                .orElseGet(() -> {
                    int anioActual = LocalDate.now().getYear();
                    String codigoInicial = CicloCodigoUtil.generarCodigoCiclo(1, anioActual);
                    return new SiguienteCicloSugeridoDTO(anioActual, 1, codigoInicial, "Ciclo 01 - Año " + anioActual);
                });
    }

    /**
     * Registra un ciclo en PLANIFICACIÓN con validación de ventanas temporales.
     */
    @Transactional(rollbackFor = Exception.class)
    public CicloOperativoDTO registrarCicloPlanificado(CrearCicloPlanificacionDTO dto) {
        // Regla: No permitir más de un ciclo en planificación de manera simultánea
        if (cicloRepository.findCicloEnPlanificacion().isPresent()) {
            throw GrupoException.reglaNegocio(
                    "Ya existe un ciclo lectivo en PLANIFICACIÓN. Debe activarlo antes de proyectar el siguiente.");
        }

        SiguienteCicloSugeridoDTO sugerido = sugerirSiguienteCiclo();

        if (cicloRepository.existsByAnioAndNumeroCiclo(sugerido.anio(), sugerido.numeroCiclo())) {
            throw GrupoException.conflicto(
                    String.format("El ciclo %s ya se encuentra registrado en el sistema.", sugerido.codigoSugerido()));
        }

        EstadoCiclo estadoPlanificacion = estadoCicloRepository.findByEstadoCicloIgnoreCase("PLANIFICACION")
                .orElseThrow(() -> GrupoException.noEncontrado("Estado 'PLANIFICACION' no configurado en el catálogo."));

        // Definición de fechas institucionales predeterminadas según el semestre
        LocalDate fechaInicioAdministrativa;
        LocalDate fechaFinAdministrativa;

        if (sugerido.numeroCiclo() == 1) {
            fechaInicioAdministrativa = LocalDate.of(sugerido.anio(), 1, 1);
            fechaFinAdministrativa = LocalDate.of(sugerido.anio(), 6, 30);
        } else {
            fechaInicioAdministrativa = LocalDate.of(sugerido.anio(), 7, 1);
            fechaFinAdministrativa = LocalDate.of(sugerido.anio(), 12, 31);
        }

        Ciclo nuevoCiclo = Ciclo.builder()
                .estadoCiclo(estadoPlanificacion)
                .anio(sugerido.anio())
                .numeroCiclo(sugerido.numeroCiclo())
                .codigoCiclo(sugerido.codigoSugerido())
                .fechaInicio(fechaInicioAdministrativa)
                .fechaFin(fechaFinAdministrativa)
                .build();

        Ciclo persistido = cicloRepository.save(nuevoCiclo);
        log.info("Ciclo [{}] registrado con vigencia fija [{} al {}]",
                persistido.getCodigoCiclo(), fechaInicioAdministrativa, fechaFinAdministrativa);

        // Clonación de aranceles del ciclo anterior
        Optional<Ciclo> cicloAnteriorOpt = cicloRepository.findCicloActivo();

        if (cicloAnteriorOpt.isPresent()) {
            Long idCicloAnterior = cicloAnteriorOpt.get().getIdCiclo();
            Long idCicloNuevo = persistido.getIdCiclo();

            try {
                int clonados = finanzasService.clonarArancelesCicloAnterior(idCicloAnterior, idCicloNuevo);
                log.info("Finanzas: {} aranceles clonados del ciclo previo ID [{}] al nuevo ciclo ID [{}]",
                        clonados, idCicloAnterior, idCicloNuevo);
            } catch (Exception ex) {
                log.error("Fallo al clonar aranceles para el ciclo ID [{}]: {}", persistido.getIdCiclo(), ex.getMessage());
                // Lanzamos GrupoException para forzar el ROLLBACK de cicloRepository.save(nuevoCiclo)
                throw GrupoException.reglaNegocio(
                        "No se pudo completar la creación del ciclo. Fallo en módulo de aranceles: " + ex.getMessage());
            }
        } else {
            log.warn("No se encontró ciclo anterior activo. Se omite clonación de aranceles.");
        }

        return new CicloOperativoDTO(
                persistido.getIdCiclo(),
                persistido.getCodigoCiclo(),
                persistido.getAnio(),
                persistido.getNumeroCiclo(),
                estadoPlanificacion.getEstadoCiclo(),
                persistido.getFechaInicio(),
                persistido.getFechaFin(),
                true
        );
    }

    /**
     * Consulta los periodos en planificación con el número de secciones asociadas.
     */
    @Transactional(readOnly = true)
    public List<CicloPlanificacionDTO> listarCiclosEnPlanificacion() {
        return cicloRepository.findAllEnPlanificacion().stream()
                .map(c -> new CicloPlanificacionDTO(
                        c.getIdCiclo(),
                        c.getCodigoCiclo(),
                        c.getAnio(),
                        c.getNumeroCiclo(),
                        c.getFechaInicio(),
                        c.getFechaFin(),
                        c.getEstadoCiclo() != null ? c.getEstadoCiclo().getEstadoCiclo() : "PLANIFICACION",
                        (long) (c.getGrupos() != null ? c.getGrupos().size() : 0)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ArancelesAperturaDTO obtenerArancelesApertura(Long idCiclo) {
        Ciclo ciclo = cicloRepository.findById(idCiclo)
                .orElseThrow(() -> GrupoException.noEncontrado("Ciclo no encontrado con ID: " + idCiclo));

        List<ConceptoCobro> conceptos = conceptoCobroRepository.findByCiclo_IdCiclo(idCiclo);

        ConceptoCobro matPre = null;
        ConceptoCobro cuoPre = null;
        ConceptoCobro matMae = null;
        ConceptoCobro cuoMae = null;

        for (ConceptoCobro c : conceptos) {
            if (c.getTipoCobro() != null && c.getTipoCobro().getIdTipoCobro() != null) {
                long tipoId = c.getTipoCobro().getIdTipoCobro();

                // Pregrado
                if (tipoId == 1L) {
                    matPre = c; // Matrícula de Pregrado ($70.00)
                } else if (tipoId == 2L) {
                    cuoPre = c; // Cuota Mensual de Pregrado ($70.00)
                }

                // Maestría (Prioridad Docencia Universitaria 6 y 7, con fallback a 8 y 9)
                else if (tipoId == 6L || (tipoId == 8L && matMae == null)) {
                    matMae = c; // Matrícula de Maestría ($100.00 / $120.00)
                } else if (tipoId == 7L || (tipoId == 9L && cuoMae == null)) {
                    cuoMae = c; // Cuota Mensual de Maestría ($100.00 / $130.00)
                }
            }
        }

        boolean completos = (matPre != null && cuoPre != null);

        return new ArancelesAperturaDTO(
                ciclo.getIdCiclo(),
                matPre != null ? matPre.getIdConceptoCobro() : null,
                matPre != null ? matPre.getMontoBase() : BigDecimal.ZERO,
                cuoPre != null ? cuoPre.getIdConceptoCobro() : null,
                cuoPre != null ? cuoPre.getMontoBase() : BigDecimal.ZERO,
                matMae != null ? matMae.getIdConceptoCobro() : null,
                matMae != null ? matMae.getMontoBase() : BigDecimal.ZERO,
                cuoMae != null ? cuoMae.getIdConceptoCobro() : null,
                cuoMae != null ? cuoMae.getMontoBase() : BigDecimal.ZERO,
                completos
        );
    }


    @Transactional(rollbackFor = Exception.class)
    public void eliminarCicloPlanificado(Long idCiclo) {
        log.info("Iniciando purga controlada del ciclo ID [{}]...", idCiclo);

        // 1. Obtención y validación de existencia
        Ciclo ciclo = cicloRepository.findById(idCiclo)
                .orElseThrow(() -> GrupoException.noEncontrado("El ciclo lectivo especificado no existe."));

        // 2. Programación Defensiva (Fail-Fast): Validar estado de borrador
        String estadoActual = (ciclo.getEstadoCiclo() != null && ciclo.getEstadoCiclo().getEstadoCiclo() != null)
                ? ciclo.getEstadoCiclo().getEstadoCiclo().trim().toUpperCase()
                : "";

        if (!"PLANIFICACION".equals(estadoActual) && !"EN_PLANIFICACION".equals(estadoActual)) {
            throw GrupoException.reglaNegocio(String.format(
                    "Operación denegada: No se puede eliminar el ciclo '%s' porque su estado es '%s'. " +
                            "Solo los ciclos en fase de planificación pueden purgarse.",
                    ciclo.getCodigoCiclo(), estadoActual));
        }

        // 3.Purgar los aranceles/conceptos clonados del ciclo
        conceptoCobroRepository.deleteByCiclo_IdCiclo(idCiclo);
        log.info("Aranceles clonados del ciclo [{}] purgados satisfactoriamente.", ciclo.getCodigoCiclo());

        // 4. Obtener SOLO IDs de los grupos para limpiar horarios (sin cargar entidades Grupo a memoria)
        List<Long> idsGrupos = grupoRepository.findIdsByCiclo_IdCiclo(idCiclo);

        if (!idsGrupos.isEmpty()) {
            // Borrar horarios de esos grupos
            horarioRepository.deleteByGrupo_IdGrupoIn(idsGrupos);

            // Borrar los grupos directamente a nivel SQL
            grupoRepository.deleteByCiclo_IdCiclo(idCiclo);
            log.info("Se purgaron los horarios y grupos asociados al ciclo [{}]", ciclo.getCodigoCiclo());
        }

        // 5. Purga final del ciclo lectivo
        cicloRepository.delete(ciclo);
        log.info("Ciclo lectivo [{}] purgado satisfactoriamente del sistema.", ciclo.getCodigoCiclo());
    }

    @Transactional(rollbackFor = Exception.class)
    public CicloOperativoDTO confirmarPlanificacion(Long idCiclo) {
        log.info("Iniciando confirmación de planificación para ciclo ID [{}]...", idCiclo);

        // 1. Validar existencia de ciclo
        Ciclo ciclo = cicloRepository.findById(idCiclo)
                .orElseThrow(() -> GrupoException.noEncontrado("Ciclo no encontrado con ID: " + idCiclo));

        String estadoActual = ciclo.getEstadoCiclo() != null
                ? ciclo.getEstadoCiclo().getEstadoCiclo().trim().toUpperCase()
                : "";

        // 2. Fail-Fast: Solo se puede confirmar lo que está en preparación
        if (!"EN_PLANIFICACION".equals(estadoActual) && !"PLANIFICACION".equals(estadoActual)) {
            throw GrupoException.conflicto(String.format(
                    "El ciclo %s no puede ser confirmado porque su estado actual es '%s' (se requiere EN_PLANIFICACION).",
                    ciclo.getCodigoCiclo(), estadoActual));
        }

        // 3. Regla de Integridad Financiera: Debe tener aranceles configurados
        boolean tieneAranceles = conceptoCobroRepository.existsByCiclo_IdCiclo(idCiclo);
        if (!tieneAranceles) {
            throw GrupoException.reglaNegocio(String.format(
                    "No se puede confirmar la planificación del ciclo %s: No posee ningún arancel configurado.",
                    ciclo.getCodigoCiclo()));
        }

        boolean tieneSecciones = grupoRepository.existsByCiclo_IdCiclo(idCiclo);
        if (!tieneSecciones) {
            throw GrupoException.reglaNegocio(String.format(
                    "Operación denegada: El ciclo %s no posee ninguna materia o sección registrada. " +
                            "Debe registrar la oferta académica antes de confirmar la planificación.",
                    ciclo.getCodigoCiclo()));
        }

        // 4. Obtener estado PLANIFICADO del catálogo
        EstadoCiclo estadoPlanificado = estadoCicloRepository.findByEstadoCicloIgnoreCase("PLANIFICADO")
                .orElseThrow(() -> GrupoException.noEncontrado("Estado 'PLANIFICADO' no configurado en el catálogo."));

        // 5. Transición de estado inmutable
        ciclo.setEstadoCiclo(estadoPlanificado);
        Ciclo cicloGuardado = cicloRepository.save(ciclo);
        log.info("Ciclo [{}] pasó exitosamente a estado PLANIFICADO con su oferta académica fijada.", cicloGuardado.getCodigoCiclo());

        // 6. Retornar DTO operativo intacto (esPlanificacion = true)
        return new CicloOperativoDTO(
                cicloGuardado.getIdCiclo(),
                cicloGuardado.getCodigoCiclo(),
                cicloGuardado.getAnio(),
                cicloGuardado.getNumeroCiclo(),
                estadoPlanificado.getEstadoCiclo(),
                cicloGuardado.getFechaInicio(),
                cicloGuardado.getFechaFin(),
                true
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivacionCicloResponseDTO ejecutarRelevoYActivacion(GenerarCobrosMasivosRequestDTO request) {
        log.info("Iniciando relevo y activación formal para el ciclo ID [{}]", request.getIdCiclo());

        // 1. Fail-Fast: Validar aranceles obligatorios de Pregrado
        if (request.getIdMatriculaCarrera() == null || request.getIdCuotaCarrera() == null) {
            throw GrupoException.reglaNegocio(
                    "Debe especificar los aranceles obligatorios de matrícula y cuota para Pregrado.");
        }

        // 2. Obtener y validar el ciclo entrante
        Ciclo cicloNuevo = cicloRepository.findById(request.getIdCiclo())
                .orElseThrow(() -> GrupoException.noEncontrado("Ciclo no encontrado con ID: " + request.getIdCiclo()));

        String estadoActual = cicloNuevo.getEstadoCiclo() != null
                ? cicloNuevo.getEstadoCiclo().getEstadoCiclo().toUpperCase()
                : "";

        // Regla estricta: Solo un ciclo formalmente PLANIFICADO puede ser promovido a ACTIVO
        if (!"PLANIFICADO".equals(estadoActual)) {
            throw GrupoException.conflicto(String.format(
                    "El ciclo %s no puede activarse directamente. Su estado actual es '%s' y debe estar 'PLANIFICADO'.",
                    cicloNuevo.getCodigoCiclo(), estadoActual));
        }

        if (!grupoRepository.existsByCiclo_IdCiclo(request.getIdCiclo())) {
            throw GrupoException.reglaNegocio(String.format(
                    "Operación denegada: El ciclo '%s' no posee ninguna materia o sección registrada. " +
                            "No se puede activar un ciclo lectivo sin oferta académica.",
                    cicloNuevo.getCodigoCiclo()));
        }

        LocalDate hoy = LocalDate.now();
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 2.1. Validar que el ciclo entrante ya haya alcanzado su fecha de inicio
        if (cicloNuevo.getFechaInicio() != null && hoy.isBefore(cicloNuevo.getFechaInicio())) {
            throw GrupoException.conflicto(String.format(
                    "Operación denegada: El ciclo entrante '%s' no puede activarse antes de su fecha de inicio (%s). Fecha actual: %s.",
                    cicloNuevo.getCodigoCiclo(),
                    cicloNuevo.getFechaInicio().format(formatoFecha),
                    hoy.format(formatoFecha)));
        }

        // 2.2. Validar que el ciclo actualmente activo ya haya finalizado según calendario
        Optional<Ciclo> cicloPrevioActivoOpt = cicloRepository.findCicloActivo();
        if (cicloPrevioActivoOpt.isPresent()) {
            Ciclo cicloViejo = cicloPrevioActivoOpt.get();
            if (!cicloViejo.getIdCiclo().equals(cicloNuevo.getIdCiclo())
                    && cicloViejo.getFechaFin() != null
                    && hoy.isBefore(cicloViejo.getFechaFin())) {
                throw GrupoException.conflicto(String.format(
                        "Operación denegada: El ciclo activo actual '%s' aún no ha finalizado (su cierre está programado para el %s). Fecha actual: %s.",
                        cicloViejo.getCodigoCiclo(),
                        cicloViejo.getFechaFin().format(formatoFecha),
                        hoy.format(formatoFecha)));
            }
        }

        // 3. Obtener referencias inmutables de estados
        EstadoCiclo estadoActivo = estadoCicloRepository.findByEstadoCicloIgnoreCase("ACTIVO")
                .orElseThrow(() -> GrupoException.noEncontrado("Estado 'ACTIVO' no configurado en el catálogo."));

        EstadoCiclo estadoFinalizado = estadoCicloRepository.findByEstadoCicloIgnoreCase("FINALIZADO")
                .orElseThrow(() -> GrupoException.noEncontrado("Estado 'FINALIZADO' no configurado en el catálogo."));

        // 4. Disparar lógica contable del compañero (solo llega aquí si las fechas son válidas)
        int totalCobrosGenerados;
        try {
            totalCobrosGenerados = finanzasService.generarCobrosMatriculaAperturaCiclo(request);
            log.info("Finanzas generó satisfactoriamente {} cargos para el ciclo [{}]",
                    totalCobrosGenerados, cicloNuevo.getCodigoCiclo());
        } catch (Exception ex) {
            log.error("Fallo al generar cobros en módulo de finanzas: {}", ex.getMessage());
            // Provoca Rollback atómico de toda la operación
            throw GrupoException.reglaNegocio("Error en generación de cobros masivos: " + ex.getMessage());
        }

        // 5. Finalizar ciclo anterior activo (reutiliza el Optional ya consultado arriba)
        cicloPrevioActivoOpt.ifPresent(cicloViejo -> {
            if (!cicloViejo.getIdCiclo().equals(cicloNuevo.getIdCiclo())) {
                cicloViejo.setEstadoCiclo(estadoFinalizado);
                cicloRepository.save(cicloViejo);
                log.info("Ciclo saliente [{}] transicionado a FINALIZADO.", cicloViejo.getCodigoCiclo());
            }
        });

        // 6. Promover el ciclo nuevo a ACTIVO
        cicloNuevo.setEstadoCiclo(estadoActivo);
        cicloRepository.save(cicloNuevo);
        log.info("Ciclo entrante [{}] transicionado a ACTIVO exitosamente.", cicloNuevo.getCodigoCiclo());

        return new ActivacionCicloResponseDTO(
                true,
                String.format("Ciclo %s activado exitosamente. Se generaron %d cobros institucionales.",
                        cicloNuevo.getCodigoCiclo(), totalCobrosGenerados),
                cicloNuevo.getIdCiclo(),
                totalCobrosGenerados
        );
    }

}