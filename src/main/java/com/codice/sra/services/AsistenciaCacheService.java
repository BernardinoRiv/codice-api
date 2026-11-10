package com.codice.sra.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
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
        int numeroAleatorio = 1000 + new Random().nextInt(9000);
        String nuevoToken = String.valueOf(numeroAleatorio);
        tokensValidos.put(nuevoToken, new TokenInfo(idClase, LocalDateTime.now()));
        return nuevoToken;
    }

    public boolean registrarAsistencia(String token, Long idEstudiante) {
        TokenInfo info = tokensValidos.get(token);
        if (info == null) return false;

        if (info.getFechaCreacion().plusSeconds(10).isBefore(LocalDateTime.now())) {
            tokensValidos.remove(token);
            return false;
        }

        Map<Long, LocalDateTime> presentes = asistenciasPorClase.get(info.getIdClase());
        if (presentes != null) {
            if (presentes.containsKey(idEstudiante)) {
                throw new RuntimeException("Ya has registrado tu asistencia para esta clase.");
            }
            presentes.put(idEstudiante, LocalDateTime.now());
            return true;
        }
        return false;
    }

    public void registrarAsistenciaManual(Long idClase, Long idEstudiante) {
        Map<Long, LocalDateTime> presentes = asistenciasPorClase.get(idClase);
        if (presentes != null) {
            presentes.put(idEstudiante, LocalDateTime.now());
        }
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
        LocalDateTime limite = LocalDateTime.now().minusSeconds(10);
        tokensValidos.entrySet().removeIf(entry -> entry.getValue().getFechaCreacion().isBefore(limite));
    }

    public Long obtenerIdClasePorToken(String token) {
        TokenInfo info = tokensValidos.get(token);
        return info != null ? info.getIdClase() : null;
    }
}