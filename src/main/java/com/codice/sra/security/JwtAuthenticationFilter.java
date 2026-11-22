package com.codice.sra.security;

import com.codice.sra.models.SesionUsuario;
import com.codice.sra.repositories.SesionUsuarioRepository;
import com.codice.sra.repositories.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService,
                                   SesionUsuarioRepository sesionUsuarioRepository,
                                   UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.sesionUsuarioRepository = sesionUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
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

                Optional<SesionUsuario> sesionOpt = idSesion != null ? sesionUsuarioRepository.findById(idSesion) : Optional.empty();

                if (sesionOpt.isEmpty() || sesionOpt.get().getFechaFin() != null
                        || !sesionOpt.get().getUsuario().getIdUsuario().equals(idUsuario)) {

                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\": \"Sesion cerrada o invalida\"}");
                    return;
                }


                boolean usuarioActivo = usuarioRepository.findById(idUsuario)
                        .map(usuario -> usuario.getEstadoUsuario() != null
                                && "ACTIVO".equalsIgnoreCase(usuario.getEstadoUsuario().getEstadoUsuario()))
                        .orElse(false);

                if (!usuarioActivo) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\":\"La cuenta se encuentra bloqueada o inactiva\"}");
                    return;
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