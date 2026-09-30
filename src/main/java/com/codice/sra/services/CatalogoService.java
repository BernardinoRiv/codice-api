package com.codice.sra.services;

import com.codice.sra.dtos.CatalogoDTO;
import com.codice.sra.enums.TipoCatalogo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final JdbcTemplate jdbcTemplate;

    public CatalogoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Consulta catálogos planos (sin dependencia de clave foránea).
     * Ejemplo: departamentos, tipos_documento, sedes.
     */
    @Cacheable(value = "catalogos", key = "#catalogo.name()")
    public List<CatalogoDTO> obtenerPorTipo(TipoCatalogo catalogo) {
        return obtenerPorTipoYPadre(catalogo, null);
    }

    /**
     * Consulta catálogos admitiendo filtro jerárquico opcional.
     * Ejemplo: distritos filtrados por id_departamento.
     * La clave de caché es compuesta para evitar colisiones entre departamentos.
     */
    @Cacheable(value = "catalogos", key = "#catalogo.name() + '_' + (#padreId != null ? #padreId : 'ALL')")
    public List<CatalogoDTO> obtenerPorTipoYPadre(TipoCatalogo catalogo, Long padreId) {
        if (catalogo == null) {
            throw new IllegalArgumentException("El tipo de catálogo no puede ser nulo.");
        }

        // Si el catálogo requiere padre obligatorio y no se envió, protegemos la consulta
        if (catalogo.tienePadre() && padreId != null) {
            String sqlConPadre = String.format(
                    "SELECT %s AS id, %s AS nombre FROM public.%s WHERE %s = ? ORDER BY %s ASC",
                    catalogo.getColumnaId(),
                    catalogo.getColumnaNombre(),
                    catalogo.getTabla(),
                    catalogo.getColumnaPadre(),
                    catalogo.getColumnaNombre()
            );

            return jdbcTemplate.query(
                    sqlConPadre,
                    (rs, rowNum) -> new CatalogoDTO(rs.getLong("id"), rs.getString("nombre")),
                    padreId
            );
        }

        // Consulta estándar para catálogos planos
        String sqlPlano = String.format(
                "SELECT %s AS id, %s AS nombre FROM public.%s ORDER BY %s ASC",
                catalogo.getColumnaId(),
                catalogo.getColumnaNombre(),
                catalogo.getTabla(),
                catalogo.getColumnaNombre()
        );

        return jdbcTemplate.query(
                sqlPlano,
                (rs, rowNum) -> new CatalogoDTO(rs.getLong("id"), rs.getString("nombre"))
        );
    }

    /**
     * Recupera múltiples catálogos en una sola petición HTTP para optimizar
     * la carga inicial de vistas en el frontend.
     */
    public Map<String, List<CatalogoDTO>> obtenerMultiples(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<CatalogoDTO>> resultado = new LinkedHashMap<>();
        for (String slug : slugs) {
            TipoCatalogo tipo = TipoCatalogo.fromSlug(slug);
            resultado.put(slug, obtenerPorTipo(tipo));
        }
        return resultado;
    }
}