package com.codice.sra.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Long obtenerIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("No existe un usuario autenticado en el contexto de seguridad.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long idUsuario) {
            return idUsuario;
        }

        // Si en tu configuración el ID viene como String en el name/principal:
        try {
            return Long.parseLong(principal.toString());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("El identificador del usuario en el token no tiene un formato numérico válido: " + principal);
        }
    }
}