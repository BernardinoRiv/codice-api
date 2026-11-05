package com.codice.sra.services;

import com.codice.sra.models.Ciclo;
import com.codice.sra.repositories.CicloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CicloMatriculaValidatorService {

    private final CicloRepository cicloRepository;

    public static final int DIAS_MAXIMOS_MATRICULA_EXTRAORDINARIA = 30;
    public static final int DIAS_GRACIA_MATRICULA_ORDINARIA = 15;

    public record EvaluacionCicloMatricula(
            Ciclo ciclo,
            boolean esExtraordinaria,
            String mensaje
    ) {}

    /**
     * Determina el ciclo lectivo hábil para la admisión en línea validando la máquina de estados.
     */
    public EvaluacionCicloMatricula resolverCicloHabilParaMatricula() {
        LocalDate hoy = LocalDate.now();

        // PRIORIDAD 1: Ciclo en estado PLANIFICADO (Aprobado por Junta Directiva para admisión regular)
        var cicloPlanificadoOpt = cicloRepository.findCicloPlanificadoParaMatricula();
        if (cicloPlanificadoOpt.isPresent()) {
            Ciclo ciclo = cicloPlanificadoOpt.get();
            log.info("Ciclo PLANIFICADO localizado: [{}]. Admisión anticipada habilitada.", ciclo.getCodigoCiclo());
            return new EvaluacionCicloMatricula(
                    ciclo,
                    false,
                    "Período ordinario de pre-matrícula habilitado para el ciclo " + ciclo.getCodigoCiclo()
            );
        }

        // PRIORIDAD 2: Ciclo en estado ACTIVO (Condicionado a los primeros 30 días tras fecha_inicio)
        var cicloActivoOpt = cicloRepository.findCicloActivo();
        if (cicloActivoOpt.isPresent()) {
            Ciclo ciclo = cicloActivoOpt.get();
            LocalDate fechaInicio = ciclo.getFechaInicio();

            // Si el ciclo está marcado ACTIVO administrativamente pero aún no llega la fecha física de inicio
            if (hoy.isBefore(fechaInicio)) {
                return new EvaluacionCicloMatricula(
                        ciclo,
                        false,
                        "Período ordinario previo al inicio lectivo del ciclo " + ciclo.getCodigoCiclo()
                );
            }

            long diasTranscurridos = ChronoUnit.DAYS.between(fechaInicio, hoy);

            if (diasTranscurridos <= DIAS_MAXIMOS_MATRICULA_EXTRAORDINARIA) {
                boolean aplicaExtraordinaria = diasTranscurridos > DIAS_GRACIA_MATRICULA_ORDINARIA;
                log.info("Admisión sobre ciclo ACTIVO [{}]. Días desde apertura: {}. Requiere arancel extraordinario: {}",
                        ciclo.getCodigoCiclo(), diasTranscurridos, aplicaExtraordinaria);

                return new EvaluacionCicloMatricula(
                        ciclo,
                        aplicaExtraordinaria,
                        aplicaExtraordinaria
                                ? "Período extraordinario de matrícula (Día " + diasTranscurridos + " de " + DIAS_MAXIMOS_MATRICULA_EXTRAORDINARIA + ")."
                                : "Matrícula regular en curso."
                );
            } else {
                throw new IllegalStateException(
                        "El período de admisiones para el ciclo " + ciclo.getCodigoCiclo() +
                                " ha finalizado. Han transcurrido " + diasTranscurridos + " días desde su inicio (Límite: " +
                                DIAS_MAXIMOS_MATRICULA_EXTRAORDINARIA + " días)."
                );
            }
        }

        throw new IllegalStateException("Actualmente no existen ciclos académicos habilitados para matrícula.");
    }
}