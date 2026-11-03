package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.models.Estudiante;
import com.codice.sra.models.Persona;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.EstudianteRepository;
import com.codice.sra.repositories.PersonaRepository;
import com.codice.sra.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AjustesService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerPerfilGeneral(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));

        Persona persona = usuario.getPersona();
        if (persona == null) {
            throw new RuntimeException("No se encontró la información personal asociada al usuario");
        }

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nombres", persona.getNombres());
        perfil.put("apellidos", persona.getApellidos());
        perfil.put("numeroDocumento", persona.getNumeroDocumento());
        perfil.put("sexo", persona.getSexo());
        perfil.put("nombreCompleto", persona.getNombres() + " " + persona.getApellidos());
        perfil.put("correoInstitucional", usuario.getCorreoInstitucional());
        perfil.put("correoPersonal", persona.getCorreoPersonal());
        perfil.put("telefono", persona.getTelefono());
        perfil.put("direccion", persona.getDireccion());
        perfil.put("urlFoto", persona.getUrlFoto());

        // Verificamos si también es estudiante para adjuntar su carnet de forma opcional
        Optional<Estudiante> estudianteOpt = estudianteRepository.findByIdUsuario(usuario.getIdUsuario());
        if (estudianteOpt.isPresent()) {
            perfil.put("carnet", estudianteOpt.get().getCarnet());
            perfil.put("esEstudiante", true);
        } else {
            perfil.put("carnet", "N/A");
            perfil.put("esEstudiante", false);
        }

        return perfil;
    }

    @Transactional
    public Map<String, Object> actualizarDatosBasicos(Long idUsuario, ActualizarPerfilDTO dto) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Persona persona = usuario.getPersona();
        persona.setCorreoPersonal(dto.getCorreoPersonal());
        persona.setTelefono(dto.getTelefono());
        persona.setDireccion(dto.getDireccion());

        personaRepository.save(persona);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Información personal actualizada correctamente.");
        return respuesta;
    }

    @Transactional
    public Map<String, Object> cambiarPassword(Long idUsuario, CambiarPasswordDTO dto) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta.");
        }

        if (!dto.getNuevaPassword().equals(dto.getConfirmarPassword())) {
            throw new RuntimeException("Las contraseñas nuevas no coinciden.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(dto.getNuevaPassword()));
        usuarioRepository.save(usuario);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Contraseña actualizada de forma segura.");
        return respuesta;
    }

    @Transactional
    public Map<String, Object> actualizarFotoPerfil(Long idUsuario, MultipartFile archivo) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Persona persona = usuario.getPersona();
        String urlFoto = fileStorageService.guardarFoto(archivo);

        persona.setUrlFoto(urlFoto);
        personaRepository.save(persona);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Fotografía actualizada correctamente.");
        respuesta.put("urlFoto", urlFoto);
        return respuesta;
    }
}