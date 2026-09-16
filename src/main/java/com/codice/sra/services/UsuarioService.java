package com.codice.sra.services;

import com.codice.sra.dtos.UsuarioRegistroRequestDTO;
import com.codice.sra.dtos.UsuarioRegistroResponseDTO;
import com.codice.sra.models.EstadoUsuario;
import com.codice.sra.models.Persona;
import com.codice.sra.models.Rol;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.EstadoUsuarioRepository;
import com.codice.sra.repositories.PersonaRepository;
import com.codice.sra.repositories.RolRepository;
import com.codice.sra.repositories.UsuarioRepository;
import com.codice.sra.utils.UserUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    public static final String ESTADO_ACTIVO = "ACTIVO";
    private static final int LONGITUD_PASSWORD_DEFAULT = 8;

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PersonaRepository personaRepository,
                          RolRepository rolRepository,
                          EstadoUsuarioRepository estadoUsuarioRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.estadoUsuarioRepository = estadoUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Endpoint administrativo: Crea y asigna una cuenta de acceso a una persona ya registrada.
     * Consumido directamente por UsuarioController.
     */
    @Transactional
    public UsuarioRegistroResponseDTO registrarUsuario(UsuarioRegistroRequestDTO request) {
        Persona persona = personaRepository.findById(request.getIdPersona())
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna persona registrada con ID: " + request.getIdPersona()));

        Rol rol = rolRepository.findById(request.getIdRol())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe con ID: " + request.getIdRol()));

        Usuario usuario = crearCuentaUsuario(persona, rol, LONGITUD_PASSWORD_DEFAULT);

        return new UsuarioRegistroResponseDTO(
                usuario.getIdUsuario(),
                persona.getNombres() + " " + persona.getApellidos(),
                usuario.getCorreoInstitucional(),
                rol.getRol(),
                "Cuenta de usuario creada exitosamente. Las credenciales han sido enviadas."
        );
    }

    /**
     * Método de integración interna: Consumido por DocenteService y EmpleadoService
     * para aprovisionar credenciales a partir de una entidad Persona procesada.
     */
    @Transactional
    public Usuario aprovisionarUsuario(Persona persona, String nombreRol, int longitudPassword) {
        Rol rol = rolRepository.findByRol(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado en el sistema: " + nombreRol));

        return crearCuentaUsuario(persona, rol, longitudPassword);
    }

    /**
     * Lógica atómica de aprovisionamiento de credenciales y persistencia.
     */
    private Usuario crearCuentaUsuario(Persona persona, Rol rol, int longitudPassword) {
        if (usuarioRepository.existsByPersona_IdPersonaAndRol_IdRol(persona.getIdPersona(), rol.getIdRol())) {
            throw new IllegalArgumentException("La persona ya tiene asignada una cuenta activa con el rol: " + rol.getRol());
        }

        EstadoUsuario estadoActivo = estadoUsuarioRepository.findByEstadoUsuario(ESTADO_ACTIVO)
                .orElseThrow(() -> new IllegalStateException("El estado '" + ESTADO_ACTIVO + "' no está configurado en la base de datos"));

        // Generar correo institucional único usando UserUtils
        String correoInstitucional = UserUtils.generarCorreoInstitucional(persona.getNombres(), persona.getApellidos(), rol.getRol());
        if (usuarioRepository.existsByCorreoInstitucional(correoInstitucional)) {
            correoInstitucional = UserUtils.generarCorreoUnico(
                    persona.getNombres(),
                    persona.getApellidos(),
                    rol.getRol(),
                    usuarioRepository::existsByCorreoInstitucional
            );
        }

        String passwordPlano = UserUtils.generarPasswordAleatorio(longitudPassword);

        Usuario usuario = new Usuario();
        usuario.setPersona(persona);
        usuario.setRol(rol);
        usuario.setEstadoUsuario(estadoActivo);
        usuario.setCorreoInstitucional(correoInstitucional);
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        usuario.setIntentosFallidos(0);
        usuario = usuarioRepository.save(usuario);

        // Envío de credenciales si la persona tiene correo registrado
        if (persona.getCorreoPersonal() != null && !persona.getCorreoPersonal().isBlank()) {
            emailService.enviarCredenciales(
                    persona.getCorreoPersonal(),
                    persona.getNombres() + " " + persona.getApellidos(),
                    correoInstitucional,
                    passwordPlano
            );
        }

        return usuario;
    }
}