
package com.codice.sra.repositories;

import com.codice.sra.dtos.DashboardDTO.SeccionDetalleProjection;
import com.codice.sra.dtos.DashboardDTO.SeccionCarreraProjection;
import com.codice.sra.dtos.DashboardDTO.HistoricoSeccionesProjection;
import com.codice.sra.dtos.DashboardDTO.AsistenciaSemanalProjection;
import com.codice.sra.dtos.DashboardDTO.DistribucionProjection;
import com.codice.sra.dtos.DashboardDTO.CarreraMatriculaProjection;
import com.codice.sra.dtos.DashboardDTO.MateriaCarreraProjection;
import com.codice.sra.dtos.DashboardDTO.HistoricoMatriculaProjection;
import com.codice.sra.models.MvDashboardGerencial;
import com.codice.sra.dtos.DashboardDTO.DocenteAsignacionProjection;
import com.codice.sra.dtos.DashboardDTO.HistoricoDocentesProjection;
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

    // ============================================================
    // FASE 2: DOCENTES (consultas independientes, con filtro sede)
    // ============================================================

    @Query(value = """
            SELECT tc.tipo_contratacion AS nombre,
                   COUNT(DISTINCT d.id_docente) AS cantidad
            FROM grupos g
            JOIN ciclos c ON c.id_ciclo = g.id_ciclo
            JOIN docentes d ON d.id_docente = g.id_docente
            JOIN estados_docente ed ON ed.id_estado_docente = d.id_estado_docente
            JOIN estados_grupo eg ON eg.id_estado_grupo = g.id_estado_grupo
            JOIN tipos_contratacion_docente tc ON tc.id_tipo_contratacion = d.id_tipo_contratacion
            WHERE c.codigo_ciclo = :codigoCiclo
              AND (:idSede IS NULL OR g.id_sede = :idSede)
              AND UPPER(ed.estado_docente) = 'ACTIVO'
              AND UPPER(eg.estado_grupo) = 'ABIERTO'
            GROUP BY tc.tipo_contratacion
            ORDER BY tc.tipo_contratacion
            """, nativeQuery = true)
    List<DistribucionProjection> getContratosDocentesDashboard(
            @Param("codigoCiclo") String codigoCiclo, @Param("idSede") Long idSede);

    @Query(value = """
            SELECT c.codigo_ciclo AS "codigoCiclo",
                   COUNT(DISTINCT d.id_docente) FILTER
                       (WHERE tc.tipo_contratacion = 'TIEMPO COMPLETO') AS "tiempoCompleto",
                   COUNT(DISTINCT d.id_docente) FILTER
                       (WHERE tc.tipo_contratacion = 'POR HORAS') AS "porHoras"
            FROM ciclos c
            LEFT JOIN grupos g ON g.id_ciclo = c.id_ciclo
                AND (:idSede IS NULL OR g.id_sede = :idSede)
                AND g.id_estado_grupo IN
                    (SELECT id_estado_grupo FROM estados_grupo WHERE UPPER(estado_grupo) = 'ABIERTO')
            LEFT JOIN docentes d ON d.id_docente = g.id_docente
                AND d.id_estado_docente IN
                    (SELECT id_estado_docente FROM estados_docente WHERE UPPER(estado_docente) = 'ACTIVO')
            LEFT JOIN tipos_contratacion_docente tc
                ON tc.id_tipo_contratacion = d.id_tipo_contratacion
            GROUP BY c.id_ciclo, c.codigo_ciclo, c.anio, c.numero_ciclo
            ORDER BY c.anio, c.numero_ciclo
            """, nativeQuery = true)
    List<HistoricoDocentesProjection> getHistoricoDocentesDashboard(@Param("idSede") Long idSede);

    // Todas las materias/grupos asignados a docentes del ciclo y sede.
    @Query(value = """
            SELECT NULL::bigint AS "idCarrera", NULL::varchar AS "nombreCarrera",
                   d.id_docente AS "idDocente",
                   CONCAT_WS(' ', p.nombres, p.apellidos) AS "nombreDocente",
                   tc.tipo_contratacion AS "tipoContratacion",
                   mat.id_materia AS "idMateria", mat.nombre_materia AS "nombreMateria",
                   g.id_grupo AS "idGrupo"
            FROM grupos g
            JOIN ciclos c ON c.id_ciclo = g.id_ciclo
            JOIN docentes d ON d.id_docente = g.id_docente
            JOIN personas p ON p.id_persona = d.id_persona
            JOIN materias mat ON mat.id_materia = g.id_materia
            JOIN tipos_contratacion_docente tc ON tc.id_tipo_contratacion = d.id_tipo_contratacion
            JOIN estados_docente ed ON ed.id_estado_docente = d.id_estado_docente
            JOIN estados_grupo eg ON eg.id_estado_grupo = g.id_estado_grupo
            WHERE c.codigo_ciclo = :codigoCiclo
              AND (:idSede IS NULL OR g.id_sede = :idSede)
              AND UPPER(ed.estado_docente) = 'ACTIVO'
              AND UPPER(eg.estado_grupo) = 'ABIERTO'
            ORDER BY "nombreDocente", "nombreMateria", "idGrupo"
            """, nativeQuery = true)
    List<DocenteAsignacionProjection> getAsignacionesDocentesDashboard(
            @Param("codigoCiclo") String codigoCiclo, @Param("idSede") Long idSede);

    // Carrera real de alumnos con inscripción activa en cada grupo.
    @Query(value = """
            SELECT DISTINCT car.id_carrera AS "idCarrera",
                   car.nombre_carrera AS "nombreCarrera",
                   d.id_docente AS "idDocente",
                   CONCAT_WS(' ', p.nombres, p.apellidos) AS "nombreDocente",
                   tc.tipo_contratacion AS "tipoContratacion",
                   mat.id_materia AS "idMateria", mat.nombre_materia AS "nombreMateria",
                   g.id_grupo AS "idGrupo"
            FROM inscripciones i
            JOIN matriculas m ON m.id_matricula = i.id_matricula
            JOIN estudiantes_carreras ec ON ec.id_estudiante_carrera = m.id_estudiante_carrera
            JOIN carreras_sedes cs ON cs.id_carrera_sede = ec.id_carrera_sede
            JOIN carreras car ON car.id_carrera = cs.id_carrera
            JOIN grupos g ON g.id_grupo = i.id_grupo AND g.id_ciclo = m.id_ciclo
            JOIN ciclos c ON c.id_ciclo = g.id_ciclo
            JOIN docentes d ON d.id_docente = g.id_docente
            JOIN personas p ON p.id_persona = d.id_persona
            JOIN materias mat ON mat.id_materia = g.id_materia
            JOIN tipos_contratacion_docente tc ON tc.id_tipo_contratacion = d.id_tipo_contratacion
            JOIN estados_docente ed ON ed.id_estado_docente = d.id_estado_docente
            JOIN estados_grupo eg ON eg.id_estado_grupo = g.id_estado_grupo
            WHERE c.codigo_ciclo = :codigoCiclo
              AND (:idSede IS NULL OR (g.id_sede = :idSede AND cs.id_sede = :idSede))
              AND m.id_estado_matricula = 2 AND i.id_estado_inscripcion = 1
              AND UPPER(ed.estado_docente) = 'ACTIVO'
              AND UPPER(eg.estado_grupo) = 'ABIERTO'
            ORDER BY "nombreCarrera", "nombreDocente", "nombreMateria", "idGrupo"
            """, nativeQuery = true)
    List<DocenteAsignacionProjection> getAsignacionesDocentesCarreraDashboard(
            @Param("codigoCiclo") String codigoCiclo, @Param("idSede") Long idSede);


    // FASE 3: SECCIONES ABIERTAS
    // La ocupacion se calcula por grupo, sin multiplicar filas por horarios.
    @Query(value = """
        WITH base AS (
            SELECT g.id_grupo, g.codigo_grupo, g.id_materia, g.id_docente,
                   g.id_plantilla, COALESCE(g.cupo_maximo, 0) AS cupo_maximo,
                   mat.nombre_materia
            FROM grupos g
            JOIN ciclos ci ON ci.id_ciclo = g.id_ciclo
            JOIN materias mat ON mat.id_materia = g.id_materia
            JOIN estados_grupo eg ON eg.id_estado_grupo = g.id_estado_grupo
            WHERE ci.codigo_ciclo = :codigoCiclo
              AND (:idSede IS NULL OR g.id_sede = :idSede)
              AND UPPER(eg.estado_grupo) = 'ABIERTO'
        ), sesiones AS (
            SELECT b.id_grupo, h.hora_inicio FROM base b
            JOIN horarios h ON h.id_grupo = b.id_grupo
            UNION ALL
            SELECT b.id_grupo, phd.hora_inicio FROM base b
            JOIN plantillas_horarios_detalle phd ON phd.id_plantilla = b.id_plantilla
            WHERE NOT EXISTS (SELECT 1 FROM horarios h WHERE h.id_grupo = b.id_grupo)
        ), jornadas AS (
            SELECT id_grupo,
                CASE
                    WHEN BOOL_OR(hora_inicio < TIME '17:00')
                     AND BOOL_OR(hora_inicio >= TIME '17:00') THEN 'MIXTA'
                    WHEN BOOL_OR(hora_inicio >= TIME '17:00') THEN 'NOCTURNA'
                    WHEN BOOL_OR(hora_inicio < TIME '17:00') THEN 'DIURNA'
                    ELSE 'SIN HORARIO'
                END AS jornada
            FROM sesiones GROUP BY id_grupo
        ), ocupacion AS (
            SELECT i.id_grupo, COUNT(DISTINCT i.id_inscripcion) AS inscritos
            FROM inscripciones i
            JOIN matriculas m ON m.id_matricula = i.id_matricula
            JOIN base b ON b.id_grupo = i.id_grupo
            WHERE i.id_estado_inscripcion = 1 AND m.id_estado_matricula = 2
            GROUP BY i.id_grupo
        )
        SELECT b.id_grupo AS "idGrupo", b.codigo_grupo AS "codigoGrupo",
               b.id_materia AS "idMateria", b.nombre_materia AS "materia",
               CONCAT_WS(' ', p.nombres, p.apellidos) AS "docente",
               COALESCE(j.jornada, 'SIN HORARIO') AS "jornada",
               COALESCE(o.inscritos, 0) AS "inscritos",
               b.cupo_maximo AS "cupoMaximo"
        FROM base b
        LEFT JOIN docentes d ON d.id_docente = b.id_docente
        LEFT JOIN personas p ON p.id_persona = d.id_persona
        LEFT JOIN jornadas j ON j.id_grupo = b.id_grupo
        LEFT JOIN ocupacion o ON o.id_grupo = b.id_grupo
        ORDER BY b.nombre_materia, b.codigo_grupo, b.id_grupo
        """, nativeQuery = true)
    List<SeccionDetalleProjection> getDetalleSeccionesDashboard(
            @Param("codigoCiclo") String codigoCiclo, @Param("idSede") Long idSede);

    @Query(value = """
        SELECT ci.codigo_ciclo AS "codigoCiclo", COUNT(g.id_grupo) AS "cantidad"
        FROM ciclos ci
        LEFT JOIN grupos g ON g.id_ciclo = ci.id_ciclo
          AND (:idSede IS NULL OR g.id_sede = :idSede)
          AND g.id_estado_grupo IN (
              SELECT eg.id_estado_grupo FROM estados_grupo eg
              WHERE UPPER(eg.estado_grupo) = 'ABIERTO'
          )
        GROUP BY ci.id_ciclo, ci.codigo_ciclo, ci.anio, ci.numero_ciclo
        ORDER BY ci.anio, ci.numero_ciclo
        """, nativeQuery = true)
    List<HistoricoSeccionesProjection> getHistoricoSeccionesDashboard(@Param("idSede") Long idSede);

    // Una misma seccion puede asociarse a varias carreras por sus alumnos.
    @Query(value = """
        SELECT DISTINCT g.id_grupo AS "idGrupo", car.id_carrera AS "idCarrera",
               car.nombre_carrera AS "nombreCarrera", mat.id_materia AS "idMateria",
               mat.nombre_materia AS "nombreMateria"
        FROM grupos g
        JOIN ciclos ci ON ci.id_ciclo = g.id_ciclo
        JOIN estados_grupo eg ON eg.id_estado_grupo = g.id_estado_grupo
        JOIN materias mat ON mat.id_materia = g.id_materia
        JOIN inscripciones i ON i.id_grupo = g.id_grupo AND i.id_estado_inscripcion = 1
        JOIN matriculas m ON m.id_matricula = i.id_matricula
          AND m.id_ciclo = g.id_ciclo AND m.id_estado_matricula = 2
        JOIN estudiantes_carreras ec ON ec.id_estudiante_carrera = m.id_estudiante_carrera
        JOIN carreras_sedes cs ON cs.id_carrera_sede = ec.id_carrera_sede
        JOIN carreras car ON car.id_carrera = cs.id_carrera
        WHERE ci.codigo_ciclo = :codigoCiclo
          AND UPPER(eg.estado_grupo) = 'ABIERTO'
          AND (:idSede IS NULL OR (g.id_sede = :idSede AND cs.id_sede = :idSede))
        ORDER BY "nombreCarrera", "nombreMateria", "idGrupo"
        """, nativeQuery = true)
    List<SeccionCarreraProjection> getSeccionesPorCarreraDashboard(
            @Param("codigoCiclo") String codigoCiclo, @Param("idSede") Long idSede);

}
