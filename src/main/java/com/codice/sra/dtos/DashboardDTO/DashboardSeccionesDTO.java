package com.codice.sra.dtos.DashboardDTO;

import java.util.List;

public record DashboardSeccionesDTO(
        long seccionesAbiertas,
        long cuposTotales,
        long inscritos,
        long cuposDisponibles,
        double ocupacionGlobal,
        List<Jornada> distribucionJornadas,
        List<Historico> historicoSecciones,
        Ocupacion estadoOcupacion,
        List<Carrera> gruposPorCarrera,
        List<Grupo> detalleGrupos
) {
    public record Jornada(String nombre, long cantidad) {}
    public record Historico(String codigoCiclo, long cantidad) {}
    public record Ocupacion(long llenos, long disponibles, long vacios) {}
    public record Materia(Long idMateria, String nombre, long cantidadGrupos) {}
    public record Carrera(Long idCarrera, String nombre, long cantidadGrupos, List<Materia> materias) {}
    public record Grupo(Long idGrupo, String codigoGrupo, Long idMateria, String materia,
                        String docente, String jornada, long inscritos, long cupoMaximo,
                        long cuposDisponibles, double porcentajeOcupacion) {}
}
