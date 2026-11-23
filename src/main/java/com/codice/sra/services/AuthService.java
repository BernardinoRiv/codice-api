package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.security.JwtService;
import com.codice.sra.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final EmpleadoRepository empleadoRepository;
    private final CicloRepository cicloRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;

    @Autowired
    public AuthService(UsuarioRepository usuarioRepository,
                       RecuperacionClaveRepository recuperacionClaveRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SesionUsuarioService sesionUsuarioService,
                       EmailService emailService,
                       EmpleadoRepository empleadoRepository,
                       CicloRepository cicloRepository,
                       AuditoriaRepository auditoriaRepository,
                       SesionUsuarioRepository sesionUsuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacionClaveRepository = recuperacionClaveRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sesionUsuarioService = sesionUsuarioService;
        this.emailService = emailService;
        this.empleadoRepository = empleadoRepository;
        this.cicloRepository = cicloRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.sesionUsuarioRepository = sesionUsuarioRepository;
    }

    @Transactional(readOnly = true)
    public TokenRefreshResponseDTO refresh(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new BadCredentialsException("Se requiere un token Bearer válido.");
        String token = authorization.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            Long idUsuario = jwtService.extractIdUsuario(token);
            Long idSesion = jwtService.extractIdSesion(token);
            if (!jwtService.isTokenValid(token, username) || idUsuario == null || idSesion == null)
                throw new BadCredentialsException("El token expiró o no es válido.");
            SesionUsuario sesion = sesionUsuarioRepository.findById(idSesion)
                    .filter(s -> s.getFechaFin() == null && s.getUsuario().getIdUsuario().equals(idUsuario))
                    .orElseThrow(() -> new BadCredentialsException("La sesión no existe, fue cerrada o no corresponde al usuario."));
            Usuario usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new BadCredentialsException("El usuario de la sesión ya no existe."));
            String rol = usuario.getRol().getRol();
            if (!("ESTUDIANTE".equalsIgnoreCase(rol) || "DOCENTE".equalsIgnoreCase(rol)))
                throw new AccessDeniedException("La renovación está habilitada únicamente para estudiantes y docentes.");
            long expiresIn = jwtService.getExpirationForRole(rol);
            return new TokenRefreshResponseDTO(jwtService.generateToken(usuario, sesion.getIdSesion()), expiresIn);
        } catch (BadCredentialsException | AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadCredentialsException("El token expiró o no es válido.");
        }
    }

    public AuthLoginResponseDTO login(AuthLoginRequestDTO request, HttpServletRequest httpRequest) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoInstitucional(request.getCorreoInstitucional());

        if (usuarioOpt.isEmpty()) {
            return new AuthLoginResponseDTO(null, null, null, null, 0, null, "Credenciales inválidas", false, null, null, null, null);
        }

        Usuario usuario = usuarioOpt.get();

        // =========================================================================
        // NUEVA VALIDACIÓN: Verificar si el usuario está ACTIVO (ID 1 en BD)
        // =========================================================================
        if (usuario.getEstadoUsuario() != null && usuario.getEstadoUsuario().getIdEstadoUsuario() != 1L) {
            String estadoReal = usuario.getEstadoUsuario().getEstadoUsuario().toLowerCase();
            return new AuthLoginResponseDTO(
                    null, null, null, null,
                    0, null,
                    "Acceso denegado. El usuario se encuentra " + estadoReal + ". Contacte a soporte.",
                    false,
                    null, null, null, null // <-- CORRECCIÓN: 4 campos agregados
            );
        }

        OffsetDateTime ultimoAccesoActual = usuario.getUltimoAcceso();

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(OffsetDateTime.now())) {
            return new AuthLoginResponseDTO(
                    null, null, null, null,
                    usuario.getIntentosFallidos(),
                    usuario.getBloqueadoHasta(),
                    "Usuario bloqueado temporalmente por múltiples intentos fallidos. Intente más tarde.",
                    false,
                    null, null, null, null // <-- CORRECCIÓN: 4 campos agregados
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
                    false,
                    null,
                    null,
                    null,
                    null
            );
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        String ip = obtenerIpCliente(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        SesionUsuarioDTO sesion = sesionUsuarioService.registrarSesion(usuario.getIdUsuario(), ip, userAgent, true);

        String jwtToken = jwtService.generateToken(usuario, sesion.getIdSesion());
        String nombreCompleto = usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos();

        Long idCargo = null;
        String rol = usuario.getRol().getRol();

        String nombreCargo = null;

        if ("EMPLEADO".equalsIgnoreCase(rol) || "FINANZAS".equalsIgnoreCase(rol) || "ADMINISTRADOR".equalsIgnoreCase(rol)) {
            Cargo cargo = empleadoRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                    .map(Empleado::getCargo)
                    .orElse(null);

            if (cargo != null) {
                idCargo = cargo.getIdCargo();
                nombreCargo = cargo.getCargo();
            }
        }

        Long idCiclo = cicloRepository.findCicloPorEstadoActivo().map(Ciclo::getIdCiclo).orElse(null);
        String codigoCiclo = null;

        Ciclo cicloActivo = cicloRepository.findCicloActivo().orElse(null);
        if (cicloActivo != null) {
            idCiclo = cicloActivo.getIdCiclo();
            codigoCiclo = cicloActivo.getCodigoCiclo();
        }

        return new AuthLoginResponseDTO(
                jwtToken,
                nombreCompleto,
                usuario.getRol().getRol(),
                ultimoAccesoActual,
                0,
                null,
                "Login exitoso",
                true,
                idCargo,
                nombreCargo,
                idCiclo,
                codigoCiclo
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

        if (usuario.getEstadoUsuario() != null && usuario.getEstadoUsuario().getIdEstadoUsuario() != 1L) {
            return "Si el correo está registrado, recibirá un código de recuperación.";
        }

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

        if (usuario.getEstadoUsuario() != null && usuario.getEstadoUsuario().getIdEstadoUsuario() != 1L) {
            return new CambiarContrasenaResponseDTO(false, "No se puede restablecer la contraseña. Cuenta inactiva.");
        }

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

    @Transactional
    public void resetPassAdmin(Long idAdministrador, Long idUsuario, HttpServletRequest request) {

        String direccionIp = obtenerIpCliente(request);

        // 1. Verificar quién realiza la operación
        Usuario administrador = usuarioRepository.findById(idAdministrador)
                .orElseThrow(() -> new AccessDeniedException(
                        "Administrador no autorizado"));

        if (!"ADMINISTRADOR".equalsIgnoreCase(administrador.getRol().getRol())) {
            throw new AccessDeniedException("No tiene permisos para restablecer contraseñas");
        }

        // 2. Buscar el usuario afectado
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado"));

        // 3. Proteger las cuentas administrativas
        String rol = usuario.getRol().getRol();

        if ("ADMINISTRADOR".equalsIgnoreCase(rol)) {
            throw new AccessDeniedException("No se permite restablecer contraseñas de administradores");
        }

        // 4. Generar contraseña temporal
        String passwordTemporal = UserUtils.generarPasswordAleatorio(8);

        // 5. Actualizar credenciales
        usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        usuario.setUltimoAcceso(null);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        usuarioRepository.save(usuario);

        // 6. Invalidar sesiones anteriores
        sesionUsuarioService.cerrarTodasLasSesiones(idUsuario);

        // 7. Registrar auditoría
        Auditoria auditoria = new Auditoria();

        auditoria.setUsuario(administrador);
        auditoria.setAccion("RESET_PASSWORD_ADMIN");
        auditoria.setTabla("usuarios");
        auditoria.setIdRegistro(idUsuario);
        auditoria.setDireccionIp(direccionIp);

        String nombreAfectado = usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos();

        auditoria.setDescripcion("Restablecimiento administrativo de contraseña. Usuario afectado: " + idUsuario
                + ". Nombre: " + nombreAfectado + ". Rol: " + usuario.getRol().getRol());

        auditoriaRepository.save(auditoria);

        String correo = usuario.getCorreoInstitucional();
        String correoPersonal = usuario.getPersona().getCorreoPersonal();

        // Correo institucional (nombre de usuario para iniciar sesión)
        String correoInstitucional = usuario.getCorreoInstitucional();

        // Validar que exista un correo personal
        if (correoPersonal == null || correoPersonal.isBlank()) {
            throw new IllegalStateException(
                    "El usuario no tiene un correo personal registrado."
            );
        }

        // Enviar el correo después de confirmar la transacción
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emailService.enviarRestablecimientoAdministrativo(
                                correoPersonal,
                                nombreAfectado,
                                correoInstitucional,
                                passwordTemporal
                        );
                    }
                }
        );
    }

}
