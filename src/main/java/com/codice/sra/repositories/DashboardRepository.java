
package com.codice.sra.repositories;

import com.codice.sra.dtos.DashboardDTO.AsistenciaSemanalProjection;
import com.codice.sra.dtos.DashboardDTO.DistribucionProjection;
import com.codice.sra.dtos.DashboardDTO.CarreraMatriculaProjection;
import com.codice.sra.dtos.DashboardDTO.MateriaCarreraProjection;
import com.codice.sra.dtos.DashboardDTO.HistoricoMatriculaProjection;
import com.codice.sra.models.MvDashboardGerencial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardRepository extends JpaRepository<MvDashboardGerencial, Long> {

    Optional<MvDashboardGerencial> findByCodigoCiclo(String codigoCiclo);

    // ============================================================
    // FASE 1: MATRÍCULA ACTIVA
    // ============================================================

    // Total de estudiantes con matrícula confirmada.
    @Query(value = """
            SELECT COUNT(DISTINCT m.id_estudiante)
            FROM matriculas m
            JOIN ciclos ci ON m.id_ciclo = ci.id_ciclo
            JOIN estudiantes_carreras ec
                ON m.id_estudiante_carrera = ec.id_estudiante_carrera
            JOIN carreras_sedes cs
                ON ec.id_carrera_sede = cs.id_carrera_sede
            WHERE ci.codigo_ciclo = :codigoCiclo
              AND m.id_estado_matricula = 2
              AND (:idSede IS NULL OR cs.id_sede = :idSede)
            """, nativeQuery = true)
    Long contarMatriculasActivas(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("idSede") Long idSede
    );

    // Estudiantes matriculados con al menos una materia activa.
    @Query(value = """
            SELECT COUNT(DISTINCT m.id_estudiante)
            FROM matriculas m
            JOIN ciclos ci ON m.id_ciclo = ci.id_ciclo
            JOIN estudiantes_carreras ec
                ON m.id_estudiante_carrera = ec.id_estudiante_carrera
            JOIN carreras_sedes cs
                ON ec.id_carrera_sede = cs.id_carrera_sede
            WHERE ci.codigo_ciclo = :codigoCiclo
              AND m.id_estado_matricula = 2
              AND (:idSede IS NULL OR cs.id_sede = :idSede)
              AND EXISTS (
                  SELECT 1
                  FROM inscripciones i
                  JOIN grupos g ON i.id_grupo = g.id_grupo
                  WHERE i.id_matricula = m.id_matricula
                    AND i.id_estado_inscripcion = 1
                    AND g.id_ciclo = m.id_ciclo
              )
            """, nativeQuery = true)
    Long contarMatriculadosConMaterias(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("idSede") Long idSede
    );

    // Estudiantes matriculados sin materias activas.
    @Query(value = """
            SELECT COUNT(DISTINCT m.id_estudiante)
            FROM matriculas m
            JOIN ciclos ci ON m.id_ciclo = ci.id_ciclo
            JOIN estudiantes_carreras ec
                ON m.id_estudiante_carrera = ec.id_estudiante_carrera
            JOIN carreras_sedes cs
                ON ec.id_carrera_sede = cs.id_carrera_sede
            WHERE ci.codigo_ciclo = :codigoCiclo
              AND m.id_estado_matricula = 2
              AND (:idSede IS NULL OR cs.id_sede = :idSede)
              AND NOT EXISTS (
                  SELECT 1
                  FROM inscripciones i
                  JOIN grupos g ON i.id_grupo = g.id_grupo
                  WHERE i.id_matricula = m.id_matricula
                    AND i.id_estado_inscripcion = 1
                    AND g.id_ciclo = m.id_ciclo
              )
            """, nativeQuery = true)
    Long contarMatriculadosSinMaterias(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("idSede") Long idSede
    );

    // Distribución de matrícula confirmada por facultad.
    @Query(value = """
            SELECT f.nombre_facultad AS nombre,
                   COUNT(DISTINCT m.id_estudiante) AS cantidad
            FROM matriculas m
            JOIN ciclos ci ON m.id_ciclo = ci.id_ciclo
            JOIN estudiantes_carreras ec
                ON m.id_estudiante_carrera = ec.id_estudiante_carrera
            JOIN carreras_sedes cs
                ON ec.id_carrera_sede = cs.id_carrera_sede
            JOIN carreras c ON cs.id_carrera = c.id_carrera
            JOIN facultades f ON c.id_facultad = f.id_facultad
            WHERE ci.codigo_ciclo = :codigoCiclo
              AND m.id_estado_matricula = 2
              AND (:idSede IS NULL OR cs.id_sede = :idSede)
            GROUP BY f.id_facultad, f.nombre_facultad
            ORDER BY cantidad DESC
            """, nativeQuery = true)
    List<DistribucionProjection> getMatriculaPorFacultad(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("idSede") Long idSede
    );

    // Distribución de matrícula confirmada por carrera.
    @Query(value = """
            SELECT c.id_carrera AS idCarrera,
                   c.nombre_carrera AS nombre,
                   COUNT(DISTINCT m.id_estudiante) AS cantidad
            FROM matriculas m
            JOIN ciclos ci ON m.id_ciclo = ci.id_ciclo
            JOIN estudiantes_carreras ec
                ON m.id_estudiante_carrera = ec.id_estudiante_carrera
            JOIN carreras_sedes cs
                ON ec.id_carrera_sede = cs.id_carrera_sede
            JOIN carreras c ON cs.id_carrera = c.id_carrera
            WHERE ci.codigo_ciclo = :codigoCiclo
              AND m.id_estado_matricula = 2
              AND (:idSede IS NULL OR cs.id_sede = :idSede)
            GROUP BY c.id_carrera, c.nombre_carrera
            ORDER BY cantidad DESC
            """, nativeQuery = true)
    List<CarreraMatriculaProjection> getMatriculaPorCarrera(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("idSede") Long idSede
    );

    // Demanda por materia: cantidad de inscripciones activas.
    @Query(value = """
            SELECT mat.nombre_materia AS nombre,
                   COUNT(i.id_inscripcion) AS cantidad
            FROM inscripciones i
            JOIN grupos g ON i.id_grupo = g.id_grupo
            JOIN materias mat ON g.id_materia = mat.id_materia
            JOIN matriculas m ON i.id_matricula = m.id_matricula
            JOIN ciclos ci ON m.id_ciclo = ci.id_ciclo
            JOIN estudiantes_carreras ec
                ON m.id_estudiante_carrera = ec.id_estudiante_carrera
            JOIN carreras_sedes cs
                ON ec.id_carrera_sede = cs.id_carrera_sede
            WHERE ci.codigo_ciclo = :codigoCiclo
              AND m.id_estado_matricula = 2
              AND i.id_estado_inscripcion = 1
              AND g.id_ciclo = m.id_ciclo
              AND (:idSede IS NULL OR cs.id_sede = :idSede)
            GROUP BY mat.nombre_materia
            ORDER BY cantidad DESC
            """, nativeQuery = true)
    List<DistribucionProjection> getMatriculaPorMateria(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("idSede") Long idSede
    );


    // Histórico: todos los ciclos, incluso aquellos sin matrícula confirmada.
    // La sede se aplica al conteo, no a la lista de ciclos.
    @Query(value = """
            SELECT ci.codigo_ciclo AS codigoCiclo,
                   COUNT(DISTINCT CASE
                       WHEN m.id_estado_matricula = 2
                        AND (:idSede IS NULL OR cs.id_sede = :idSede)
                       THEN m.id_estudiante
                   END) AS cantidad
            FROM ciclos ci
            LEFT JOIN matriculas m ON m.id_ciclo = ci.id_ciclo
            LEFT JOIN estudiantes_carreras ec
                ON m.id_estudiante_carrera = ec.id_estudiante_carrera
            LEFT JOIN carreras_sedes cs
                ON ec.id_carrera_sede = cs.id_carrera_sede
            GROUP BY ci.id_ciclo, ci.codigo_ciclo
            """, nativeQuery = true)
    List<HistoricoMatriculaProjection> getHistoricoMatricula(
            @Param("idSede") Long idSede
    );

    // Demanda por materia de una carrera concreta.
    // DISTINCT evita contar dos veces al estudiante inscrito
    // en distintos grupos de la misma asignatura.
    @Query(value = """
            SELECT mat.id_materia AS idMateria,
                   mat.nombre_materia AS nombre,
                   COUNT(DISTINCT m.id_estudiante) AS cantidad
            FROM inscripciones i
            JOIN matriculas m ON i.id_matricula = m.id_matricula
            JOIN grupos g ON i.id_grupo = g.id_grupo
            JOIN materias mat ON g.id_materia = mat.id_materia
            JOIN ciclos ci ON m.id_ciclo = ci.id_ciclo
            JOIN estudiantes_carreras ec
                ON m.id_estudiante_carrera = ec.id_estudiante_carrera
            JOIN carreras_sedes cs
                ON ec.id_carrera_sede = cs.id_carrera_sede
            WHERE ci.codigo_ciclo = :codigoCiclo
              AND m.id_estado_matricula = 2
              AND i.id_estado_inscripcion = 1
              AND g.id_ciclo = m.id_ciclo
              AND cs.id_carrera = :idCarrera
              AND (:idSede IS NULL OR cs.id_sede = :idSede)
            GROUP BY mat.id_materia, mat.nombre_materia
            ORDER BY cantidad DESC, mat.nombre_materia
            """, nativeQuery = true)
    List<MateriaCarreraProjection> getMateriasPorCarrera(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("idSede") Long idSede,
            @Param("idCarrera") Long idCarrera
    );

    // ============================================================
    // DOCENTES
    // ============================================================

    @Query(value = "SELECT f.codigo_facultad AS nombre, COUNT(DISTINCT g.id_docente) AS cantidad " +
            "FROM grupos g JOIN materias m ON g.id_materia = m.id_materia " +
            "JOIN pensum_materias pm ON m.id_materia = pm.id_materia " +
            "JOIN pensum p ON pm.id_pensum = p.id_pensum " +
            "JOIN carreras c ON p.id_carrera = c.id_carrera " +
            "JOIN facultades f ON c.id_facultad = f.id_facultad " +
            "JOIN ciclos ci ON g.id_ciclo = ci.id_ciclo " +
            "WHERE ci.codigo_ciclo = :codigoCiclo AND g.id_estado_grupo = 1 " +
            "GROUP BY f.codigo_facultad", nativeQuery = true)
    List<DistribucionProjection> getDocentesPorFacultad(
            @Param("codigoCiclo") String codigoCiclo
    );

    @Query(value = "SELECT tc.tipo_contratacion AS nombre, COUNT(DISTINCT d.id_docente) AS cantidad " +
            "FROM docentes d JOIN grupos g ON d.id_docente = g.id_docente " +
            "JOIN tipos_contratacion_docente tc ON d.id_tipo_contratacion = tc.id_tipo_contratacion " +
            "JOIN ciclos ci ON g.id_ciclo = ci.id_ciclo " +
            "WHERE ci.codigo_ciclo = :codigoCiclo AND g.id_estado_grupo = 1 " +
            "GROUP BY tc.tipo_contratacion", nativeQuery = true)
    List<DistribucionProjection> getDocentesPorContrato(
            @Param("codigoCiclo") String codigoCiclo
    );

    // ============================================================
    // ASISTENCIA
    // ============================================================

    @Query(value = "SELECT " +
            "CASE WHEN :modo = 'diario' THEN TO_CHAR(c.fecha_clase, 'YYYY-MM-DD') " +
            "WHEN :modo = 'mensual' THEN TO_CHAR(c.fecha_clase, 'YYYY-MM') " +
            "ELSE 'Sem ' || TO_CHAR(c.fecha_clase, 'WW') END AS semana, " +
            "ROUND((SUM(CASE WHEN a.id_estado_asistencia = 1 THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(a.id_asistencia), 0), 1) AS porcentaje " +
            "FROM asistencias a JOIN clases c ON a.id_clase = c.id_clase " +
            "JOIN grupos g ON c.id_grupo = g.id_grupo " +
            "JOIN ciclos ci ON g.id_ciclo = ci.id_ciclo " +
            "WHERE ci.codigo_ciclo = :codigoCiclo AND g.id_estado_grupo = 1 " +
            "GROUP BY 1 ORDER BY MIN(c.fecha_clase) ASC", nativeQuery = true)
    List<AsistenciaSemanalProjection> getAsistenciaPromedioTemporal(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("modo") String modo
    );

    @Query(value = "SELECT f.codigo_facultad AS nombre, " +
            "ROUND((SUM(CASE WHEN a.id_estado_asistencia = 1 THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(a.id_asistencia), 0), 1) AS porcentaje " +
            "FROM asistencias a JOIN clases cl ON a.id_clase = cl.id_clase " +
            "JOIN grupos g ON cl.id_grupo = g.id_grupo " +
            "JOIN materias m ON g.id_materia = m.id_materia " +
            "JOIN pensum_materias pm ON m.id_materia = pm.id_materia " +
            "JOIN pensum p ON pm.id_pensum = p.id_pensum " +
            "JOIN carreras c ON p.id_carrera = c.id_carrera " +
            "JOIN facultades f ON c.id_facultad = f.id_facultad " +
            "JOIN ciclos ci ON g.id_ciclo = ci.id_ciclo " +
            "WHERE ci.codigo_ciclo = :codigoCiclo " +
            "GROUP BY f.codigo_facultad ORDER BY porcentaje DESC", nativeQuery = true)
    List<DistribucionProjection> getAsistenciaPorFacultad(
            @Param("codigoCiclo") String codigoCiclo
    );
}
