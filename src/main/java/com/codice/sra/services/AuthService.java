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
    private final JwtService jwtService; // Inyectamos nuestro generador de tokens

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
            throw new RuntimeException("Credenciales inválidas");
        }

        Usuario usuario = usuarioOpt.get();

        // 2. Verificar si el usuario está bloqueado
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(OffsetDateTime.now())) {
            throw new RuntimeException("Usuario bloqueado por múltiples intentos fallidos. Intente más tarde.");
        }

        // 3. Verificar contraseña encriptada
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            manejarIntentoFallido(usuario);
            throw new RuntimeException("Credenciales inválidas");
        }

        // 4. Si el login es exitoso, reiniciar intentos fallidos
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        // 5. Generar el token real usando la clave secreta
        String jwtToken = jwtService.generateToken(usuario.getCorreoInstitucional(), usuario.getRol().getRol());
        String nombreCompleto = usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos();

        return new AuthLoginResponseDTO(
                jwtToken, // Ahora devolvemos el token matemáticamente firmado
                nombreCompleto,
                usuario.getRol().getRol()
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