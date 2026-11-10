package com.codice.sra.dtos.DashboardDTO;

import java.util.List;

public record DashboardDocentesDTO(
        long docentesActivos,
        List<Distribucion> distribucionContratos,
        List<Historico> historicoDocentes,
        List<Carrera> docentesPorCarrera,
        List<Docente> detalleDocentes
) {
    public record Distribucion(String nombre, long cantidad) {}
    public record Historico(String codigoCiclo, long tiempoCompleto, long porHoras) {}
    public record Carrera(Long idCarrera, String nombre, long cantidad, List<Docente> docentes) {}
    public record Docente(Long idDocente, String nombre, String tipoContratacion,
                          long cantidadMaterias, List<Materia> materias) {}
    public record Materia(Long idMateria, String nombre, long cantidadGrupos) {}
}
