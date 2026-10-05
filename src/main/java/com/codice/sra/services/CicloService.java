package com.codice.sra.services;

import com.codice.sra.dtos.CicloOperativoDTO;
import com.codice.sra.dtos.CicloPlanificacionDTO;
import com.codice.sra.dtos.CrearCicloPlanificacionDTO;
import com.codice.sra.dtos.SiguienteCicloSugeridoDTO;
import com.codice.sra.exceptions.GrupoException;
import com.codice.sra.models.Ciclo;
import com.codice.sra.models.EstadoCiclo;
import com.codice.sra.models.Grupo;
import com.codice.sra.repositories.CicloRepository;
import com.codice.sra.repositories.EstadoCicloRepository;
import com.codice.sra.repositories.GrupoRepository;
import com.codice.sra.repositories.HorarioRepository;
import com.codice.sra.utils.CicloCodigoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CicloService {

    private final CicloRepository cicloRepository;
    private final EstadoCicloRepository estadoCicloRepository;
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;

    /**
     * Resuelve el ciclo lectivo operativo:
     * - Prioridad 1: Ciclo en PLANIFICACIÓN (oferta académica futura).
     * - Prioridad 2: Ciclo ACTIVO (oferta regular en curso).
     */
    @Transactional(readOnly = true)
    public CicloOperativoDTO obtenerCicloParaOferta() {
        Ciclo ciclo = cicloRepository.findCicloEnPlanificacion()
                .or(cicloRepository::findCicloActivo)
                .orElseThrow(() -> GrupoException.reglaNegocio(
                        "No existe ningún ciclo en estado PLANIFICACIÓN ni ACTIVO en el sistema."));

        boolean esPlanificacion = "PLANIFICACION".equalsIgnoreCase(ciclo.getEstadoCiclo().getEstadoCiclo());

        return new CicloOperativoDTO(
                ciclo.getIdCiclo(),
                ciclo.getCodigoCiclo(),
                ciclo.getAnio(),
                ciclo.getNumeroCiclo(),
                ciclo.getEstadoCiclo().getEstadoCiclo(),
                ciclo.getFechaInicio(),
                ciclo.getFechaFin(),
                esPlanificacion
        );
    }

    /**
     * Proyección semestral matemática determinista:
     * (A, 1) -> (A, 2)
     * (A, 2) -> (A + 1, 1)
     * Formato resultante: '01-YYYY' o '02-YYYY'
     */
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
                        (long) (c.getGrupos() != null ? c.getGrupos().size() : 0)
                ))
                .toList();
    }

    /**
     * Transición atómica de relevo semestral:
     * - Cierra el ciclo que estaba ACTIVO pasando a FINALIZADO.
     * - Promueve el ciclo en PLANIFICACIÓN al estado ACTIVO.
     */
    @Transactional(rollbackFor = Exception.class)
    public void promoverCicloAActivo(Long idCicloPlanificado) {
        Ciclo cicloPlanificado = cicloRepository.findById(idCicloPlanificado)
                .orElseThrow(() -> GrupoException.noEncontrado("El ciclo lectivo a activar no existe."));

        if (!"PLANIFICACION".equalsIgnoreCase(cicloPlanificado.getEstadoCiclo().getEstadoCiclo())) {
            throw GrupoException.reglaNegocio("Solo se puede activar un ciclo que se encuentre en PLANIFICACIÓN.");
        }

        EstadoCiclo estadoFinalizado = estadoCicloRepository.findByEstadoCicloIgnoreCase("FINALIZADO")
                .orElseThrow(() -> GrupoException.noEncontrado("Estado 'FINALIZADO' no configurado en la base de datos."));

        EstadoCiclo estadoActivo = estadoCicloRepository.findByEstadoCicloIgnoreCase("ACTIVO")
                .orElseThrow(() -> GrupoException.noEncontrado("Estado 'ACTIVO' no configurado en la base de datos."));

        // 1. Relevar el ciclo activo saliente si existe
        cicloRepository.findCicloActivo().ifPresent(actual -> {
            log.info("Finalizando ciclo saliente: [{}]", actual.getCodigoCiclo());
            actual.setEstadoCiclo(estadoFinalizado);
            cicloRepository.save(actual);
        });

        // 2. Activar el nuevo ciclo
        cicloPlanificado.setEstadoCiclo(estadoActivo);
        cicloRepository.save(cicloPlanificado);
        log.info("Ciclo lectivo [{}] promovido exitosamente a estado ACTIVO.", cicloPlanificado.getCodigoCiclo());
    }


    @Transactional(rollbackFor = Exception.class)
    public void eliminarCicloPlanificado(Long idCiclo) {
        Ciclo ciclo = cicloRepository.findById(idCiclo)
                .orElseThrow(() -> GrupoException.noEncontrado("El ciclo lectivo especificado no existe."));

        // Regla de Integridad Histórica: Solo se permite eliminar periodos en PLANIFICACIÓN
        String estadoActual = ciclo.getEstadoCiclo().getEstadoCiclo().trim().toUpperCase();
        if (!"PLANIFICACION".equals(estadoActual)) {
            throw GrupoException.reglaNegocio(String.format(
                    "Operación denegada: No se puede eliminar el ciclo '%s' porque su estado es '%s'. Solo los ciclos en PLANIFICACIÓN pueden eliminarse.",
                    ciclo.getCodigoCiclo(), estadoActual));
        }

        // Limpieza de secciones y horarios si ya se habían cargado
        List<Grupo> gruposAsociados = grupoRepository.findByCiclo_IdCiclo(idCiclo);
        if (!gruposAsociados.isEmpty()) {
            for (Grupo g : gruposAsociados) {
                horarioRepository.deleteByGrupo_IdGrupo(g.getIdGrupo());
            }
            grupoRepository.deleteAll(gruposAsociados);
            log.info("Se eliminaron {} secciones asociadas al ciclo [{}] previo a su purga.",
                    gruposAsociados.size(), ciclo.getCodigoCiclo());
        }

        cicloRepository.delete(ciclo);
        log.info("Ciclo lectivo [{}] eliminado satisfactoriamente del sistema.", ciclo.getCodigoCiclo());
    }
}