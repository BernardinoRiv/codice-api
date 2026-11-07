
package com.codice.sra.services;

import com.codice.sra.dtos.DashboardDTO.*;
import com.codice.sra.models.MvDashboardGerencial;
import com.codice.sra.repositories.DashboardRepository;
import com.codice.sra.repositories.CicloRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository metricasRepository;
    private final CicloRepository cicloRepository;

    @Transactional(readOnly = true)
    public DashboardGerencialDTO obtenerMetricas(
            String codigoCiclo,
            Long idSede,
            String granularidad
    ) {

        // 1. Determinar el ciclo a evaluar
        String cicloAEvaluar =
                (codigoCiclo == null || codigoCiclo.isBlank())
                        ? cicloRepository.findCicloPorEstadoActivo()
                        .map(c -> c.getCodigoCiclo())
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe ningún ciclo activo configurado en el sistema."
                        ))
                        : codigoCiclo.trim();

        // 2. Obtener métricas existentes de la vista materializada
        MvDashboardGerencial metricas =
                metricasRepository.findByCodigoCiclo(cicloAEvaluar)
                        .orElseGet(() -> {
                            MvDashboardGerencial empty =
                                    new MvDashboardGerencial();

                            empty.setCodigoCiclo(cicloAEvaluar);
                            empty.setTotalMatricula(0L);
                            empty.setTotalDocentes(0L);
                            empty.setTotalSecciones(0L);
                            empty.setCapacidadPromedio(0.0);
                            empty.setTotalFacturado(BigDecimal.ZERO);
                            empty.setTotalRecaudado(BigDecimal.ZERO);

                            return empty;
                        });

        // 3. Normalizar granularidad
        String modoGranularidad =
                (granularidad == null || granularidad.isBlank())
                        ? "semanal"
                        : granularidad.trim().toLowerCase();

        // 4. Crear DTO conservando las métricas existentes
        DashboardGerencialDTO dto = new DashboardGerencialDTO(
                metricas.getTotalMatricula(),
                metricas.getTotalDocentes(),
                metricas.getTotalSecciones(),
                metricas.getCapacidadPromedio() != null
                        ? metricas.getCapacidadPromedio()
                        : 0.0,
                metricas.getTotalFacturado(),
                metricas.getTotalRecaudado()
        );

        // =====================================================
        // FASE 1: MATRÍCULA ACTIVA
        // =====================================================

        Long totalMatriculas =
                metricasRepository.contarMatriculasActivas(
                        cicloAEvaluar, idSede
                );

        Long conMaterias =
                metricasRepository.contarMatriculadosConMaterias(
                        cicloAEvaluar, idSede
                );

        Long sinMaterias =
                metricasRepository.contarMatriculadosSinMaterias(
                        cicloAEvaluar, idSede
                );

        dto.setMatriculaActiva(
                totalMatriculas != null ? totalMatriculas : 0L
        );

        dto.setMatriculadosConMaterias(
                conMaterias != null ? conMaterias : 0L
        );

        dto.setMatriculadosSinMaterias(
                sinMaterias != null ? sinMaterias : 0L
        );

        // Distribuciones académicas
        dto.setDistribucionMatriculaFacultad(
                metricasRepository.getMatriculaPorFacultad(
                        cicloAEvaluar, idSede
                )
        );

        // Cargar todas las carreras y sus materias en una sola respuesta HTTP.
        // La consulta de materias cuenta estudiantes distintos por asignatura.
        List<DashboardGerencialDTO.CarreraConMateriasDTO> carreras =
                metricasRepository.getMatriculaPorCarrera(cicloAEvaluar, idSede)
                        .stream()
                        .map(carrera -> {
                            DashboardGerencialDTO.CarreraConMateriasDTO item =
                                    new DashboardGerencialDTO.CarreraConMateriasDTO(
                                            carrera.getIdCarrera(),
                                            carrera.getNombre(),
                                            carrera.getCantidad()
                                    );
                            item.setMaterias(
                                    metricasRepository.getMateriasPorCarrera(
                                                    cicloAEvaluar, idSede, carrera.getIdCarrera()
                                            ).stream()
                                            .map(materia -> new DashboardGerencialDTO.MateriaDTO(
                                                    materia.getIdMateria(),
                                                    materia.getNombre(),
                                                    materia.getCantidad()
                                            ))
                                            .toList()
                            );
                            return item;
                        })
                        .toList();
        dto.setDistribucionMatriculaCarrera(carreras);

        // Campo anterior conservado vacío para compatibilidad.
        // La nueva estructura se encuentra en cada carrera.materias.

        dto.setHistoricoMatricula(
                metricasRepository.getHistoricoMatricula(idSede)
                        .stream()
                        .filter(item -> compararCiclos(item.getCodigoCiclo(), cicloAEvaluar) <= 0)
                        .sorted((a, b) -> compararCiclos(a.getCodigoCiclo(), b.getCodigoCiclo()))
                        .toList()
        );

        // =====================================================
        // MÉTRICAS EXISTENTES: DOCENTES
        // =====================================================

        List<DistribucionProjection> facultades =
                metricasRepository.getDocentesPorFacultad(
                        cicloAEvaluar
                );

        List<DistribucionProjection> contratos =
                metricasRepository.getDocentesPorContrato(
                        cicloAEvaluar
                );

        dto.setDistribucionFacultades(facultades);
        dto.setDistribucionContratos(contratos);

        // =====================================================
        // MÉTRICAS EXISTENTES: ASISTENCIA
        // =====================================================

        List<AsistenciaSemanalProjection> asistenciaTemporal =
                metricasRepository.getAsistenciaPromedioTemporal(
                        cicloAEvaluar, modoGranularidad
                );

        dto.setAsistenciaSemanal(asistenciaTemporal);

        // No sobrescribir distribucionFacultades con asistencia.
        // La asistencia por facultad necesita un campo propio
        // cuando implementemos ese módulo.

        return dto;
    }


    @Transactional(readOnly = true)
    public DashboardDocentesDTO obtenerMetricasDocentes(String codigoCiclo, Long idSede) {
        String ciclo = (codigoCiclo == null || codigoCiclo.isBlank())
                ? cicloRepository.findCicloPorEstadoActivo()
                    .map(c -> c.getCodigoCiclo())
                    .orElseThrow(() -> new IllegalStateException("No existe un ciclo activo."))
                : codigoCiclo.trim();

        List<DashboardDocentesDTO.Distribucion> contratos =
                metricasRepository.getContratosDocentesDashboard(ciclo, idSede).stream()
                    .map(p -> new DashboardDocentesDTO.Distribucion(p.getNombre(), p.getCantidad()))
                    .toList();
        long total = contratos.stream().mapToLong(DashboardDocentesDTO.Distribucion::cantidad).sum();

        List<DashboardDocentesDTO.Historico> historico =
                metricasRepository.getHistoricoDocentesDashboard(idSede).stream()
                    .filter(p -> compararCiclos(p.getCodigoCiclo(), ciclo) <= 0)
                    .map(p -> new DashboardDocentesDTO.Historico(
                            p.getCodigoCiclo(), p.getTiempoCompleto(), p.getPorHoras()))
                    .toList();

        List<DashboardDocentesDTO.Docente> detalle = construirDocentes(
                metricasRepository.getAsignacionesDocentesDashboard(ciclo, idSede));

        Map<Long, List<DocenteAsignacionProjection>> porCarrera = new LinkedHashMap<>();
        metricasRepository.getAsignacionesDocentesCarreraDashboard(ciclo, idSede)
                .forEach(f -> porCarrera.computeIfAbsent(f.getIdCarrera(), k -> new java.util.ArrayList<>()).add(f));

        List<DashboardDocentesDTO.Carrera> carreras = porCarrera.entrySet().stream()
                .map(e -> {
                    List<DashboardDocentesDTO.Docente> docentes = construirDocentes(e.getValue());
                    return new DashboardDocentesDTO.Carrera(
                            e.getKey(), e.getValue().get(0).getNombreCarrera(),
                            docentes.size(), docentes);
                })
                .sorted((a, b) -> Long.compare(b.cantidad(), a.cantidad()))
                .toList();

        return new DashboardDocentesDTO(total, contratos, historico, carreras, detalle);
    }

    private static List<DashboardDocentesDTO.Docente> construirDocentes(
            List<DocenteAsignacionProjection> filas) {
        Map<Long, List<DocenteAsignacionProjection>> porDocente = new LinkedHashMap<>();
        filas.forEach(f -> porDocente.computeIfAbsent(f.getIdDocente(), k -> new java.util.ArrayList<>()).add(f));
        return porDocente.entrySet().stream().map(e -> {
            List<DocenteAsignacionProjection> asignaciones = e.getValue();
            DocenteAsignacionProjection primera = asignaciones.get(0);
            Map<Long, Set<Long>> gruposPorMateria = new LinkedHashMap<>();
            Map<Long, String> nombres = new LinkedHashMap<>();
            asignaciones.forEach(f -> {
                gruposPorMateria.computeIfAbsent(f.getIdMateria(), k -> new LinkedHashSet<>()).add(f.getIdGrupo());
                nombres.putIfAbsent(f.getIdMateria(), f.getNombreMateria());
            });
            List<DashboardDocentesDTO.Materia> materias = gruposPorMateria.entrySet().stream()
                    .map(m -> new DashboardDocentesDTO.Materia(
                            m.getKey(), nombres.get(m.getKey()), m.getValue().size()))
                    .toList();
            return new DashboardDocentesDTO.Docente(
                    e.getKey(), primera.getNombreDocente(), primera.getTipoContratacion(),
                    materias.size(), materias);
        }).toList();
    }

    // Orden cronológico para códigos de la forma 1-2026, 2-2026 o 01-2027.
    private static int compararCiclos(String a, String b) {
        int[] x = partesCiclo(a);
        int[] y = partesCiclo(b);
        int porAnio = Integer.compare(x[1], y[1]);
        return porAnio != 0 ? porAnio : Integer.compare(x[0], y[0]);
    }

    private static int[] partesCiclo(String codigo) {
        if (codigo == null || !codigo.matches("\\d+-\\d{4}")) {
            throw new IllegalArgumentException("Código de ciclo no reconocido: " + codigo);
        }
        String[] partes = codigo.split("-");
        return new int[]{Integer.parseInt(partes[0]), Integer.parseInt(partes[1])};
    }
}
