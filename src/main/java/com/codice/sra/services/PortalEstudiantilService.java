package com.codice.sra.services;

import com.codice.sra.dtos.DashboardEstudianteDTO;
import com.codice.sra.dtos.HistorialAcademicoDTO;
import com.codice.sra.models.Estudiante;
import com.codice.sra.repositories.CargoEstudianteRepository;
import com.codice.sra.repositories.EstudianteRepository;
import com.codice.sra.repositories.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PortalEstudiantilService {

    private final InscripcionRepository inscripcionRepository;
    private final CargoEstudianteRepository cargoEstudianteRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstudianteService estudianteService; // Inyectamos tu servicio existente

    @Transactional(readOnly = true)
    public DashboardEstudianteDTO obtenerResumenDashboard(String carnet) {

        // 1. Obtener el estudiante para extraer su ID
        Estudiante estudiante = estudianteRepository.findByCarnet(carnet)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado en el sistema."));

        // 2. CUM Histórico (REUTILIZAMOS TU LÓGICA DE NEGOCIO EXACTA)
        HistorialAcademicoDTO historial = estudianteService.obtenerHistorialAcademico(estudiante.getIdEstudiante());
        double cum = historial.getCumAcumulado() != null ? historial.getCumAcumulado().doubleValue() : 0.0;

        // 3. Materias Inscritas (Ciclo Activo)
        int materias = inscripcionRepository.countMateriasInscritasEnCicloActivo(carnet);

        // 4. Estado Financiero (Ciclo Activo)
        BigDecimal deuda = cargoEstudianteRepository.calcularDeudaPendienteCicloActivo(carnet);
        boolean tieneMora = false;
        String estadoFinanciero = "Al día";

        if (deuda != null && deuda.compareTo(BigDecimal.ZERO) > 0) {
            estadoFinanciero = "Saldo pendiente";
            tieneMora = true;
        }

        return DashboardEstudianteDTO.builder()
                .cum(cum)
                .materiasInscritas(materias)
                .estadoFinanciero(estadoFinanciero)
                .tieneMora(tieneMora)
                .build();
    }
}