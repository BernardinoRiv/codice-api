package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.models.RecuperacionClave;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.RecuperacionClaveRepository;
import com.codice.sra.repositories.UsuarioRepository;
import com.codice.sra.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RecuperacionClaveRepository recuperacionClaveRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SesionUsuarioService sesionUsuarioService;
    private final EmailService emailService;

    @Autowired
    public AuthService(UsuarioRepository usuarioRepository,
                       RecuperacionClaveRepository recuperacionClaveRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SesionUsuarioService sesionUsuarioService,
                       EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacionClaveRepository = recuperacionClaveRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sesionUsuarioService = sesionUsuarioService;
        this.emailService = emailService;
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
                ultimoAccesoActual,
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

    @Transactional
    public String solicitarRecuperacionClave(SolicitarRecuperacionDTO request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoInstitucional(request.getCorreoInstitucional());

        if (usuarioOpt.isEmpty()) {
            return "Si el correo está registrado, recibirá un código de recuperación.";
        }

        Usuario usuario = usuarioOpt.get();

        String codigoCrudo = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        String codigoHash = passwordEncoder.encode(codigoCrudo);

        RecuperacionClave recuperacion = new RecuperacionClave();
        recuperacion.setUsuario(usuario);
        recuperacion.setCodigoHash(codigoHash);
        recuperacion.setFechaGeneracion(LocalDateTime.now());
        recuperacion.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        recuperacion.setUsado(false);

        recuperacionClaveRepository.save(recuperacion);

        String nombrePila = usuario.getPersona().getNombres().split(" ")[0];

        emailService.enviarCodigoRecuperacion(usuario.getCorreoInstitucional(), nombrePila, codigoCrudo);

        return "Si el correo está registrado, recibirá un código de recuperación.";
    }

    @Transactional
    public CambiarContrasenaResponseDTO restablecerClave(RestablecerClaveDTO request) {
        if (!request.getNuevaContrasena().equals(request.getConfirmarContrasena())) {
            return new CambiarContrasenaResponseDTO(false, "Las contraseñas no coinciden.");
        }
        if (request.getNuevaContrasena().length() < 8) {
            return new CambiarContrasenaResponseDTO(false, "La contraseña debe tener al menos 8 caracteres.");
        }

        Usuario usuario = usuarioRepository.findByCorreoInstitucional(request.getCorreoInstitucional())
                .orElseThrow(() -> new RuntimeException("Solicitud inválida."));

        List<RecuperacionClave> codigosActivos = recuperacionClaveRepository
                .findByUsuarioAndUsadoFalseAndFechaExpiracionAfter(usuario, LocalDateTime.now());

        RecuperacionClave codigoValido = null;
        for (RecuperacionClave rec : codigosActivos) {
            if (passwordEncoder.matches(request.getCodigo(), rec.getCodigoHash())) {
                codigoValido = rec;
                break;
            }
        }

        if (codigoValido == null) {
            return new CambiarContrasenaResponseDTO(false, "El código de recuperación es inválido o ha expirado.");
        }

        if (passwordEncoder.matches(request.getNuevaContrasena(), usuario.getPasswordHash())) {
            return new CambiarContrasenaResponseDTO(false, "La nueva contraseña no puede ser igual a la anterior.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaContrasena()));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        codigoValido.setUsado(true);
        codigoValido.setFechaUso(LocalDateTime.now());
        recuperacionClaveRepository.save(codigoValido);

        return new CambiarContrasenaResponseDTO(true, "Contraseña restablecida exitosamente. Ya puede iniciar sesión.");
    }
}