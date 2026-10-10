package com.codice.sra.services;

import com.codice.sra.models.Auditoria;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.AuditoriaRepository;
import com.codice.sra.repositories.UsuarioRepository;
import com.codice.sra.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HttpServletRequest httpServletRequest;
    private final SecurityUtils securityUtils;

    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            UsuarioRepository usuarioRepository,
                            HttpServletRequest httpServletRequest,
                            SecurityUtils securityUtils) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.httpServletRequest = httpServletRequest;
        this.securityUtils = securityUtils;
    }

    /**
     * Se ejecuta dentro de la misma transacción (o inicia una) para garantizar atomicidad.
     */

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarEvento(String accion, String tabla, Long idRegistroAfectado, String detalle) {
        // 1. Extraer ID del actor autenticado desde el contexto del token
        Long idUsuario = securityUtils.obtenerIdUsuarioAutenticado();

        // 2. Resolver proxy JPA (Cero impacto de I/O en PostgreSQL)
        Usuario usuarioProxy = usuarioRepository.getReferenceById(idUsuario);

        Auditoria log = new Auditoria();
        log.setUsuario(usuarioProxy);
        log.setAccion(accion);
        log.setTabla(tabla);
        log.setIdRegistro(idRegistroAfectado);
        log.setDescripcion(detalle);
        log.setFechaHora(OffsetDateTime.now());
        log.setDireccionIp(obtenerIpCliente());

        auditoriaRepository.save(log);
    }

    private String obtenerIpCliente() {
        if (httpServletRequest == null) {
            return "127.0.0.1";
        }
        String xfHeader = httpServletRequest.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return httpServletRequest.getRemoteAddr();
    }
}