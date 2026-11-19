package com.codice.sra.services;

import com.codice.sra.dtos.PensumCatalogoMateriaDTO;
import com.codice.sra.dtos.PensumDetalleDTO;
import com.codice.sra.dtos.PensumListadoDTO;
import com.codice.sra.dtos.PensumMateriaDetalleDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codice.sra.dtos.PensumCrearDTO;
import com.codice.sra.repositories.CarreraRepository;
import com.codice.sra.repositories.EstadoPensumRepository;
import java.time.Year;
import com.codice.sra.dtos.PensumBorradorDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import java.util.List;


@Service
@RequiredArgsConstructor
public class PensumService {

    private final PensumRepository pensumRepository;
    private final PensumMateriaRepository pensumMateriaRepository;
    private final MateriaRepository materiaRepository;
    private final CarreraRepository carreraRepository;
    private final EstadoPensumRepository estadoPensumRepository;
    private final EntityManager entityManager;


    @Transactional
    public PensumListadoDTO crearPensum(PensumCrearDTO dto) {
        if (dto.version() == null || dto.anioInicio() == null || dto.anioFin() == null)
            throw new IllegalArgumentException("Debe proporcionar la versión y los años del pensum");

        if (!dto.version().equals(dto.anioInicio()))
            throw new IllegalArgumentException("La versión debe coincidir con el año de inicio");

        if (dto.anioInicio() <= Year.now().getValue())
            throw new IllegalArgumentException("El año de inicio debe ser posterior al año actual");

        if (dto.anioFin() < dto.anioInicio())
            throw new IllegalArgumentException("El año final no puede ser anterior al año de inicio");

        Carrera carrera = carreraRepository.findById(dto.idCarrera())
                .orElseThrow(() -> new IllegalArgumentException("La carrera seleccionada no existe"));

        if (carrera.getEstadoCarrera() == null ||
                !Long.valueOf(3L).equals(carrera.getEstadoCarrera().getIdEstadoCarrera()))
            throw new IllegalArgumentException("Solo se pueden crear pensum para carreras en PLANIFICACION");

        if (pensumRepository.existsByCarrera_IdCarreraAndVersion(dto.idCarrera(), dto.version()))
            throw new IllegalArgumentException("Ya existe un pensum para esta carrera y versión");

        EstadoPensum estado = estadoPensumRepository.findByEstadoPensum("PLANIFICANDO")
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el estado PLANIFICANDO"));

        if (!Boolean.TRUE.equals(estado.getActivo()))
            throw new IllegalArgumentException("El estado PLANIFICANDO no está habilitado");

        Pensum pensum = new Pensum();
        pensum.setCarrera(carrera);
        pensum.setEstadoPensum(estado);
        pensum.setVersion(dto.version());
        pensum.setAnioInicio(dto.anioInicio());
        pensum.setAnioFin(dto.anioFin());

        return mapearCabecera(pensumRepository.saveAndFlush(pensum));
    }


    @Transactional(readOnly = true)
    public List<PensumListadoDTO> listarTodos() {
        return pensumRepository.findTodosParaAdministracion().stream().map(this::mapearCabecera).toList();
    }

    @Transactional(readOnly = true)
    public PensumDetalleDTO obtenerDetalle(Long idPensum) {
        if (idPensum == null || idPensum <= 0) throw new IllegalArgumentException("El ID del pensum debe ser positivo");
        Pensum pensum = pensumRepository.findDetalleCabecera(idPensum)
                .orElseThrow(() -> new IllegalArgumentException("No existe un pensum con ID: " + idPensum));
        List<PensumMateria> asignaciones = pensumMateriaRepository.findByPensumIdOrderByCicloRecomendado(idPensum);
        List<PensumMateriaDetalleDTO> materias = asignaciones.stream().map(pm -> {
            Materia materia = pm.getMateria();
            List<PensumMateriaDetalleDTO.PrerrequisitoDTO> prerrequisitos = materia.getPrerrequisitos() == null ? List.of() :
                    materia.getPrerrequisitos().stream().map(pr -> new PensumMateriaDetalleDTO.PrerrequisitoDTO(pr.getIdMateria(), pr.getCodigoMateria(), pr.getNombreMateria())).toList();
            return new PensumMateriaDetalleDTO(materia.getIdMateria(), materia.getCodigoMateria(), materia.getNombreMateria(),
                    materia.getUnidadesValorativas(), pm.getCicloRecomendado(), pm.getObligatoria(), prerrequisitos);
        }).toList();
        BigDecimal totalUv = materias.stream().map(PensumMateriaDetalleDTO::unidadesValorativas).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalCiclos = materias.stream().map(PensumMateriaDetalleDTO::cicloRecomendado).filter(c -> c != null).max(Integer::compareTo).orElse(0);
        return new PensumDetalleDTO(mapearCabecera(pensum), materias.size(), totalUv, totalCiclos, materias);
    }

    @Transactional(readOnly = true)
    public List<PensumCatalogoMateriaDTO> listarMateriasActivas() {
        return materiaRepository.findTodasActivas().stream()
                .map(m -> new PensumCatalogoMateriaDTO(m.getIdMateria(), m.getCodigoMateria(), m.getNombreMateria(), m.getUnidadesValorativas()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PensumMateriaDetalleDTO.PrerrequisitoDTO> obtenerPrerrequisitos(Long idMateria) {
        if (idMateria == null || idMateria <= 0) throw new IllegalArgumentException("El ID de la materia debe ser positivo");

        Materia materia = materiaRepository.findByIdConPrerrequisitos(idMateria)
                .orElseThrow(() -> new IllegalArgumentException("No existe una materia con ID: " + idMateria));

        return materia.getPrerrequisitos().stream()
                .map(m -> new PensumMateriaDetalleDTO.PrerrequisitoDTO(m.getIdMateria(), m.getCodigoMateria(), m.getNombreMateria()))
                .sorted(java.util.Comparator.comparing(PensumMateriaDetalleDTO.PrerrequisitoDTO::nombreMateria))
                .toList();
    }

    private PensumListadoDTO mapearCabecera(Pensum p) {
        return new PensumListadoDTO(p.getIdPensum(), p.getCarrera().getIdCarrera(), p.getCarrera().getCodigoCarrera(),
                p.getCarrera().getNombreCarrera(), p.getVersion(), p.getAnioInicio(), p.getAnioFin(),
                p.getEstadoPensum().getIdEstadoPensum(), p.getEstadoPensum().getEstadoPensum());
    }


    @Transactional
    public PensumDetalleDTO guardarBorrador(Long idPensum, PensumBorradorDTO dto) {
        if (idPensum == null || idPensum <= 0)
            throw new IllegalArgumentException("El ID del pensum debe ser positivo");

        if (dto == null || dto.materias() == null)
            throw new IllegalArgumentException("Debe proporcionar la lista de materias");

        Pensum pensum = pensumRepository.findDetalleCabecera(idPensum)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pensum solicitado"));

        String estado = pensum.getEstadoPensum().getEstadoPensum();

        if (!estado.equals("PLANIFICANDO") && !estado.equals("PENDIENTE_APROBACION"))
            throw new IllegalArgumentException("El estado actual del pensum no permite modificaciones");

        if (pensum.getAnioInicio() == null || pensum.getAnioFin() == null ||
                pensum.getAnioFin() < pensum.getAnioInicio())
            throw new IllegalArgumentException("El pensum no tiene un período académico válido");

        int totalCiclos = (pensum.getAnioFin() - pensum.getAnioInicio() + 1) * 2;

        Map<Long, PensumBorradorDTO.MateriaAsignadaDTO> asignaciones = new HashMap<>();
        Map<Integer, Integer> materiasPorCiclo = new HashMap<>();
        Map<Long, Materia> materiasValidas = new HashMap<>();

        for (PensumBorradorDTO.MateriaAsignadaDTO asignacion : dto.materias()) {
            if (asignacion == null || asignacion.idMateria() == null ||
                    asignacion.cicloRecomendado() == null || asignacion.obligatoria() == null)
                throw new IllegalArgumentException("Hay materias con información incompleta");

            Long idMateria = asignacion.idMateria();
            Integer ciclo = asignacion.cicloRecomendado();

            if (ciclo < 1 || ciclo > totalCiclos)
                throw new IllegalArgumentException("El ciclo " + ciclo + " está fuera del rango permitido");

            if (asignaciones.putIfAbsent(idMateria, asignacion) != null)
                throw new IllegalArgumentException("La materia " + idMateria + " está duplicada");

            materiasPorCiclo.merge(ciclo, 1, Integer::sum);

            if (materiasPorCiclo.get(ciclo) > 5)
                throw new IllegalArgumentException("El ciclo " + ciclo + " supera el máximo de 5 materias");

            Materia materia = materiaRepository.findByIdConPrerrequisitos(idMateria)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la materia: " + idMateria));

            materiasValidas.put(idMateria, materia);
        }

        for (PensumBorradorDTO.MateriaAsignadaDTO asignacion : dto.materias()) {
            Materia materia = materiasValidas.get(asignacion.idMateria());

            for (Materia prerrequisito : materia.getPrerrequisitos()) {
                PensumBorradorDTO.MateriaAsignadaDTO requisito = asignaciones.get(prerrequisito.getIdMateria());


                if (requisito == null && estado.equals("PENDIENTE_APROBACION"))
                    throw new IllegalArgumentException(
                            "La materia " + materia.getNombreMateria() +
                                    " requiere incluir " + prerrequisito.getNombreMateria() + " en el pensum");

                if (requisito != null && requisito.cicloRecomendado() >= asignacion.cicloRecomendado())
                    throw new IllegalArgumentException(
                            "La materia " + materia.getNombreMateria() +
                                    " requiere que " + prerrequisito.getNombreMateria() +
                                    " esté ubicada en un ciclo anterior");

            }
        }

        if (estado.equals("PENDIENTE_APROBACION")) {
            for (int ciclo = 1; ciclo <= totalCiclos; ciclo++) {
                int cantidad = materiasPorCiclo.getOrDefault(ciclo, 0);

                if (cantidad < 4)
                    throw new IllegalArgumentException(
                            "El ciclo " + ciclo + " debe contener entre 4 y 5 materias");
            }
        }

        Map<Long, PensumMateria> existentes = new HashMap<>();

        for (PensumMateria pm : pensum.getPensumMaterias())
            existentes.put(pm.getMateria().getIdMateria(), pm);

        pensum.getPensumMaterias().removeIf(pm ->
                !asignaciones.containsKey(pm.getMateria().getIdMateria()));

        for (PensumBorradorDTO.MateriaAsignadaDTO asignacion : dto.materias()) {
            PensumMateria pm = existentes.get(asignacion.idMateria());

            if (pm == null) {
                pm = new PensumMateria();
                pm.setPensum(pensum);
                pm.setMateria(materiasValidas.get(asignacion.idMateria()));
                pensum.getPensumMaterias().add(pm);
            }

            pm.setCicloRecomendado(asignacion.cicloRecomendado());
            pm.setObligatoria(asignacion.obligatoria());
        }

        pensumRepository.saveAndFlush(pensum);

        return obtenerDetalle(idPensum);
    }


    @Transactional
    public PensumListadoDTO enviarAprobacion(Long idPensum) {
        if (idPensum == null || idPensum <= 0)
            throw new IllegalArgumentException("El ID del pensum debe ser positivo");

        Pensum pensum = pensumRepository.findDetalleCabecera(idPensum)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pensum solicitado"));

        if (!"PLANIFICANDO".equals(pensum.getEstadoPensum().getEstadoPensum()))
            throw new IllegalArgumentException("Solo se pueden enviar a aprobación pensum en PLANIFICANDO");

        if (pensum.getAnioInicio() == null || pensum.getAnioFin() == null ||
                pensum.getAnioFin() < pensum.getAnioInicio())
            throw new IllegalArgumentException("El pensum no tiene un período académico válido");

        int totalCiclos = (pensum.getAnioFin() - pensum.getAnioInicio() + 1) * 2;

        List<PensumMateria> materias = pensumMateriaRepository.findByPensumIdOrderByCicloRecomendado(idPensum);

        Map<Integer, Integer> materiasPorCiclo = new HashMap<>();
        Map<Long, Integer> ciclosPorMateria = new HashMap<>();

        for (PensumMateria pm : materias) {
            Long idMateria = pm.getMateria().getIdMateria();
            Integer ciclo = pm.getCicloRecomendado();

            if (ciclo == null || ciclo < 1 || ciclo > totalCiclos)
                throw new IllegalArgumentException("La materia " + pm.getMateria().getNombreMateria() + " tiene un ciclo inválido");

            if (ciclosPorMateria.putIfAbsent(idMateria, ciclo) != null)
                throw new IllegalArgumentException("La materia " + pm.getMateria().getNombreMateria() + " está duplicada");

            materiasPorCiclo.merge(ciclo, 1, Integer::sum);
        }

        for (int ciclo = 1; ciclo <= totalCiclos; ciclo++) {
            int cantidad = materiasPorCiclo.getOrDefault(ciclo, 0);

            if (cantidad < 4 || cantidad > 5)
                throw new IllegalArgumentException(
                        "El ciclo " + ciclo + " debe contener entre 4 y 5 materias. Actualmente tiene " + cantidad);
        }

        for (PensumMateria pm : materias) {
            Materia materia = materiaRepository.findByIdConPrerrequisitos(pm.getMateria().getIdMateria())
                    .orElseThrow(() -> new IllegalArgumentException("No existe la materia asignada"));

            for (Materia prerrequisito : materia.getPrerrequisitos()) {
                Integer cicloPrerrequisito = ciclosPorMateria.get(prerrequisito.getIdMateria());

                if (cicloPrerrequisito == null)
                    throw new IllegalArgumentException(
                            "La materia " + materia.getNombreMateria() +
                                    " requiere incluir " + prerrequisito.getNombreMateria() + " en el pensum");

                if (cicloPrerrequisito >= pm.getCicloRecomendado())
                    throw new IllegalArgumentException(
                            "El prerrequisito " + prerrequisito.getNombreMateria() +
                                    " debe estar en un ciclo anterior a " + materia.getNombreMateria());
            }
        }

        EstadoPensum estado = estadoPensumRepository.findByEstadoPensum("PENDIENTE_APROBACION")
                .orElseThrow(() -> new IllegalArgumentException("No existe el estado PENDIENTE_APROBACION"));

        if (!Boolean.TRUE.equals(estado.getActivo()))
            throw new IllegalArgumentException("El estado PENDIENTE_APROBACION está deshabilitado");

        pensum.setEstadoPensum(estado);
        pensumRepository.saveAndFlush(pensum);

        return mapearCabecera(pensum);
    }


    @Transactional
    public PensumListadoDTO activarPensum(Long idPensum) {
        if (idPensum == null || idPensum <= 0)
            throw new IllegalArgumentException("El ID del pensum debe ser positivo");

        Pensum pensum = pensumRepository.findDetalleCabecera(idPensum)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pensum solicitado"));

        if (!"PENDIENTE_APROBACION".equals(pensum.getEstadoPensum().getEstadoPensum()))
            throw new IllegalArgumentException("Solo se pueden activar pensum pendientes de aprobación");

        Carrera carrera = pensum.getCarrera();

        if (carrera.getEstadoCarrera() == null || !Long.valueOf(3L).equals(carrera.getEstadoCarrera().getIdEstadoCarrera()))
            throw new IllegalArgumentException("La carrera debe estar en PLANIFICACION para activar su primer pensum");

        if (carreraRepository.tienePensumVigente(carrera.getIdCarrera()))
            throw new IllegalArgumentException("La carrera ya tiene un pensum vigente");

        if (pensum.getAnioInicio() == null || pensum.getAnioFin() == null || pensum.getAnioFin() < pensum.getAnioInicio())
            throw new IllegalArgumentException("El pensum no tiene un período académico válido");

        int totalCiclos = (pensum.getAnioFin() - pensum.getAnioInicio() + 1) * 2;

        List<PensumMateria> materias = pensumMateriaRepository.findByPensumIdOrderByCicloRecomendado(idPensum);
        Map<Integer, Integer> materiasPorCiclo = new HashMap<>();
        Map<Long, Integer> ciclosPorMateria = new HashMap<>();

        for (PensumMateria pm : materias) {
            Long idMateria = pm.getMateria().getIdMateria();
            Integer ciclo = pm.getCicloRecomendado();

            if (ciclo == null || ciclo < 1 || ciclo > totalCiclos)
                throw new IllegalArgumentException("La materia " + pm.getMateria().getNombreMateria() + " tiene un ciclo inválido");

            if (ciclosPorMateria.putIfAbsent(idMateria, ciclo) != null)
                throw new IllegalArgumentException("La materia " + pm.getMateria().getNombreMateria() + " está duplicada");

            materiasPorCiclo.merge(ciclo, 1, Integer::sum);
        }

        for (int ciclo = 1; ciclo <= totalCiclos; ciclo++) {
            int cantidad = materiasPorCiclo.getOrDefault(ciclo, 0);

            if (cantidad < 4 || cantidad > 5)
                throw new IllegalArgumentException("El ciclo " + ciclo + " debe contener entre 4 y 5 materias. Actualmente tiene " + cantidad);
        }

        for (PensumMateria pm : materias) {
            Materia materia = materiaRepository.findByIdConPrerrequisitos(pm.getMateria().getIdMateria())
                    .orElseThrow(() -> new IllegalArgumentException("No existe la materia asignada"));

            for (Materia prerrequisito : materia.getPrerrequisitos()) {
                Integer cicloPrerrequisito = ciclosPorMateria.get(prerrequisito.getIdMateria());

                if (cicloPrerrequisito == null || cicloPrerrequisito >= pm.getCicloRecomendado())
                    throw new IllegalArgumentException("El prerrequisito " + prerrequisito.getNombreMateria() + " de " + materia.getNombreMateria() + " no está ubicado correctamente");
            }
        }

        EstadoPensum estadoVigente = estadoPensumRepository.findByEstadoPensum("VIGENTE")
                .orElseThrow(() -> new IllegalArgumentException("No existe el estado VIGENTE"));

        if (!Boolean.TRUE.equals(estadoVigente.getActivo()))
            throw new IllegalArgumentException("El estado VIGENTE está deshabilitado");

        EstadoCarrera estadoActiva = entityManager.find(EstadoCarrera.class, 1L);

        if (estadoActiva == null || !"ACTIVA".equals(estadoActiva.getEstadoCarrera()))
            throw new IllegalArgumentException("No se encontró el estado ACTIVA de la carrera");

        pensum.setEstadoPensum(estadoVigente);
        carrera.setEstadoCarrera(estadoActiva);

        carreraRepository.save(carrera);
        pensumRepository.saveAndFlush(pensum);

        return mapearCabecera(pensum);
    }



}
