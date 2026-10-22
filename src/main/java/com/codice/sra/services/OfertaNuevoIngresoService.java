package com.codice.sra.services;

import com.codice.sra.dtos.OfertaNuevoIngresoDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfertaNuevoIngresoService {

    private final CicloRepository cicloRepository;
    private final PensumMateriaRepository pensumMateriaRepository;
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;
    private final InscripcionRepository inscripcionRepository;

    @Transactional(readOnly = true)
    public List<OfertaNuevoIngresoDTO> obtenerOfertaPrimerCiclo(Long idPensum, Long idSede) {

        Ciclo cicloOperativo = cicloRepository.findCicloActivo()
                .or(cicloRepository::findCicloEnPlanificacion)
                .orElseThrow(() -> new RuntimeException("No hay un ciclo activo ni en planificación en el sistema."));

        List<Materia> materiasPrimerCiclo = pensumMateriaRepository.findMateriasPrimerCicloByPensum(idPensum.intValue());

        if(materiasPrimerCiclo.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> idMaterias = materiasPrimerCiclo.stream().map(Materia::getIdMateria).collect(Collectors.toList());

        // USAMOS LA NUEVA CONSULTA PERSONALIZADA
        List<Grupo> gruposDisponibles = grupoRepository.findGruposParaOfertaAcademica(idMaterias, idSede, cicloOperativo.getIdCiclo());

        List<OfertaNuevoIngresoDTO> ofertaFinal = new ArrayList<>();

        for (Materia materia : materiasPrimerCiclo) {

            List<OfertaNuevoIngresoDTO.GrupoDisponibleDTO> gruposDTO = new ArrayList<>();

            List<Grupo> gruposMateria = gruposDisponibles.stream()
                    .filter(g -> g.getMateria().getIdMateria().equals(materia.getIdMateria()))
                    .toList();

            for (Grupo grupo : gruposMateria) {
                // USAMOS LA NUEVA CONSULTA DE CONTEO
                int inscritos = inscripcionRepository.countInscripcionesActivasPorGrupo(grupo.getIdGrupo());
                int disponibles = grupo.getCupoMaximo() - inscritos;

                // USAMOS LA NUEVA CONSULTA DE HORARIOS
                List<Horario> horarios = horarioRepository.findHorariosParaOfertaAcademica(grupo.getIdGrupo());
                List<OfertaNuevoIngresoDTO.HorarioDTO> horariosDTO = horarios.stream().map(h ->
                        OfertaNuevoIngresoDTO.HorarioDTO.builder()
                                .diaSemana(h.getDia() != null ? h.getDia().getDia() : "N/A")
                                .horaInicio(h.getHoraInicio())
                                .horaFin(h.getHoraFin())
                                .modalidad(h.getModalidad() != null ? h.getModalidad().getModalidad() : "N/A")
                                .aula(h.getAula() != null ? h.getAula().getCodigoAula() : "N/A")
                                .enlaceVirtual(h.getEnlaceVirtual())
                                .build()
                ).collect(Collectors.toList());

                // Manejo seguro del docente nulo
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
                        .unidadesValorativas(materia.getUnidadesValorativas().intValue())
                        .gruposDisponibles(gruposDTO)
                        .build());
            }
        }

        return ofertaFinal;
    }
}