package com.codice.sra.services;

import com.codice.sra.dtos.AuthLoginRequestDTO;
import com.codice.sra.dtos.AuthLoginResponseDTO;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.UsuarioRepository;
import com.codice.sra.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
            return new AuthLoginResponseDTO(null, null, null, 0, null, "Credenciales inválidas", false);
        }

        Usuario usuario = usuarioOpt.get();

        // 2. Verificar si el usuario está bloqueado
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(OffsetDateTime.now())) {
            return new AuthLoginResponseDTO(
                    null, null, null,
                    usuario.getIntentosFallidos(),
                    usuario.getBloqueadoHasta(),
                    "Usuario bloqueado por múltiples intentos fallidos. Intente más tarde.",
                    false
            );
        }

        // 3. Verificar contraseña encriptada
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            manejarIntentoFallido(usuario);

            // Calculamos el estado actualizado para devolverlo en la respuesta
            int nuevosIntentos = usuario.getIntentosFallidos();
            OffsetDateTime nuevoBloqueo = nuevosIntentos >= 3 ? OffsetDateTime.now().plusMinutes(15) : null;

            return new AuthLoginResponseDTO(
                    null, null, null,
                    nuevosIntentos,
                    nuevoBloqueo,
                    "Credenciales inválidas",
                    false
            );
        }

        // 4. Si el login es exitoso, reiniciar intentos fallidos
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        // 5. Generar el token real
        String jwtToken = jwtService.generateToken(usuario);
        String nombreCompleto = usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos();

        return new AuthLoginResponseDTO(
                jwtToken,
                nombreCompleto,
                usuario.getRol().getRol(),
                0, // 0 intentos fallidos porque fue exitoso
                null, // No está bloqueado
                "Login exitoso",
                true
        );
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