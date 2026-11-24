package com.codice.sra.services;

import com.codice.sra.dtos.MateriaActualizarDTO;
import com.codice.sra.dtos.MateriaCrearDTO;
import com.codice.sra.dtos.MateriaDetalleDTO;
import com.codice.sra.dtos.MateriaListadoDTO;
import com.codice.sra.exceptions.DuplicateResourceException;
import com.codice.sra.exceptions.ResourceNotFoundException;
import com.codice.sra.models.Materia;
import com.codice.sra.repositories.MateriaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MateriaService {

    private final MateriaRepository materiaRepository;
    private final EntityManager entityManager;

    public Page<MateriaListadoDTO> listar(
            String busqueda, Boolean estado, int pagina, int tamanio) {
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new IllegalArgumentException(
                    "La página debe ser mayor o igual a 0 y el tamaño debe estar entre 1 y 100");
        }

        String texto = Normalizer.normalize(busqueda.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String codigo = texto.replaceAll("[ ._-]+", "");
        return materiaRepository.buscarParaGestion(
                texto,
                codigo,
                estado,
                PageRequest.of(pagina, tamanio));
    }

    @Transactional
    public MateriaDetalleDTO crear(MateriaCrearDTO dto) {
        String codigo = dto.codigoMateria().strip();
        String nombre = dto.nombreMateria().strip();
        List<Long> idsPrerrequisitos = dto.idsPrerrequisitos() == null
                ? List.of()
                : dto.idsPrerrequisitos();

        if (new HashSet<>(idsPrerrequisitos).size() != idsPrerrequisitos.size()) {
            throw new IllegalArgumentException("Hay prerrequisitos duplicados");
        }

        bloquearEscrituras();

        if (materiaRepository.existsByCodigoMateriaIgnoreCase(codigo)) {
            throw new DuplicateResourceException("Ya existe una materia con código: " + codigo);
        }
        if (materiaRepository.existsByNombreMateriaIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Ya existe una materia con nombre: " + nombre);
        }

        List<Materia> prerrequisitos = materiaRepository.findAllById(idsPrerrequisitos);
        if (prerrequisitos.size() != idsPrerrequisitos.size()) {
            throw new IllegalArgumentException("Uno o más prerrequisitos no existen");
        }
        for (Materia prerrequisito : prerrequisitos) {
            if (!Boolean.TRUE.equals(prerrequisito.getEstadoMateria())) {
                throw new IllegalArgumentException(
                        "El prerrequisito " + prerrequisito.getCodigoMateria() + " está inactivo");
            }
        }

        Materia materia = new Materia();
        materia.setCodigoMateria(codigo);
        materia.setNombreMateria(nombre);
        materia.setUnidadesValorativas(dto.unidadesValorativas());
        materia.setPrerrequisitos(prerrequisitos);

        materiaRepository.saveAndFlush(materia);
        return obtenerDetalle(materia.getIdMateria());
    }

    @Transactional
    public MateriaDetalleDTO actualizar(Long idMateria, MateriaActualizarDTO dto) {
        bloquearEscrituras();
        Materia materia = buscarMateria(idMateria);
        String codigo = dto.codigoMateria().strip();
        String nombre = dto.nombreMateria().strip();

        if (codigo.equals(materia.getCodigoMateria())
                && nombre.equals(materia.getNombreMateria())
                && dto.unidadesValorativas().compareTo(materia.getUnidadesValorativas()) == 0) {
            return obtenerDetalle(idMateria);
        }

        if (materiaRepository.existsByCodigoMateriaIgnoreCaseAndIdMateriaNot(codigo, idMateria)) {
            throw new DuplicateResourceException("Ya existe una materia con código: " + codigo);
        }
        if (materiaRepository.existsByNombreMateriaIgnoreCaseAndIdMateriaNot(nombre, idMateria)) {
            throw new DuplicateResourceException("Ya existe una materia con nombre: " + nombre);
        }

        materia.setCodigoMateria(codigo);
        materia.setNombreMateria(nombre);
        materia.setUnidadesValorativas(dto.unidadesValorativas());
        return obtenerDetalle(idMateria);
    }

    @Transactional
    public MateriaDetalleDTO cambiarEstado(Long idMateria, boolean activa) {
        bloquearEscrituras();
        Materia materia = buscarMateria(idMateria);
        if (materia.getEstadoMateria() == activa) {
            return obtenerDetalle(idMateria);
        }

        if (!activa) {
            List<String> dependientes = materiaRepository.findCodigosDependientesActivos(idMateria);
            if (!dependientes.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Materias activas que requieren esta materia: " + String.join(", ", dependientes));
            }
        }

        materia.setEstadoMateria(activa);
        return obtenerDetalle(idMateria);
    }

    @Transactional
    public MateriaDetalleDTO reemplazarPrerrequisitos(Long idMateria, List<Long> idsPrerrequisitos) {
        bloquearEscrituras();
        Materia materia = buscarMateria(idMateria);
        Set<Long> solicitados = new HashSet<>(idsPrerrequisitos);
        if (solicitados.size() != idsPrerrequisitos.size()) {
            throw new IllegalArgumentException("Hay prerrequisitos duplicados");
        }

        Set<Long> actuales = materia.getPrerrequisitos().stream()
                .map(Materia::getIdMateria)
                .collect(Collectors.toSet());
        if (actuales.equals(solicitados) && actuales.size() == materia.getPrerrequisitos().size()) {
            return obtenerDetalle(idMateria);
        }

        if (solicitados.contains(idMateria)) {
            throw new IllegalArgumentException("Una materia no puede ser su propio prerrequisito");
        }

        List<Materia> requisitos = materiaRepository.findAllById(idsPrerrequisitos);
        if (requisitos.size() != solicitados.size()) {
            throw new IllegalArgumentException("Uno o más prerrequisitos no existen");
        }

        for (Materia requisito : requisitos) {
            if (!actuales.contains(requisito.getIdMateria()) && !Boolean.TRUE.equals(requisito.getEstadoMateria())) {
                throw new IllegalArgumentException(
                        "El prerrequisito " + requisito.getCodigoMateria() + " está inactivo");
            }
        }

        verificarSinCiclo(idMateria, requisitos);
        materia.getPrerrequisitos().clear();
        materia.getPrerrequisitos().addAll(requisitos);
        return obtenerDetalle(idMateria);
    }

    private void bloquearEscrituras() {
        entityManager.createNativeQuery("LOCK TABLE public.materias IN SHARE ROW EXCLUSIVE MODE")
                .executeUpdate();
    }

    private Materia buscarMateria(Long idMateria) {
        return materiaRepository.findByIdConPrerrequisitos(idMateria)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una materia con ID: " + idMateria));
    }

    private void verificarSinCiclo(Long idMateria, List<Materia> requisitos) {
        Set<Long> visitadas = new HashSet<>();
        ArrayDeque<Materia> pendientes = new ArrayDeque<>(requisitos);
        while (!pendientes.isEmpty()) {
            Materia requisito = pendientes.removeFirst();
            if (idMateria.equals(requisito.getIdMateria())) {
                throw new IllegalArgumentException(
                        "El cambio crearía un ciclo de prerrequisitos para la materia " + idMateria);
            }
            if (visitadas.add(requisito.getIdMateria())) {
                pendientes.addAll(requisito.getPrerrequisitos());
            }
        }
    }

    public MateriaDetalleDTO obtenerDetalle(Long idMateria) {
        Materia materia = buscarMateria(idMateria);

        List<MateriaDetalleDTO.PrerrequisitoDTO> prerrequisitos = materia.getPrerrequisitos()
                .stream()
                .sorted(Comparator.comparing(Materia::getNombreMateria)
                        .thenComparing(Materia::getIdMateria))
                .map(requisito -> new MateriaDetalleDTO.PrerrequisitoDTO(
                        requisito.getIdMateria(),
                        requisito.getCodigoMateria(),
                        requisito.getNombreMateria(),
                        requisito.getEstadoMateria()))
                .toList();

        List<MateriaDetalleDTO.PensumUsoDTO> pensums = materiaRepository
                .findPensumsPorMateria(idMateria)
                .stream()
                .map(pensum -> new MateriaDetalleDTO.PensumUsoDTO(
                        pensum.getIdPensum(),
                        pensum.getCarrera().getNombreCarrera(),
                        pensum.getVersion(),
                        pensum.getEstadoPensum().getEstadoPensum()))
                .toList();

        return new MateriaDetalleDTO(
                materia.getIdMateria(),
                materia.getCodigoMateria(),
                materia.getNombreMateria(),
                materia.getUnidadesValorativas(),
                materia.getEstadoMateria(),
                prerrequisitos,
                pensums,
                materiaRepository.findCodigosDependientesActivos(idMateria));
    }
}
