package com.codice.sra.security;

import com.codice.sra.models.SesionUsuario;
import com.codice.sra.repositories.SesionUsuarioRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SesionUsuarioRepository sesionUsuarioRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService,
                                   SesionUsuarioRepository sesionUsuarioRepository) {
        this.jwtService = jwtService;
        this.sesionUsuarioRepository = sesionUsuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String userEmail = null;

        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (ExpiredJwtException e) {
            logger.warn("Token JWT expirado detectado");
        } catch (Exception e) {
            logger.warn("Token JWT inválido detectado");
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            if (jwtService.isTokenValid(jwt, userEmail)) {
                String rol = jwtService.extractClaim(jwt, claims -> claims.get("rol", String.class));
                Long idUsuario = jwtService.extractClaim(jwt, claims -> claims.get("idUsuario", Long.class));
                Long idSesion = jwtService.extractClaim(jwt, claims -> claims.get("idSesion", Long.class));

                // VALIDACIÓN CRÍTICA CORREGIDA:
                // Verificamos que ESTA sesión específica (la del token de la computadora) siga activa
                if (idSesion != null) {
                    Optional<SesionUsuario> sesionOpt = sesionUsuarioRepository.findById(idSesion);

                    // Si la sesión no existe o YA TIENE fecha_fin, se rechaza la petición inmediatamente
                    if (sesionOpt.isEmpty() || sesionOpt.get().getFechaFin() != null) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"error\": \"Sesion cerrada o invalida\"}");
                        return;
                    }
                }

                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol));

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        idUsuario,
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}