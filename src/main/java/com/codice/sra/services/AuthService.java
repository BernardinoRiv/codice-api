package com.codice.sra.services;

import com.codice.sra.dtos.AuthLoginRequestDTO;
import com.codice.sra.dtos.AuthLoginResponseDTO;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.UsuarioRepository;
import com.codice.sra.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.codice.sra.dtos.CambiarContrasenaRequestDTO;
import com.codice.sra.dtos.CambiarContrasenaResponseDTO;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthLoginResponseDTO login(AuthLoginRequestDTO request) {
        // 1. Buscar al usuario por correo
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoInstitucional(request.getCorreoInstitucional());

        if (usuarioOpt.isEmpty()) {
            return new AuthLoginResponseDTO(null, null, null, null, 0, null, "Credenciales inválidas", false);
        }

        Usuario usuario = usuarioOpt.get();

        // Guardar el último acceso ANTES de actualizarlo
        OffsetDateTime ultimoAccesoAntes = usuario.getUltimoAcceso();

        // 2. Verificar si el usuario está bloqueado
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(OffsetDateTime.now())) {
            return new AuthLoginResponseDTO(
                    null, null, null, null,
                    usuario.getIntentosFallidos(),
                    usuario.getBloqueadoHasta(),
                    "Usuario bloqueado por múltiples intentos fallidos. Intente más tarde.",
                    false
            );
        }

        // 3. Verificar contraseña encriptada
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

        // 4. Si el login es exitoso, reiniciar intentos fallidos y actualizar último acceso
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        OffsetDateTime ahora = OffsetDateTime.now();
        usuario.setUltimoAcceso(ahora);
        usuarioRepository.save(usuario);

        // 5. Generar el token
        String jwtToken = jwtService.generateToken(usuario);
        String nombreCompleto = usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos();

        return new AuthLoginResponseDTO(
                jwtToken,
                nombreCompleto,
                usuario.getRol().getRol(),
                ultimoAccesoAntes,  // Devolvemos el último acceso ANTES de este login
                0,
                null,
                "Login exitoso",
                true
        );
    }

    @Transactional
    public CambiarContrasenaResponseDTO cambiarContrasena(Long idUsuario, CambiarContrasenaRequestDTO request) {

        // 1. Validar que las nuevas contraseñas coincidan
        if (!request.getNuevaContrasena().equals(request.getConfirmarNuevaContrasena())) {
            return new CambiarContrasenaResponseDTO(false, "Las nuevas contraseñas no coinciden");
        }

        // 2. Buscar al usuario
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Validar contraseña actual
        if (!passwordEncoder.matches(request.getContrasenaActual(), usuario.getPasswordHash())) {
            return new CambiarContrasenaResponseDTO(false, "La contraseña actual es incorrecta");
        }

        // 4. Validar que la nueva contraseña no sea igual a la actual
        if (passwordEncoder.matches(request.getNuevaContrasena(), usuario.getPasswordHash())) {
            return new CambiarContrasenaResponseDTO(false,
                    "La nueva contraseña debe ser diferente a la actual");
        }

        // 5. Validar fortaleza de la contraseña (opcional pero recomendado)
        if (request.getNuevaContrasena().length() < 8) {
            return new CambiarContrasenaResponseDTO(false,
                    "La contraseña debe tener al menos 8 caracteres");
        }

        // 6. Encriptar y guardar la nueva contraseña
        String nuevaContrasenaHash = passwordEncoder.encode(request.getNuevaContrasena());
        usuario.setPasswordHash(nuevaContrasenaHash);
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
}