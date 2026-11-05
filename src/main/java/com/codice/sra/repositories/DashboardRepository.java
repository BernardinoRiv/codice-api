package com.codice.sra.repositories;

import com.codice.sra.dtos.DashboardDTO.AsistenciaSemanalProjection;
import com.codice.sra.dtos.DashboardDTO.DistribucionProjection;
import com.codice.sra.models.MvDashboardGerencial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardRepository extends JpaRepository<MvDashboardGerencial, Long> {

    // BUENA PRÁCTICA: Zero SQL nativo. Spring lo traduce a:
    // SELECT * FROM mv_dashboard_gerencial WHERE codigo_ciclo = ?
    Optional<MvDashboardGerencial> findByCodigoCiclo(String codigoCiclo);

    // 1. Desglose de docentes activos por Facultad
    @Query(value = "SELECT f.codigo_facultad AS nombre, COUNT(DISTINCT g.id_docente) AS cantidad " +
            "FROM grupos g " +
            "JOIN materias m ON g.id_materia = m.id_materia " +
            "JOIN pensum_materias pm ON m.id_materia = pm.id_materia " +
            "JOIN pensum p ON pm.id_pensum = p.id_pensum " +
            "JOIN carreras c ON p.id_carrera = c.id_carrera " +
            "JOIN facultades f ON c.id_facultad = f.id_facultad " +
            "JOIN ciclos ci ON g.id_ciclo = ci.id_ciclo " +
            "WHERE ci.codigo_ciclo = :codigoCiclo AND g.id_estado_grupo = 1 " +
            "GROUP BY f.codigo_facultad", nativeQuery = true)
    List<DistribucionProjection> getDocentesPorFacultad(@Param("codigoCiclo") String codigoCiclo);

    // 2. Desglose de docentes activos por Tipo de Contrato
    @Query(value = "SELECT tc.tipo_contratacion AS nombre, COUNT(DISTINCT d.id_docente) AS cantidad " +
            "FROM docentes d " +
            "JOIN grupos g ON d.id_docente = g.id_docente " +
            "JOIN tipos_contratacion_docente tc ON d.id_tipo_contratacion = tc.id_tipo_contratacion " +
            "JOIN ciclos ci ON g.id_ciclo = ci.id_ciclo " +
            "WHERE ci.codigo_ciclo = :codigoCiclo AND g.id_estado_grupo = 1 " +
            "GROUP BY tc.tipo_contratacion", nativeQuery = true)
    List<DistribucionProjection> getDocentesPorContrato(@Param("codigoCiclo") String codigoCiclo);

    // 3. Desglose de Asistencia Promedio Semanal
    @Query(value = "SELECT " +
            "'Sem ' || TO_CHAR(c.fecha_clase, 'WW') AS semana, " +
            "ROUND( (SUM(CASE WHEN a.id_estado_asistencia = 1 THEN 1 ELSE 0 END) * 100.0) / NULLIF(COUNT(a.id_asistencia), 0), 1) AS porcentaje " +
            "FROM asistencias a " +
            "JOIN clases c ON a.id_clase = c.id_clase " +
            "JOIN grupos g ON c.id_grupo = g.id_grupo " +
            "JOIN ciclos ci ON g.id_ciclo = ci.id_ciclo " +
            "WHERE ci.codigo_ciclo = :codigoCiclo AND g.id_estado_grupo = 1 " +
            "GROUP BY TO_CHAR(c.fecha_clase, 'WW') " +
            "ORDER BY MIN(c.fecha_clase) ASC " +
            "LIMIT 10", nativeQuery = true)
    List<AsistenciaSemanalProjection> getAsistenciaPromedioSemanal(@Param("codigoCiclo") String codigoCiclo);
}