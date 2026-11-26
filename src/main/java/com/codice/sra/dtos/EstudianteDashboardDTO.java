package com.codice.sra.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EstudianteDashboardDTO(
        Kpis kpis,
        List<ClaseHoy> clasesHoy,
        List<EvaluacionProxima> evaluacionesProximas,
        List<EvolucionCum> evolucionCum,
        List<AsistenciaCiclo> asistenciasPorCiclo,
        Long idCicloActivo
) {
    public record Kpis(BigDecimal cumActual, BigDecimal variacionCum, int materiasAprobadas,
                       int totalMateriasPensum, BigDecimal porcentajeAvance, int uvAprobadas,
                       BigDecimal porcentajeAsistenciaCicloActual, String estadoAsistenciaGlobal,
                       int cuotasPendientes, int cuotasEnMora, boolean enMora, BigDecimal montoMoraTotal,
                       BigDecimal saldoPendienteCiclo, boolean solvente, String proximoPagoConcepto,
                       BigDecimal proximoPagoMonto, LocalDate proximoPagoFechaVencimiento,
                       boolean proximoPagoVencido) {}

    public record ClaseHoy(Long idGrupo, String codigoMateria, String nombreMateria, String codigoGrupo,
                           LocalTime horaInicio, LocalTime horaFin, String aula, boolean enCurso) {}

    public record EvaluacionProxima(Long idEvaluacion, String codigoMateria, String nombreMateria,
                                    String tipoEvaluacion, Integer numeroEvaluacion, LocalDateTime fechaInicio,
                                    LocalDateTime fechaFin, boolean activaHoy, long diasRestantes) {}

    public record EvolucionCum(String codigoCiclo, Integer anio, Integer numeroCiclo,
                               BigDecimal promedioCiclo, BigDecimal cumAcumulado,
                               int materiasAprobadasCiclo) {}

    public record AsistenciaCiclo(Long idCiclo, String codigoCiclo, String estadoCiclo,
                                  List<AsistenciaMateria> materias) {}

    public record AsistenciaMateria(String codigoMateria, String nombreMateria, String codigoGrupo,
                                    int totalSesiones, int asistidas, int tardanzas, int justificadas,
                                    int inasistencias, BigDecimal porcentajeAsistencia, String nivelAlerta) {}
}
