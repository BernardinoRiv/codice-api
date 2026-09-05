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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired
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

    @Transactional
    public UsuarioRegistroResponseDTO registrarUsuario(UsuarioRegistroRequestDTO request) {
        Rol rol = rolRepository.findById(request.getIdRol())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe"));

        EstadoUsuario estadoActivo = estadoUsuarioRepository.findByEstadoUsuario("Activo")
                .orElseThrow(() -> new IllegalStateException("Estado Activo no configurado en el sistema"));

        if (personaRepository.existsByDocumento(request.getDocumento())) {
            throw new IllegalArgumentException("Ya existe una persona registrada con este documento");
        }

        if (request.getCorreoPersonal() != null && personaRepository.existsByCorreoPersonal(request.getCorreoPersonal())) {
            throw new IllegalArgumentException("Ya existe una persona registrada con este correo personal");
        }

        String correoInstitucional = generarCorreoInstitucional(request.getNombres(), request.getApellidos(), rol.getRol());

        if (usuarioRepository.existsByCorreoInstitucional(correoInstitucional)) {
            correoInstitucional = generarCorreoUnico(request.getNombres(), request.getApellidos(), rol.getRol());
        }

        Persona persona = new Persona();
        persona.setDocumento(request.getDocumento());
        persona.setNombres(request.getNombres());
        persona.setApellidos(request.getApellidos());
        persona.setFechaNacimiento(request.getFechaNacimiento());
        persona.setTelefono(request.getTelefono());
        persona.setCorreoPersonal(request.getCorreoPersonal());
        persona.setDireccion(request.getDireccion());
        persona = personaRepository.save(persona);

        String passwordPlano = generarPasswordAleatorio(10);

        Usuario usuario = new Usuario();
        usuario.setPersona(persona);
        usuario.setRol(rol);
        usuario.setEstadoUsuario(estadoActivo);
        usuario.setCorreoInstitucional(correoInstitucional);
        usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
        usuario.setIntentosFallidos(0);
        usuario = usuarioRepository.save(usuario);

        // Envío de credenciales por correo electrónico de forma segura
        if (persona.getCorreoPersonal() != null && !persona.getCorreoPersonal().isEmpty()) {
            emailService.enviarCredenciales(
                    persona.getCorreoPersonal(),
                    persona.getNombres() + " " + persona.getApellidos(),
                    correoInstitucional,
                    passwordPlano
            );
        }

        String nombreCompleto = persona.getNombres() + " " + persona.getApellidos();

        return new UsuarioRegistroResponseDTO(
                usuario.getIdUsuario(),
                nombreCompleto,
                correoInstitucional,
                rol.getRol(),
                "Cuenta creada exitosamente. Las credenciales han sido enviadas al correo personal."
        );
    }

    private String generarPasswordAleatorio(int longitud) {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }

    private String generarCorreoInstitucional(String nombres, String apellidos, String rol) {
        String iniciales = extraerIniciales(nombres);
        String primerApellido = apellidos.split("\\s+")[0].toLowerCase().replaceAll("[^a-z]", "");
        String sufijoRol = switch (rol.toUpperCase()) {
            case "DOCENTE" -> "docente";
            case "ESTUDIANTE", "ALUMNO" -> "alumno";
            case "ADMINISTRADOR", "ADMIN" -> "admin";
            default -> "usuario";
        };
        return String.format("%s.%s@%s.uma.edu.svvv", iniciales, primerApellido, sufijoRol);
    }

    private String generarCorreoUnico(String nombres, String apellidos, String rol) {
        String base = generarCorreoInstitucional(nombres, apellidos, rol);
        String localPart = base.substring(0, base.indexOf('@'));
        String domain = base.substring(base.indexOf('@') + 1);
        int contador = 1;
        String candidato;
        do {
            candidato = String.format("%s%d@%s", localPart, contador, domain);
            contador++;
        } while (usuarioRepository.existsByCorreoInstitucional(candidato));
        return candidato;
    }

    private String extraerIniciales(String nombres) {
        String[] partes = nombres.trim().split("\\s+");
        StringBuilder iniciales = new StringBuilder();
        for (String parte : partes) {
            if (!parte.isEmpty()) {
                iniciales.append(Character.toLowerCase(parte.charAt(0)));
            }
        }
        return iniciales.toString();
    }
}