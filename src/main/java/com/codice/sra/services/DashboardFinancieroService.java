package com.codice.sra.services;

import com.codice.sra.repositories.CicloRepository;
import com.codice.sra.repositories.DashboardFinancieroRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardFinancieroService {

    private final DashboardFinancieroRepository repository;
    private final CicloRepository cicloRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public JsonNode obtenerDashboardCompleto(String codigoCiclo, Long idSede) {

        String ciclo = resolverCodigoCiclo(codigoCiclo);
        String resultado = repository.obtenerDashboardCompleto(ciclo, idSede);

        if (resultado == null || resultado.isBlank()) {
            throw new IllegalStateException("No se pudo obtener el dashboard financiero.");
        }

        try {
            return objectMapper.readTree(resultado);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error al procesar los datos financieros.", e);
        }
    }

    private String resolverCodigoCiclo(String codigoCiclo) {

        if (codigoCiclo != null && !codigoCiclo.isBlank()) {
            return codigoCiclo.trim();
        }

        return cicloRepository.findCicloPorEstadoActivo()
                .map(ciclo -> ciclo.getCodigoCiclo())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe un ciclo activo."
                        )
                );
    }
}