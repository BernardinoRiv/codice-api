package com.codice.sra.dtos;

import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;

public record OfertaMatriculaEstudianteDTO(
        Long idCiclo,
        String codigoCiclo,
        BigDecimal montoMatricula,
        String conceptoMatricula,
        BigDecimal montoPrimeraCuota,
        String conceptoPrimeraCuota,
        BigDecimal montoTotalInicial,
        boolean matriculaYaPagada,
        String numeroFacturaExistente,
        Long idPagoExistente,
        List<MateriaDTO> materias
) {
    public record MateriaDTO(Long idMateria, String codigoMateria, String nombreMateria,
                             Integer cicloRecomendado, java.math.BigDecimal uvs,
                             boolean habilitada, List<String> motivosBloqueo,
                             List<GrupoDTO> gruposDisponibles) {}

    public record GrupoDTO(Long idGrupo, String codigoGrupo, String docente,
                           Integer capacidadMaxima, Integer cuposDisponibles,
                           List<HorarioDTO> horarios) {}

    public record HorarioDTO(String diaSemana, LocalTime horaInicio, LocalTime horaFin,
                             String aula) {}
}
