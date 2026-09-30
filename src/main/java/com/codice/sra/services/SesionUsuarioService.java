package com.codice.sra.services;

import com.codice.sra.dtos.AlertaSeguridadDTO;
import com.codice.sra.dtos.SesionUsuarioDTO;
import com.codice.sra.models.SesionUsuario;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.SesionUsuarioRepository;
import com.codice.sra.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SesionUsuarioService {

    private final SesionUsuarioRepository sesionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public SesionUsuarioService(SesionUsuarioRepository sesionRepository,
                                UsuarioRepository usuarioRepository,
                                EmailService emailService) {
        this.sesionRepository = sesionRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    @Transactional
    public SesionUsuarioDTO registrarSesion(Long idUsuario, String direccionIp, String agenteUsuario, boolean exitosa) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<SesionUsuario> sesionesActivas = sesionRepository.findByUsuarioIdUsuarioAndFechaFinIsNull(idUsuario);

        Optional<SesionUsuario> sesionExistente = sesionesActivas.stream()
                .filter(s -> s.getDireccionIp() != null && s.getDireccionIp().equals(direccionIp) &&
                        s.getAgenteUsuario() != null && s.getAgenteUsuario().equals(agenteUsuario))
                .findFirst();

        SesionUsuario sesionAGuardar;

        if (sesionExistente.isPresent()) {
            sesionAGuardar = sesionExistente.get();
            sesionAGuardar.setFechaInicio(LocalDateTime.now());
        } else {
            sesionAGuardar = new SesionUsuario();
            sesionAGuardar.setUsuario(usuario);
            sesionAGuardar.setRol(usuario.getRol());
            sesionAGuardar.setFechaInicio(LocalDateTime.now());
            sesionAGuardar.setDireccionIp(direccionIp);
            sesionAGuardar.setAgenteUsuario(agenteUsuario);
            sesionAGuardar.setExitosa(exitosa);
        }

        if (exitosa) {
            usuario.setUltimoAcceso(OffsetDateTime.now());
            usuario.setIntentosFallidos(0);

            boolean esAnomalia = detectarAnomalia(idUsuario, direccionIp, agenteUsuario);

            if (esAnomalia) {
                enviarAlertaSeguridad(usuario, direccionIp, agenteUsuario);
            }
        } else {
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        }

        usuarioRepository.save(usuario);
        SesionUsuario sesionGuardada = sesionRepository.save(sesionAGuardar);

        return mapearADTO(sesionGuardada, false);
    }

    private boolean detectarAnomalia(Long idUsuario, String nuevaIp, String nuevoAgente) {
        List<String> ipsConocidas = sesionRepository.findDistinctIpsByUsuario(idUsuario);
        List<String> agentesConocidos = sesionRepository.findDistinctAgentesByUsuario(idUsuario);

        if (ipsConocidas.isEmpty() && agentesConocidos.isEmpty()) {
            return false;
        }

        boolean ipDesconocida = !ipsConocidas.contains(nuevaIp);
        boolean dispositivoDesconocido = !agentesConocidos.contains(nuevoAgente);

        return ipDesconocida || dispositivoDesconocido;
    }

    private void enviarAlertaSeguridad(Usuario usuario, String ipSospechosa, String dispositivoSospechoso) {
        try {
            String nombreCompleto = usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos();

            AlertaSeguridadDTO alerta = new AlertaSeguridadDTO();
            alerta.setNombreDestinatario(nombreCompleto);
            alerta.setCorreoDestinatario(usuario.getCorreoInstitucional());
            alerta.setFechaEvento(LocalDateTime.now());
            alerta.setDireccionIpSospechosa(ipSospechosa);
            alerta.setDispositivoSospechoso(dispositivoSospechoso);
            alerta.setTipoAlerta("NUEVO_ACCESO_NO_RECONOCIDO");

            emailService.enviarAlertaSeguridad(alerta);
        } catch (Exception e) {
            System.err.println("Error al enviar alerta de seguridad: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<SesionUsuarioDTO> obtenerHistorialSesiones(Long idUsuario) {
        List<SesionUsuario> sesiones = sesionRepository.findByUsuarioIdUsuarioOrderByFechaInicioDesc(idUsuario);

        List<String> ipsConocidas = sesionRepository.findDistinctIpsByUsuario(idUsuario);
        List<String> agentesConocidos = sesionRepository.findDistinctAgentesByUsuario(idUsuario);

        return sesiones.stream()
                .map(sesion -> {
                    boolean esAnomalia = !ipsConocidas.contains(sesion.getDireccionIp()) ||
                            !agentesConocidos.contains(sesion.getAgenteUsuario());
                    return mapearADTO(sesion, esAnomalia);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void cerrarSesion(Long idSesion) {
        SesionUsuario sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (sesion.getFechaFin() == null) {
            sesion.setFechaFin(LocalDateTime.now());
            sesionRepository.save(sesion);
        }
    }

    @Transactional
    public void cerrarTodasLasSesiones(Long idUsuario) {
        sesionRepository.cerrarSesionesActivas(idUsuario);
    }

    private SesionUsuarioDTO mapearADTO(SesionUsuario sesion, boolean esAnomalia) {
        SesionUsuarioDTO dto = new SesionUsuarioDTO();
        dto.setIdSesion(sesion.getIdSesion());

        if (sesion.getUsuario() != null && sesion.getUsuario().getPersona() != null) {
            dto.setNombreUsuario(sesion.getUsuario().getPersona().getNombres() + " " + sesion.getUsuario().getPersona().getApellidos());
        }

        dto.setRol(sesion.getRol() != null ? sesion.getRol().getRol() : "Desconocido");
        dto.setFechaInicio(sesion.getFechaInicio());
        dto.setFechaFin(sesion.getFechaFin());
        dto.setDireccionIp(sesion.getDireccionIp());
        dto.setDispositivo(simplificarAgenteUsuario(sesion.getAgenteUsuario()));
        dto.setExitosa(sesion.getExitosa());
        dto.setEsAnomalia(esAnomalia);

        return dto;
    }

    private String simplificarAgenteUsuario(String agente) {
        if (agente == null || agente.isEmpty()) return "Dispositivo desconocido";
        if (agente.toLowerCase().contains("mobile") || agente.toLowerCase().contains("android")) return "Dispositivo Móvil";
        if (agente.toLowerCase().contains("windows")) return "Windows PC";
        if (agente.toLowerCase().contains("macintosh")) return "Mac";
        if (agente.toLowerCase().contains("linux")) return "Linux";
        return "Navegador Web";
    }
}