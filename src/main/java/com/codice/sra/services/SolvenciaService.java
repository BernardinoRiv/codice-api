package com.codice.sra.services;

import com.codice.sra.dtos.SolvenciaEstudianteDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolvenciaService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private CargoEstudianteRepository cargoEstudianteRepository;

    @Autowired
    private PagoCargoRepository pagoCargoRepository;

    public List<SolvenciaEstudianteDTO> obtenerSolvenciaPorGrupo(Long idGrupo) {
        List<Inscripcion> inscripciones = inscripcionRepository.findByGrupoIdGrupo(idGrupo);

        return inscripciones.stream().map(inscripcion -> {
            Matricula matricula = inscripcion.getMatricula();
            Estudiante estudiante = matricula.getEstudiante();
            Persona persona = estudiante.getPersona();

            // Calcular total de cargos
            List<CargoEstudiante> cargos = cargoEstudianteRepository.findByMatriculaIdMatricula(matricula.getIdMatricula());
            BigDecimal totalCargos = cargos.stream()
                    .map(CargoEstudiante::getMontoTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calcular total pagado
            BigDecimal totalPagado = BigDecimal.ZERO;
            for (CargoEstudiante cargo : cargos) {
                List<PagoCargo> pagos = pagoCargoRepository.findByCargoIdCargoEstudiante(cargo.getIdCargoEstudiante());
                BigDecimal pagadoCargo = pagos.stream()
                        .map(PagoCargo::getMontoAplicado)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalPagado = totalPagado.add(pagadoCargo);
            }

            BigDecimal saldoPendiente = totalCargos.subtract(totalPagado);

            // Determinar solvencia: debe estar MATRICULADA y sin saldo pendiente
            Boolean esSolvente = matricula.getEstadoMatricula().getEstadoMatricula().equals("MATRICULADA")
                    && saldoPendiente.compareTo(BigDecimal.ZERO) <= 0;

            return new SolvenciaEstudianteDTO(
                    inscripcion.getIdInscripcion(),
                    estudiante.getCarnet(),
                    persona.getNombres() + " " + persona.getApellidos(),
                    matricula.getEstadoMatricula().getEstadoMatricula(),
                    totalCargos,
                    totalPagado,
                    saldoPendiente,
                    esSolvente
            );
        }).collect(Collectors.toList());
    }

    public Boolean verificarSolvencia(Long idInscripcion) {
        Inscripcion inscripcion = inscripcionRepository.findById(idInscripcion)
                .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

        Matricula matricula = inscripcion.getMatricula();

        // Verificar estado de matrícula
        if (!matricula.getEstadoMatricula().getEstadoMatricula().equals("MATRICULADA")) {
            return false;
        }

        // Calcular saldo pendiente
        List<CargoEstudiante> cargos = cargoEstudianteRepository.findByMatriculaIdMatricula(matricula.getIdMatricula());
        BigDecimal totalCargos = cargos.stream()
                .map(CargoEstudiante::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagado = BigDecimal.ZERO;
        for (CargoEstudiante cargo : cargos) {
            List<PagoCargo> pagos = pagoCargoRepository.findByCargoIdCargoEstudiante(cargo.getIdCargoEstudiante());
            BigDecimal pagadoCargo = pagos.stream()
                    .map(PagoCargo::getMontoAplicado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalPagado = totalPagado.add(pagadoCargo);
        }

        return totalPagado.compareTo(totalCargos) >= 0;
    }
}