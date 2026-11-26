package com.codice.sra.security;

import com.codice.sra.models.Empleado;
import com.codice.sra.models.Usuario;
import com.codice.sra.repositories.DocenteRepository;
import com.codice.sra.repositories.EstudianteCarreraRepository;
import com.codice.sra.repositories.EstudianteRepository;
import lombok.RequiredArgsConstructor;
import com.codice.sra.repositories.EmpleadoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PermisosEmpleado {

    private final EmpleadoRepository empleadoRepository;

    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstudianteCarreraRepository estudianteCarreraRepository;

    public boolean puede(Authentication authentication, String... vistasRequeridas) {
        return empleadoAutenticado(authentication)
                .map(empleado -> !Collections.disjoint(vistas(empleado), List.of(vistasRequeridas)))
                .orElse(false);
    }

    public boolean puedeRegistrarEnSede(Authentication authentication, Long idSede, String vista) {
        return idSede != null && empleadoAutenticado(authentication)
                .filter(empleado -> empleado.getSede() != null)
                .filter(empleado -> Objects.equals(empleado.getSede().getIdSede(), idSede))
                .map(empleado -> vistas(empleado).contains(vista))
                .orElse(false);
    }

    public Long sedeConsulta(Authentication authentication, String vista) {
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()))) {
            return null;
        }
        return empleadoAutenticado(authentication)
                .filter(empleado -> vistas(empleado).contains(vista))
                .map(Empleado::getSede)
                .map(sede -> sede.getIdSede())
                .orElseThrow(() -> new AccessDeniedException("No tienes acceso a este directorio."));
    }

    @Transactional(readOnly = true)
    public boolean puedeEditarEmpleado(Authentication autenticacion, Long idEmpleado) {
        return empleadoDeSede(autenticacion, idEmpleado).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean puedeEditarEmpleado(Authentication autenticacion, Long idEmpleado, Long idSede, Long idArea, Long idCargo) {
        return empleadoDeSede(autenticacion, idEmpleado)
                .filter(empleado -> Objects.equals(empleado.getSede().getIdSede(), idSede))
                .filter(empleado -> empleado.getArea() != null && Objects.equals(empleado.getArea().getIdArea(), idArea))
                .filter(empleado -> empleado.getCargo() != null && Objects.equals(empleado.getCargo().getIdCargo(), idCargo))
                .isPresent();
    }

    private Optional<Empleado> empleadoDeSede(Authentication autenticacion, Long idEmpleado) {
        return sedeRegistrador(autenticacion)
                .flatMap(sede -> empleadoRepository.findByIdEmpleadoAndSede_IdSede(idEmpleado, sede))
                .filter(empleado -> tieneRol(empleado.getUsuario(), "EMPLEADO"));
    }

    @Transactional(readOnly = true)
    public boolean puedeEditarDocente(Authentication autenticacion, Long idDocente) {
        return puedeEditarDocente(autenticacion, idDocente, null);
    }

    @Transactional(readOnly = true)
    public boolean puedeEditarDocente(Authentication autenticacion, Long idDocente, Long sedeDestino) {
        return sedeRegistrador(autenticacion)
                .filter(sede -> sedeDestino == null || Objects.equals(sede, sedeDestino))
                .flatMap(sede -> docenteRepository.findByIdDocenteAndSede_IdSede(idDocente, sede))
                .filter(docente -> tieneRol(docente.getUsuario(), "DOCENTE"))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean puedeEditarEstudiante(Authentication autenticacion, Long idEstudiante) {
        return sedeRegistrador(autenticacion)
                .filter(sede -> estudianteEnSede(idEstudiante, sede))
                .flatMap(sede -> estudianteRepository.findById(idEstudiante))
                .filter(estudiante -> tieneRol(estudiante.getUsuario(), "ESTUDIANTE"))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean puedeRestablecer(Authentication autenticacion, Usuario usuario) {
        Long idUsuario = usuario.getIdUsuario();
        return switch (usuario.getRol().getRol().toUpperCase(Locale.ROOT)) {
            case "EMPLEADO" -> empleadoRepository.findByUsuarioIdUsuario(idUsuario)
                    .filter(empleado -> puedeEditarEmpleado(autenticacion, empleado.getIdEmpleado())).isPresent();
            case "DOCENTE" -> docenteRepository.findByUsuarioIdUsuario(idUsuario)
                    .filter(docente -> puedeEditarDocente(autenticacion, docente.getIdDocente())).isPresent();
            case "ESTUDIANTE" -> estudianteRepository.findByUsuarioIdUsuario(idUsuario)
                    .filter(estudiante -> puedeEditarEstudiante(autenticacion, estudiante.getIdEstudiante())).isPresent();
            default -> false;
        };
    }

    private Optional<Long> sedeRegistrador(Authentication autenticacion) {
        return empleadoAutenticado(autenticacion)
                .filter(empleado -> empleado.getUsuario() != null && tieneRol(empleado.getUsuario(), "EMPLEADO"))
                .filter(empleado -> empleado.getArea() != null && empleado.getCargo() != null
                        && esAsignacionRegistrador(empleado.getArea().getArea(), empleado.getCargo().getCargo()))
                .map(Empleado::getSede)
                .map(sede -> sede.getIdSede());
    }

    private boolean estudianteEnSede(Long idEstudiante, Long idSede) {
        return estudianteCarreraRepository.findIdsSedesPorEstudiante(idEstudiante).equals(List.of(idSede));
    }

    private boolean tieneRol(Usuario usuario, String rol) {
        return usuario == null || (usuario.getRol() != null && rol.equalsIgnoreCase(usuario.getRol().getRol()));
    }

    private Optional<Empleado> empleadoAutenticado(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long idUsuario)
                || authentication.getAuthorities().stream()
                    .noneMatch(autoridad -> "ROLE_EMPLEADO".equals(autoridad.getAuthority()))) {
            return Optional.empty();
        }
        return empleadoRepository.findByUsuarioIdUsuario(idUsuario);
    }

    public static boolean esAsignacionRegistrador(String area, String cargo) {
        return "ADMINISTRACION ACADEMICA".equals(normalizar(area))
                && "REGISTRADOR ACADEMICO".equals(normalizar(cargo));
    }

    static List<String> vistas(Empleado empleado) {
        if (empleado.getArea() == null || empleado.getCargo() == null) {
            return List.of();
        }

        String area = normalizar(empleado.getArea().getArea());
        String cargo = normalizar(empleado.getCargo().getCargo());

        if (esAsignacionRegistrador(area, cargo)) {
            return empleado.getSede() == null ? List.of()
                    : List.of("registro-empleados", "registro-docente", "nuevo-ingreso",
                            "directorio-docentes", "directorio-docentes/:idDocente",
                            "directorio-empleados", "directorio-empleados/:idEmpleado",
                            "directorio-estudiantes", "directorio-estudiantes/:idEstudiante");
        }

        return switch (area + "/" + cargo) {
            case "FINANZAS/CAJERO", "COLECTURIA/CAJERO" -> List.of("pagos", "facturas");
            case "FINANZAS/ASISTENTE FINANCIERO" -> List.of("facturas");
            default -> List.of();
        };
    }

    private static String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }
}