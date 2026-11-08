
package com.codice.sra.repositories;

import com.codice.sra.models.MvDashboardGerencial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardFinancieroRepository
        extends JpaRepository<MvDashboardGerencial, Long> {

    @Query(value = """
        WITH ciclo AS (
            SELECT
                id_ciclo,
                codigo_ciclo,
                fecha_inicio,
                fecha_fin
            FROM ciclos
            WHERE codigo_ciclo = :codigoCiclo
        ),

        -- 1. CARGOS GENERADOS PARA EL CICLO Y SEDE
        cargos AS (
            SELECT
                ce.id_cargo_estudiante AS id_cargo,
                m.id_estudiante,
                ca.id_carrera,
                ca.nombre_carrera,
                f.id_facultad,
                f.nombre_facultad,
                ce.id_estudiante_beneficio,
                ce.monto_base,
                ce.monto_descuento,
                ce.monto_recargo,
                ce.monto_total,
                ce.fecha_generacion::date AS fecha_generacion,
                ce.fecha_vencimiento,

                GREATEST(
                    ce.fecha_generacion::date,
                    ce.fecha_vencimiento
                ) AS fecha_incorporacion

            FROM cargos_estudiante ce

            JOIN matriculas m
                ON m.id_matricula = ce.id_matricula

            JOIN ciclo ci
                ON ci.id_ciclo = m.id_ciclo

            JOIN estudiantes_carreras ec
                ON ec.id_estudiante_carrera = m.id_estudiante_carrera

            JOIN carreras_sedes cs
                ON cs.id_carrera_sede = ec.id_carrera_sede

            JOIN carreras ca
                ON ca.id_carrera = cs.id_carrera

            JOIN facultades f
                ON f.id_facultad = ca.id_facultad

            WHERE ce.id_estado_cargo <> 3
              AND m.id_estado_matricula <> 3
              AND (:idSede IS NULL OR cs.id_sede = :idSede)
        ),

        -- 2. PAGOS APROBADOS APLICADOS A LOS CARGOS
        aplicaciones AS (
            SELECT
                pc.id_cargo,
                p.fecha_pago::date AS fecha_pago,
                pc.monto_aplicado

            FROM pagos_cargos pc

            JOIN pagos p
                ON p.id_pago = pc.id_pago

            JOIN cargos c
                ON c.id_cargo = pc.id_cargo

            WHERE p.id_estado_pago = 2
        ),

        -- 3. TOTAL PAGADO POR CARGO
        pagos_por_cargo AS (
            SELECT
                id_cargo,
                SUM(monto_aplicado) AS pagado,
                MAX(fecha_pago) AS ultima_fecha_pago

            FROM aplicaciones
            GROUP BY id_cargo
        ),

        -- 4. SALDOS PENDIENTES Y VENCIDOS
        saldos AS (
            SELECT
                c.*,

                COALESCE(pc.pagado, 0) AS pagado,
                pc.ultima_fecha_pago,

                GREATEST(
                    c.monto_total - COALESCE(pc.pagado, 0),
                    0
                ) AS pendiente,

                CASE
                    WHEN c.fecha_vencimiento < CURRENT_DATE
                    THEN GREATEST(
                        c.monto_total - COALESCE(pc.pagado, 0),
                        0
                    )
                    ELSE 0
                END AS vencido

            FROM cargos c

            LEFT JOIN pagos_por_cargo pc
                ON pc.id_cargo = c.id_cargo
        ),

        -- 5. RESUMEN FINANCIERO GENERAL
        resumen AS (
            SELECT
                COALESCE(SUM(monto_total), 0) AS esperado,
                COALESCE(SUM(pagado), 0) AS real,
                COALESCE(SUM(pendiente), 0) AS pendiente,
                COALESCE(SUM(vencido), 0) AS vencido

            FROM saldos
        ),

        -- 6. SEMANAS DEL CICLO ACADEMICO
        semanas AS (
            SELECT
                ROW_NUMBER() OVER (
                    ORDER BY gs.fecha
                )::integer AS numero,

                gs.fecha::date AS inicio,

                LEAST(
                    gs.fecha::date + 6,
                    ci.fecha_fin
                ) AS fin

            FROM ciclo ci

            CROSS JOIN LATERAL generate_series(
                ci.fecha_inicio::timestamp,
                ci.fecha_fin::timestamp,
                INTERVAL '7 days'
            ) AS gs(fecha)
        ),

        -- 7. PROYECCION SEMANAL ACUMULADA
        evolucion AS (
            SELECT
                s.numero,

                COALESCE((
                    SELECT SUM(c.monto_total)
                    FROM cargos c
                    WHERE c.fecha_incorporacion <= s.fin
                ), 0) AS esperado,

                COALESCE((
                    SELECT SUM(a.monto_aplicado)
                    FROM aplicaciones a
                    WHERE a.fecha_pago <= s.fin
                ), 0) AS real,

                COALESCE((
                    SELECT SUM(a.monto_aplicado)
                    FROM aplicaciones a
                    WHERE a.fecha_pago >= s.inicio
                      AND a.fecha_pago <= s.fin
                ), 0) AS semanal

            FROM semanas s
        ),

        -- 8. MESES DEL CICLO ACADEMICO
        meses_ciclo AS (
            SELECT
                gs.mes::date AS inicio_mes,

                LEAST(
                    (
                        gs.mes
                        + INTERVAL '1 month'
                        - INTERVAL '1 day'
                    )::date,
                    ci.fecha_fin
                ) AS fin_mes

            FROM ciclo ci

            CROSS JOIN LATERAL generate_series(
                DATE_TRUNC(
                    'month',
                    ci.fecha_inicio::timestamp
                ),
                DATE_TRUNC(
                    'month',
                    ci.fecha_fin::timestamp
                ),
                INTERVAL '1 month'
            ) AS gs(mes)
        ),

        -- 9. PROYECCION MENSUAL ACUMULADA
        evolucion_mensual AS (
            SELECT
                mc.inicio_mes,

                COALESCE((
                    SELECT SUM(c.monto_total)
                    FROM cargos c
                    WHERE c.fecha_incorporacion <= mc.fin_mes
                ), 0) AS esperado,

                COALESCE((
                    SELECT SUM(a.monto_aplicado)
                    FROM aplicaciones a
                    WHERE a.fecha_pago <= mc.fin_mes
                ), 0) AS real

            FROM meses_ciclo mc
        ),

        -- 10. MOROSIDAD AGRUPADA POR CARRERA
        mora_carreras AS (
            SELECT
                id_facultad,
                nombre_facultad,
                id_carrera,
                nombre_carrera,

                SUM(vencido) AS saldo_vencido,

                COUNT(DISTINCT id_estudiante)
                    AS estudiantes_mora

            FROM saldos

            WHERE vencido > 0

            GROUP BY
                id_facultad,
                nombre_facultad,
                id_carrera,
                nombre_carrera
        ),

        -- 11. MOROSIDAD AGRUPADA POR FACULTAD
        mora_facultades AS (
            SELECT
                id_facultad,
                nombre_facultad,
                SUM(saldo_vencido) AS saldo_vencido

            FROM mora_carreras

            GROUP BY
                id_facultad,
                nombre_facultad
        ),

        -- 12. SITUACION DE PAGO POR ESTUDIANTE
        situacion_estudiante AS (
            SELECT
                id_estudiante,

                BOOL_AND(
                    pendiente = 0
                    AND ultima_fecha_pago <= fecha_vencimiento
                ) AS todos_puntuales,

                BOOL_AND(
                    pendiente = 0
                ) AS ciclo_pagado,

                MAX(
                    CASE
                        WHEN pendiente > 0
                             AND fecha_vencimiento < CURRENT_DATE
                        THEN CURRENT_DATE - fecha_vencimiento

                        WHEN pendiente = 0
                             AND ultima_fecha_pago > fecha_vencimiento
                        THEN ultima_fecha_pago - fecha_vencimiento

                        ELSE 0
                    END
                ) AS max_atraso,

                MAX(fecha_vencimiento) AS ultimo_vencimiento,
                MAX(ultima_fecha_pago) AS ultimo_pago

            FROM saldos

            GROUP BY id_estudiante
        ),

        -- 13. PERFIL DE COMPORTAMIENTO DE PAGO
        comportamiento AS (
            SELECT
                COUNT(*) FILTER (
                    WHERE todos_puntuales
                ) AS puntuales,

                COUNT(*) FILTER (
                    WHERE ciclo_pagado
                      AND ultimo_pago < ultimo_vencimiento
                ) AS pago_completo,

                COUNT(*) FILTER (
                    WHERE max_atraso BETWEEN 1 AND 29
                ) AS retrasos_dias,

                COUNT(*) FILTER (
                    WHERE max_atraso >= 30
                ) AS retrasos_meses

            FROM situacion_estudiante
        ),

        -- 14. IMPACTO FINANCIERO DE BECAS
        becas_resumen AS (
            SELECT
                COUNT(DISTINCT id_estudiante)
                    FILTER (
                        WHERE id_estudiante_beneficio IS NOT NULL
                          AND COALESCE(monto_descuento, 0) > 0
                    ) AS becarios,

                COALESCE(
                    SUM(
                        CASE
                            WHEN id_estudiante_beneficio IS NOT NULL
                            THEN COALESCE(monto_descuento, 0)
                            ELSE 0
                        END
                    ),
                    0
                ) AS monto_becado,

                COALESCE(
                    SUM(
                        COALESCE(monto_base, 0)
                        + COALESCE(monto_recargo, 0)
                    ),
                    0
                ) AS bruto

            FROM cargos
        ),

        -- 15. ESTUDIANTES CON OBLIGACIONES VENCIDAS
        estudiantes_morosos AS (
            SELECT
                s.id_estudiante,

                CONCAT_WS(
                    ' ',
                    pe.nombres,
                    pe.apellidos
                ) AS nombre,

                s.nombre_carrera,

                SUM(s.vencido) AS deuda,

                MAX(
                    CURRENT_DATE - s.fecha_vencimiento
                ) FILTER (
                    WHERE s.vencido > 0
                ) AS dias_atraso

            FROM saldos s

            JOIN estudiantes e
                ON e.id_estudiante = s.id_estudiante

            JOIN personas pe
                ON pe.id_persona = e.id_persona

            WHERE s.vencido > 0

            GROUP BY
                s.id_estudiante,
                pe.nombres,
                pe.apellidos,
                s.nombre_carrera
        ),

        -- 16. PAGOS PUNTUALES Y TARDIOS POR MES
        pagos_mensuales AS (
            SELECT
                DATE_TRUNC(
                    'month',
                    a.fecha_pago
                )::date AS mes,

                COALESCE(
                    SUM(a.monto_aplicado) FILTER (
                        WHERE a.fecha_pago <= c.fecha_vencimiento
                    ),
                    0
                ) AS puntuales,

                COALESCE(
                    SUM(a.monto_aplicado) FILTER (
                        WHERE a.fecha_pago > c.fecha_vencimiento
                    ),
                    0
                ) AS tardios

            FROM aplicaciones a

            JOIN cargos c
                ON c.id_cargo = a.id_cargo

            GROUP BY
                DATE_TRUNC('month', a.fecha_pago)::date
        ),

        -- 17. MESES PARA LA TENDENCIA DE PAGOS
        meses_dashboard AS (
            SELECT
                gs.mes::date AS mes

            FROM ciclo ci

            CROSS JOIN LATERAL generate_series(
                DATE_TRUNC(
                    'month',
                    ci.fecha_inicio::timestamp
                ),
                DATE_TRUNC(
                    'month',
                    ci.fecha_fin::timestamp
                ),
                INTERVAL '1 month'
            ) AS gs(mes)
        ),

        -- 18. TENDENCIA MENSUAL DE PAGOS
        tendencia_mensual AS (
            SELECT
                md.mes,

                COALESCE(
                    pm.puntuales,
                    0
                ) AS puntuales,

                COALESCE(
                    pm.tardios,
                    0
                ) AS tardios

            FROM meses_dashboard md

            LEFT JOIN pagos_mensuales pm
                ON pm.mes = md.mes
        ),

        -- 19. TOP 5 CARRERAS CON MAYOR MOROSIDAD
        top_carreras_morosas AS (
            SELECT
                s.id_carrera,
                s.nombre_carrera,

                SUM(s.vencido) AS saldo_vencido,

                COUNT(DISTINCT s.id_estudiante)
                    AS estudiantes_mora

            FROM saldos s

            WHERE s.vencido > 0

            GROUP BY
                s.id_carrera,
                s.nombre_carrera

            ORDER BY
                saldo_vencido DESC

            LIMIT 5
        )

        -- RESPUESTA JSON COMPLETA DEL DASHBOARD
        SELECT json_build_object(

            'codigoCiclo', :codigoCiclo,
            'idSede', :idSede,

            -- RESUMEN FINANCIERO
            'recaudacionEsperada', r.esperado,
            'recaudacionReal', r.real,
            'saldoPendiente', r.pendiente,
            'saldoVencido', r.vencido,

            'porcentajeCumplimiento',
                CASE
                    WHEN r.esperado = 0 THEN 0
                    ELSE ROUND(
                        r.real * 100 / r.esperado,
                        2
                    )
                END,

            -- PROYECCION SEMANAL
            'proyeccionSemanal',
                COALESCE((
                    SELECT json_agg(
                        json_build_object(
                            'semana', 'S' || numero,
                            'recaudacionEsperada', esperado,
                            'recaudacionReal', real
                        )
                        ORDER BY numero
                    )
                    FROM evolucion
                ), '[]'::json),

            -- PROYECCION MENSUAL
            'proyeccionMensual',
                COALESCE((
                    SELECT json_agg(
                        json_build_object(
                            'mes', TO_CHAR(
                                inicio_mes,
                                'YYYY-MM'
                            ),
                            'recaudacionEsperada', esperado,
                            'recaudacionReal', real
                        )
                        ORDER BY inicio_mes
                    )
                    FROM evolucion_mensual
                ), '[]'::json),

            -- TENDENCIA MENSUAL DE PAGOS
            'tendenciaMensualPagos',
                COALESCE((
                    SELECT json_agg(
                        json_build_object(
                            'mes', TO_CHAR(mes, 'YYYY-MM'),
                            'puntuales', puntuales,
                            'tardios', tardios
                        )
                        ORDER BY mes
                    )
                    FROM tendencia_mensual
                ), '[]'::json),

            -- TOP 5 CARRERAS MOROSAS
            'topCarrerasMorosas',
                COALESCE((
                    SELECT json_agg(
                        json_build_object(
                            'idCarrera', id_carrera,
                            'nombre', nombre_carrera,
                            'saldoVencido', saldo_vencido,
                            'estudiantesEnMora', estudiantes_mora
                        )
                        ORDER BY saldo_vencido DESC
                    )
                    FROM top_carreras_morosas
                ), '[]'::json),

            -- PAGOS SEMANALES (COMPATIBILIDAD)
            'pagosSemanales',
                COALESCE((
                    SELECT json_agg(
                        json_build_object(
                            'semana', 'S' || numero,
                            'monto', semanal
                        )
                        ORDER BY numero
                    )
                    FROM evolucion
                ), '[]'::json),

            -- MOROSIDAD POR FACULTAD Y CARRERA
            'morosidadFacultades',
                COALESCE((
                    SELECT json_agg(
                        json_build_object(
                            'idFacultad', mf.id_facultad,
                            'nombre', mf.nombre_facultad,
                            'saldoVencido', mf.saldo_vencido,

                            'carreras',
                                COALESCE((
                                    SELECT json_agg(
                                        json_build_object(
                                            'idCarrera', mc.id_carrera,
                                            'nombre', mc.nombre_carrera,
                                            'saldoVencido', mc.saldo_vencido,
                                            'estudiantesEnMora',
                                                mc.estudiantes_mora
                                        )
                                        ORDER BY mc.saldo_vencido DESC
                                    )
                                    FROM mora_carreras mc
                                    WHERE mc.id_facultad = mf.id_facultad
                                ), '[]'::json)
                        )
                        ORDER BY mf.saldo_vencido DESC
                    )
                    FROM mora_facultades mf
                ), '[]'::json),

            -- COMPORTAMIENTO DE PAGO
            'comportamientoPago',
                (
                    SELECT json_build_object(
                        'puntuales', puntuales,
                        'pagoCompleto', pago_completo,
                        'retrasosDias', retrasos_dias,
                        'retrasosMeses', retrasos_meses
                    )
                    FROM comportamiento
                ),

            -- BECAS
            'becas',
                (
                    SELECT json_build_object(
                        'becariosActivos', becarios,
                        'montoBecado', monto_becado,
                        'cobertura',
                            CASE
                                WHEN bruto = 0 THEN 0
                                ELSE ROUND(
                                    monto_becado * 100 / bruto,
                                    2
                                )
                            END
                    )
                    FROM becas_resumen
                ),

            -- ESTUDIANTES MOROSOS
            'estudiantesMorosos',
                COALESCE((
                    SELECT json_agg(
                        json_build_object(
                            'idEstudiante', id_estudiante,
                            'nombre', nombre,
                            'carrera', nombre_carrera,
                            'deuda', deuda,
                            'diasAtraso', dias_atraso
                        )
                        ORDER BY deuda DESC
                    )
                    FROM estudiantes_morosos
                ), '[]'::json)

        )::text

        FROM resumen r
        """, nativeQuery = true)
    String obtenerDashboardCompleto(
            @Param("codigoCiclo") String codigoCiclo,
            @Param("idSede") Long idSede
    );
}
