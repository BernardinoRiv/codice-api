package com.codice.sra.services;

// Nota: Recuerda la convención de paquetes en minúsculas (dto en lugar de DashboardDTO)
import com.codice.sra.dtos.DashboardDTO.*;
import com.codice.sra.models.MvDashboardGerencial;
import com.codice.sra.repositories.DashboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    // BUENA PRÁCTICA 1: Dependencia inmutable (final)
    private final DashboardRepository metricasRepository;

    // BUENA PRÁCTICA 2: Inyección por constructor en lugar de @Autowired en el campo.
    // Esto hace que la clase sea fácil de instanciar en pruebas unitarias (Mocks).
    public DashboardService(DashboardRepository metricasRepository) {
        this.metricasRepository = metricasRepository;
    }

    // BUENA PRÁCTICA 3: Optimización a nivel de Hibernate. No hace tracking de entidades.
    @Transactional(readOnly = true)
    public DashboardGerencialDTO obtenerMetricas(String codigoCiclo) {

        // 1. Obtener la métrica base de la vista materializada
        MvDashboardGerencial metricas = metricasRepository.findByCodigoCiclo(codigoCiclo)
                .orElseThrow(() -> new IllegalArgumentException("No hay métricas para el ciclo: " + codigoCiclo));

        // 2. Obtener los datos dinámicos para los gráficos (Drill-down)
        List<DistribucionProjection> facultades = metricasRepository.getDocentesPorFacultad(codigoCiclo);
        List<DistribucionProjection> contratos = metricasRepository.getDocentesPorContrato(codigoCiclo);
        List<AsistenciaSemanalProjection> asistencia = metricasRepository.getAsistenciaPromedioSemanal(codigoCiclo);

        // 3. Mapeo directo del Modelo al DTO (Base)
        DashboardGerencialDTO dto = new DashboardGerencialDTO(
                metricas.getCodigoCiclo(),
                metricas.getTotalMatricula(),
                metricas.getTotalDocentes(),
                metricas.getTotalSecciones(),
                metricas.getCapacidadPromedio(),
                metricas.getTotalFacturado(),
                metricas.getTotalRecaudado()
        );

        // 4. Enriquecer el DTO con las proyecciones analíticas
        dto.setDistribucionFacultades(facultades);
        dto.setDistribucionContratos(contratos);
        dto.setAsistenciaSemanal(asistencia);

        return dto;
    }
}