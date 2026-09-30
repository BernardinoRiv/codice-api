package com.codice.sra.services;

import com.codice.sra.dtos.AuthLoginRequestDTO;
import com.codice.sra.dtos.AuthLoginResponseDTO;
import com.codice.sra.dtos.CambiarContrasenaRequestDTO;
import com.codice.sra.dtos.CambiarContrasenaResponseDTO;
import com.codice.sra.dtos.SesionUsuarioDTO;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.UsuarioRepository;
import com.codice.sra.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SesionUsuarioService sesionUsuarioService;

    @Autowired
    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SesionUsuarioService sesionUsuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sesionUsuarioService = sesionUsuarioService;
    }

    public AuthLoginResponseDTO login(AuthLoginRequestDTO request, HttpServletRequest httpRequest) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoInstitucional(request.getCorreoInstitucional());

        if (usuarioOpt.isEmpty()) {
            return new AuthLoginResponseDTO(null, null, null, null, 0, null, "Credenciales inválidas", false);
        }

        Usuario usuario = usuarioOpt.get();

        OffsetDateTime ultimoAccesoActual = usuario.getUltimoAcceso();

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(OffsetDateTime.now())) {
            return new AuthLoginResponseDTO(
                    null, null, null, null,
                    usuario.getIntentosFallidos(),
                    usuario.getBloqueadoHasta(),
                    "Usuario bloqueado por múltiples intentos fallidos. Intente más tarde.",
                    false
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            manejarIntentoFallido(usuario);

            int nuevosIntentos = usuario.getIntentosFallidos();
            OffsetDateTime nuevoBloqueo = nuevosIntentos >= 3 ? OffsetDateTime.now().plusMinutes(15) : null;

            return new AuthLoginResponseDTO(
                    null, null, null, null,
                    nuevosIntentos,
                    nuevoBloqueo,
                    "Credenciales inválidas",
                    false
            );
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        String ip = obtenerIpCliente(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        SesionUsuarioDTO sesion = sesionUsuarioService.registrarSesion(usuario.getIdUsuario(), ip, userAgent, true);

        String jwtToken = jwtService.generateToken(usuario, sesion.getIdSesion());
        String nombreCompleto = usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos();

        return new AuthLoginResponseDTO(
                jwtToken,
                nombreCompleto,
                usuario.getRol().getRol(),
                usuario.getUltimoAcceso(),
                0,
                null,
                "Login exitoso",
                true
        );
    }

    @Transactional
    public CambiarContrasenaResponseDTO cambiarContrasena(Long idUsuario, CambiarContrasenaRequestDTO request) {
        if (!request.getNuevaContrasena().equals(request.getConfirmarNuevaContrasena())) {
            return new CambiarContrasenaResponseDTO(false, "Las nuevas contraseñas no coinciden");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getContrasenaActual(), usuario.getPasswordHash())) {
            return new CambiarContrasenaResponseDTO(false, "La contraseña actual es incorrecta");
        }

        if (passwordEncoder.matches(request.getNuevaContrasena(), usuario.getPasswordHash())) {
            return new CambiarContrasenaResponseDTO(false, "La nueva contraseña debe ser diferente a la actual");
        }

        if (request.getNuevaContrasena().length() < 8) {
            return new CambiarContrasenaResponseDTO(false, "La contraseña debe tener al menos 8 caracteres");
        }

        String nuevaContrasenaHash = passwordEncoder.encode(request.getNuevaContrasena());
        usuario.setPasswordHash(nuevaContrasenaHash);
        usuario.setUltimoAcceso(OffsetDateTime.now());

        usuarioRepository.save(usuario);

        return new CambiarContrasenaResponseDTO(true, "Contraseña cambiada exitosamente");
    }

    private void manejarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);

        if (intentos >= 3) {
            usuario.setBloqueadoHasta(OffsetDateTime.now().plusMinutes(15));
        }
        usuarioRepository.save(usuario);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}