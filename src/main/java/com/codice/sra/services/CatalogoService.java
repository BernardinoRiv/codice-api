package com.codice.sra.services;

import com.codice.sra.dtos.CatalogoDTO;
import com.codice.sra.enums.TipoCatalogo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final JdbcTemplate jdbcTemplate;

    // Inyección por constructor (Clean Architecture)
    public CatalogoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(value = "catalogos", key = "#catalogo.name()")
    public List<CatalogoDTO> obtenerPorTipo(TipoCatalogo catalogo) {
        String sql = String.format(
                "SELECT %s AS id, %s AS nombre FROM public.%s ORDER BY %s ASC",
                catalogo.getColumnaId(),
                catalogo.getColumnaNombre(),
                catalogo.getTabla(),
                catalogo.getColumnaNombre()
        );

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new CatalogoDTO(rs.getLong("id"), rs.getString("nombre"))
        );
    }

    public Map<String, List<CatalogoDTO>> obtenerMultiples(List<String> slugs) {
        Map<String, List<CatalogoDTO>> resultado = new LinkedHashMap<>();
        for (String slug : slugs) {
            TipoCatalogo tipo = TipoCatalogo.fromSlug(slug);
            resultado.put(slug, obtenerPorTipo(tipo));
        }
        return resultado;
    }
}