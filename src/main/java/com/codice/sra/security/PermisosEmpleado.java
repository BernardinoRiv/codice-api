package com.codice.sra.security;

import com.codice.sra.models.Empleado;
import com.codice.sra.repositories.EmpleadoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
public class PermisosEmpleado {

    private final EmpleadoRepository empleadoRepository;

    public PermisosEmpleado(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    public boolean puede(Authentication authentication, String... vistasRequeridas) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long idUsuario)
                || authentication.getAuthorities().stream()
                    .noneMatch(autoridad -> "ROLE_EMPLEADO".equals(autoridad.getAuthority()))) {
            return false;
        }

        return empleadoRepository.findByUsuarioIdUsuario(idUsuario)
                .map(empleado -> !Collections.disjoint(vistas(empleado), List.of(vistasRequeridas)))
                .orElse(false);
    }

    static List<String> vistas(Empleado empleado) {
        if (empleado.getArea() == null || empleado.getCargo() == null) {
            return List.of();
        }

        String area = normalizar(empleado.getArea().getArea());
        String cargo = normalizar(empleado.getCargo().getCargo());

        return switch (area + "/" + cargo) {
            case "FINANZAS/CAJERO", "COLECTURIA/CAJERO" -> List.of("pagos", "facturas");
            case "FINANZAS/ASISTENTE FINANCIERO" -> List.of("facturas");
            default -> List.of();
        };
    }

    private static String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }
}