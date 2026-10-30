package com.codice.sra.services;

import com.codice.sra.dtos.OfertaNuevoIngresoDTO;
import com.codice.sra.exceptions.GrupoException;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfertaNuevoIngresoService {

    // 1. Inyectamos CicloService para unificar la regla de resolución de ciclo
    private final CicloService cicloService;
    private final PensumMateriaRepository pensumMateriaRepository;
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;
    private final InscripcionRepository inscripcionRepository;

    @Transactional(readOnly = true)
    public List<OfertaNuevoIngresoDTO> obtenerOfertaPrimerCiclo(Long idPensum, Long idSede) {

        // 2. APLICACIÓN DE LA REGLA DE DOMINIO:
        // Prioriza PLANIFICADO (ej. 01-2027) y valida ventana de 45 días si es ACTIVO
        Ciclo cicloOperativo = cicloService.obtenerCicloParaNuevoIngreso();

        log.info("Generando oferta de nuevo ingreso para Pensum ID [{}], Sede ID [{}] bajo Ciclo [{}] (Estado: {})",
                idPensum, idSede, cicloOperativo.getCodigoCiclo(),
                cicloOperativo.getEstadoCiclo() != null ? cicloOperativo.getEstadoCiclo().getEstadoCiclo() : "N/A");

        List<Materia> materiasPrimerCiclo = pensumMateriaRepository.findMateriasPrimerCicloByPensum(idPensum.intValue());

        if (materiasPrimerCiclo.isEmpty()) {
            log.warn("El Pensum ID [{}] no posee materias parametrizadas para el nivel/ciclo 1.", idPensum);
            return Collections.emptyList();
        }

        List<Long> idMaterias = materiasPrimerCiclo.stream()
                .map(Materia::getIdMateria)
                .collect(Collectors.toList());

        // 3. Consulta de grupos garantizando que pertenezcan al cicloOperativo resuelto
        List<Grupo> gruposDisponibles = grupoRepository.findGruposParaOfertaAcademica(
                idMaterias,
                idSede,
                cicloOperativo.getIdCiclo()
        );

        if (gruposDisponibles.isEmpty()) {
            log.info("No hay grupos aperturados en el ciclo [{}] para las materias de primer ciclo.", cicloOperativo.getCodigoCiclo());
            return Collections.emptyList();
        }

        List<OfertaNuevoIngresoDTO> ofertaFinal = new ArrayList<>();

        for (Materia materia : materiasPrimerCiclo) {

            List<Grupo> gruposMateria = gruposDisponibles.stream()
                    .filter(g -> g.getMateria().getIdMateria().equals(materia.getIdMateria()))
                    .toList();

            if (gruposMateria.isEmpty()) {
                continue;
            }

            List<OfertaNuevoIngresoDTO.GrupoDisponibleDTO> gruposDTO = new ArrayList<>();

            for (Grupo grupo : gruposMateria) {
                // Cálculo de cupos disponibles
                int inscritos = inscripcionRepository.countInscripcionesActivasPorGrupo(grupo.getIdGrupo());
                int disponibles = Math.max(0, grupo.getCupoMaximo() - inscritos);

                // Consulta de horarios de la sección
                List<Horario> horarios = horarioRepository.findHorariosParaOfertaAcademica(grupo.getIdGrupo());
                List<OfertaNuevoIngresoDTO.HorarioDTO> horariosDTO = horarios.stream().map(h ->
                        OfertaNuevoIngresoDTO.HorarioDTO.builder()
                                .diaSemana(h.getDia() != null ? h.getDia().getDia() : "N/A")
                                .horaInicio(h.getHoraInicio())
                                .horaFin(h.getHoraFin())
                                .modalidad(h.getModalidad() != null ? h.getModalidad().getModalidad() : "N/A")
                                .aula(h.getAula() != null ? h.getAula().getCodigoAula() : "Virtual")
                                .enlaceVirtual(h.getEnlaceVirtual())
                                .build()
                ).collect(Collectors.toList());

                // Mapeo defensivo de datos del docente
                String nombreDocenteStr = "Sin asignar";
                if (grupo.getDocente() != null && grupo.getDocente().getPersona() != null) {
                    nombreDocenteStr = grupo.getDocente().getPersona().getNombres() + " " + grupo.getDocente().getPersona().getApellidos();
                }

                gruposDTO.add(OfertaNuevoIngresoDTO.GrupoDisponibleDTO.builder()
                        .idGrupo(grupo.getIdGrupo())
                        .codigoGrupo(grupo.getCodigoGrupo())
                        .nombreDocente(nombreDocenteStr)
                        .cupoMaximo(grupo.getCupoMaximo())
                        .cuposDisponibles(disponibles)
                        .horarios(horariosDTO)
                        .build());
            }

            if (!gruposDTO.isEmpty()) {
                ofertaFinal.add(OfertaNuevoIngresoDTO.builder()
                        .idMateria(materia.getIdMateria())
                        .codigoMateria(materia.getCodigoMateria())
                        .nombreMateria(materia.getNombreMateria())
                        .unidadesValorativas(materia.getUnidadesValorativas() != null ? materia.getUnidadesValorativas().intValue() : 0)
                        .gruposDisponibles(gruposDTO)
                        .build());
            }
        }

        return ofertaFinal;
    }
}