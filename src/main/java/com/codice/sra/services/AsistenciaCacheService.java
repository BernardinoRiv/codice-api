package com.codice.sra.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AsistenciaCacheService {

    private final Map<String, TokenInfo> tokensValidos = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, LocalDateTime>> asistenciasPorClase = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class TokenInfo {
        private Long idClase;
        private LocalDateTime fechaCreacion;
    }

    public void inicializarClase(Long idClase) {
        asistenciasPorClase.put(idClase, new ConcurrentHashMap<>());
    }

    public String generarTokenRotativo(Long idClase) {
        limpiarTokensExpirados();
        String nuevoToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        tokensValidos.put(nuevoToken, new TokenInfo(idClase, LocalDateTime.now()));
        return nuevoToken;
    }

    public boolean registrarAsistencia(String token, Long idEstudiante) {
        TokenInfo info = tokensValidos.get(token);
        if (info == null) return false;

        if (info.getFechaCreacion().plusSeconds(20).isBefore(LocalDateTime.now())) {
            tokensValidos.remove(token);
            return false;
        }

        Map<Long, LocalDateTime> presentes = asistenciasPorClase.get(info.getIdClase());
        if (presentes != null) {
            // Evita que el estudiante se registre múltiples veces
            if (presentes.containsKey(idEstudiante)) {
                throw new RuntimeException("Ya has registrado tu asistencia para esta clase.");
            }
            presentes.put(idEstudiante, LocalDateTime.now());
            return true;
        }
        return false;
    }

    public Map<Long, LocalDateTime> obtenerPresentes(Long idClase) {
        return asistenciasPorClase.getOrDefault(idClase, new ConcurrentHashMap<>());
    }

    public Map<Long, LocalDateTime> finalizarClaseYObtenerPresentes(Long idClase) {
        Map<Long, LocalDateTime> presentes = asistenciasPorClase.remove(idClase);
        tokensValidos.entrySet().removeIf(entry -> entry.getValue().getIdClase().equals(idClase));
        return presentes != null ? presentes : Map.of();
    }

    private void limpiarTokensExpirados() {
        LocalDateTime limite = LocalDateTime.now().minusSeconds(20);
        tokensValidos.entrySet().removeIf(entry -> entry.getValue().getFechaCreacion().isBefore(limite));
    }
    // Agregar al final de AsistenciaCacheService.java
    public Long obtenerIdClasePorToken(String token) {
        TokenInfo info = tokensValidos.get(token);
        return info != null ? info.getIdClase() : null;
    }
}