package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EstudianteClasesService {

    private final InscripcionRepository inscripcionRepo;
    private final PensumMateriaRepository pensumMateriaRepo;
    private final HorarioRepository horarioRepo;
    private final CicloRepository cicloRepository;
    private final ConceptoCobroRepository conceptoCobroRepo;
    private final CargoEstudianteRepository cargoEstudianteRepo;
    private final EstadoCargoRepository estadoCargoRepo;
    private final EstadoMateriaEstudianteRepository estadoMateriaEstudianteRepo;

    public EstudianteClasesService(InscripcionRepository inscripcionRepo,
                                   PensumMateriaRepository pensumMateriaRepo,
                                   HorarioRepository horarioRepo,
                                   CicloRepository cicloRepository,
                                   ConceptoCobroRepository conceptoCobroRepo,
                                   CargoEstudianteRepository cargoEstudianteRepo,
                                   EstadoCargoRepository estadoCargoRepo,
                                   EstadoMateriaEstudianteRepository estadoMateriaEstudianteRepo) {
        this.inscripcionRepo = inscripcionRepo;
        this.pensumMateriaRepo = pensumMateriaRepo;
        this.horarioRepo = horarioRepo;
        this.cicloRepository = cicloRepository;
        this.conceptoCobroRepo = conceptoCobroRepo;
        this.cargoEstudianteRepo = cargoEstudianteRepo;
        this.estadoCargoRepo = estadoCargoRepo;
        this.estadoMateriaEstudianteRepo = estadoMateriaEstudianteRepo;
    }

    @Transactional(readOnly = true)
    public List<ClaseInscritaDTO> obtenerClasesCicloActivo(Long idEstudiante) {
        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("no hay ciclo activo"));

        List<Inscripcion> inscripciones = inscripcionRepo.findByEstudianteAndCiclo(idEstudiante, cicloActivo.getIdCiclo());

        return inscripciones.stream().map(insc -> {
            ClaseInscritaDTO dto = new ClaseInscritaDTO();
            Grupo grupo = insc.getGrupo();
            Materia materia = grupo.getMateria();

            dto.setCodigoMateria(materia.getCodigoMateria());
            dto.setNombreMateria(capitalizarTexto(materia.getNombreMateria()));
            dto.setGrupo(grupo.getCodigoGrupo());
            dto.setUvs(materia.getUnidadesValorativas());
            dto.setNombreDocente(capitalizarTexto(grupo.getDocente().getPersona().getNombres() + " " + grupo.getDocente().getPersona().getApellidos()));

            List<Horario> horarios = horarioRepo.findByGrupo_IdGrupo(grupo.getIdGrupo());
            dto.setHorarios(horarios.stream().map(h -> {
                HorarioDTO hd = new HorarioDTO();
                hd.setDia(capitalizarTexto(h.getDia().getDia()));
                hd.setHoraInicio(h.getHoraInicio());
                hd.setHoraFin(h.getHoraFin());
                hd.setAula(h.getAula() != null ? h.getAula().getCodigoAula() : "virtual");
                hd.setModalidad(h.getModalidad() != null ? capitalizarTexto(h.getModalidad().getModalidad()) : "presencial");

                return hd;
            }).collect(Collectors.toList()));

            dto.setIdGrupo(grupo.getIdGrupo());
            if (insc.getEstadoMateria() != null) {
                dto.setEstadoMateria(capitalizarTexto(insc.getEstadoMateria().getEstadoMateria()));
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PensumEstudianteDTO obtenerPensum(Long idEstudiante) {
        List<Inscripcion> historial = inscripcionRepo.findAllByEstudianteId(idEstudiante);
        if (historial.isEmpty()) {
            throw new RuntimeException("el estudiante no tiene historial académico asignado.");
        }

        Long idPensum = historial.get(0).getMatricula().getEstudianteCarrera().getPensum().getIdPensum();
        List<PensumMateria> estructuraPensum = pensumMateriaRepo.findByPensumIdOrderByCicloRecomendado(idPensum);

        Ciclo cicloActivo = cicloRepository.findCicloActivo().orElse(null);
        Long idCicloActivo = (cicloActivo != null) ? cicloActivo.getIdCiclo() : -1L;

        Map<Long, List<Inscripcion>> inscripcionesPorMateria = historial.stream()
                .collect(Collectors.groupingBy(i -> i.getGrupo().getMateria().getIdMateria()));

        PensumEstudianteDTO respuesta = new PensumEstudianteDTO();
        Map<Integer, List<MateriaPensumDTO>> agrupadoPorCiclo = new HashMap<>();

        int aprobadas = 0;
        int cursando = 0;
        int pendientes = 0;
        int totalMaterias = estructuraPensum.size();

        for (PensumMateria pm : estructuraPensum) {
            MateriaPensumDTO dto = new MateriaPensumDTO();
            dto.setCodigo(pm.getMateria().getCodigoMateria());
            dto.setNombre(capitalizarTexto(pm.getMateria().getNombreMateria()));
            dto.setUvs(pm.getMateria().getUnidadesValorativas());
            dto.setCicloRecomendado(pm.getCicloRecomendado());

            // Lógica para extraer prerrequisitos con nombre, código o "Bachillerato"
            if (pm.getMateria().getPrerrequisitos() != null && !pm.getMateria().getPrerrequisitos().isEmpty()) {
                List<String> codigosPre = pm.getMateria().getPrerrequisitos().stream()
                        .map(pre -> "[" + pre.getCodigoMateria() + "] " + capitalizarTexto(pre.getNombreMateria()))
                        .collect(Collectors.toList());
                dto.setPrerrequisitos(codigosPre);
            } else {
                dto.setPrerrequisitos(List.of("Bachillerato"));
            }

            List<Inscripcion> inscripcionesMateria = inscripcionesPorMateria.getOrDefault(pm.getMateria().getIdMateria(), new ArrayList<>());

            if (inscripcionesMateria.isEmpty()) {
                dto.setEstado("pendiente");
                pendientes++;
            } else {
                boolean estaAprobada = false;
                boolean estaCursando = false;
                boolean estaRetirada = false;
                boolean estaReprobada = false;

                for (Inscripcion insc : inscripcionesMateria) {
                    String estadoInsc = insc.getEstadoMateria() != null ? insc.getEstadoMateria().getEstadoMateria().toUpperCase() : "";

                    if ("APROBADA".equals(estadoInsc)) {
                        estaAprobada = true;
                    }
                    if (insc.getGrupo().getCiclo().getIdCiclo().equals(idCicloActivo)) {
                        if ("RETIRADA".equals(estadoInsc) || "RETIRO PENDIENTE".equals(estadoInsc)) {
                            estaRetirada = true;
                        } else if ("CURSANDO".equals(estadoInsc)) {
                            estaCursando = true;
                        } else if ("REPROBADA".equals(estadoInsc)) {
                            estaReprobada = true;
                        }
                    }
                }

                if (estaAprobada) {
                    dto.setEstado("aprobada");
                    aprobadas++;
                } else if (estaRetirada) {
                    dto.setEstado("retirada");
                } else if (estaReprobada) {
                    dto.setEstado("reprobada");
                    pendientes++;
                } else if (estaCursando) {
                    dto.setEstado("cursando");
                    cursando++;
                } else {
                    dto.setEstado("pendiente");
                    pendientes++;
                }
            }
            agrupadoPorCiclo.computeIfAbsent(pm.getCicloRecomendado(), k -> new ArrayList<>()).add(dto);
        }

        respuesta.setMateriasPorCiclo(agrupadoPorCiclo);
        respuesta.setMateriasAprobadas(aprobadas);
        respuesta.setMateriasCursando(cursando);
        respuesta.setMateriasPendientes(pendientes);

        double porcentaje = totalMaterias > 0 ? ((double) aprobadas / totalMaterias) * 100 : 0.0;
        respuesta.setPorcentajeAvance(Math.round(porcentaje * 100.0) / 100.0);

        return respuesta;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> solicitarRetiroMateria(Long idEstudiante, Long idGrupo) {
        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("no hay ciclo activo."));

        List<Inscripcion> inscripciones = inscripcionRepo.findByEstudianteAndCiclo(idEstudiante, cicloActivo.getIdCiclo());
        Inscripcion inscripcion = inscripciones.stream()
                .filter(i -> i.getGrupo().getIdGrupo().equals(idGrupo))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no te encuentras inscrito en esta materia o no pertenece al ciclo activo."));

        if (inscripcion.getEstadoMateria() != null) {
            String estadoActual = inscripcion.getEstadoMateria().getEstadoMateria().toUpperCase();
            if ("RETIRADA".equals(estadoActual)) throw new RuntimeException("esta materia ya se encuentra retirada.");
            if ("RETIRO PENDIENTE".equals(estadoActual)) throw new RuntimeException("ya tienes un proceso de retiro pendiente para esta materia.");
        }

        EstadoMateriaEstudiante estadoRetiroPendiente = estadoMateriaEstudianteRepo.findById(6L)
                .orElseThrow(() -> new RuntimeException("estado retiro pendiente no configurado."));
        inscripcion.setEstadoMateria(estadoRetiroPendiente);
        inscripcionRepo.save(inscripcion);

        ConceptoCobro conceptoRetiro = conceptoCobroRepo.findConceptoRetiroPorCiclo(cicloActivo.getIdCiclo())
                .orElseThrow(() -> new RuntimeException("no se encontró el arancel configurado para el retiro de materias."));

        EstadoCargo estadoPendiente = estadoCargoRepo.findById(1L).orElseThrow();

        CargoEstudiante nuevoCargo = new CargoEstudiante();
        nuevoCargo.setMatricula(inscripcion.getMatricula());
        nuevoCargo.setConceptoCobro(conceptoRetiro);
        nuevoCargo.setEstadoCargo(estadoPendiente);
        nuevoCargo.setFechaGeneracion(java.time.LocalDateTime.now());
        nuevoCargo.setFechaVencimiento(java.time.LocalDate.now().plusDays(5));
        nuevoCargo.setMontoBase(conceptoRetiro.getMontoBase());
        nuevoCargo.setMontoDescuento(java.math.BigDecimal.ZERO);
        nuevoCargo.setMontoRecargo(java.math.BigDecimal.ZERO);
        nuevoCargo.setMontoTotal(conceptoRetiro.getMontoBase());

        // =========================================================================
        // SOLUCIÓN: Asignar número de cuota dinámico para evitar error en BD
        // =========================================================================
        int cuotaUnica = (int) (System.currentTimeMillis() % 1000000);
        nuevoCargo.setNumeroCuota(cuotaUnica);

        cargoEstudianteRepo.save(nuevoCargo);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "solicitud generada con éxito. para completar el retiro de " +
                capitalizarTexto(inscripcion.getGrupo().getMateria().getNombreMateria()) +
                ", por favor cancela el arancel en tu estado de cuenta.");
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> cancelarRetiroMateria(Long idEstudiante, Long idGrupo) {
        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("no hay ciclo activo."));

        List<Inscripcion> inscripciones = inscripcionRepo.findByEstudianteAndCiclo(idEstudiante, cicloActivo.getIdCiclo());
        Inscripcion inscripcion = inscripciones.stream()
                .filter(i -> i.getGrupo().getIdGrupo().equals(idGrupo))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("materia no encontrada."));

        if (!"RETIRO PENDIENTE".equalsIgnoreCase(inscripcion.getEstadoMateria().getEstadoMateria())) {
            throw new RuntimeException("esta materia no tiene una solicitud de retiro pendiente.");
        }

        ConceptoCobro conceptoRetiro = conceptoCobroRepo.findConceptoRetiroPorCiclo(cicloActivo.getIdCiclo())
                .orElseThrow(() -> new RuntimeException("no se encontró el concepto de retiro."));

        // =========================================================================
        // SOLUCIÓN: Borrar solo el cargo de retiro más reciente que esté pendiente
        // =========================================================================
        List<CargoEstudiante> cargosRetiro = cargoEstudianteRepo
                .findByMatricula_IdMatriculaAndEstadoCargo_EstadoCargo(inscripcion.getMatricula().getIdMatricula(), "PENDIENTE")
                .stream()
                .filter(c -> c.getConceptoCobro().getIdConceptoCobro().equals(conceptoRetiro.getIdConceptoCobro()))
                .sorted((c1, c2) -> {
                    Integer num1 = c1.getNumeroCuota() != null ? c1.getNumeroCuota() : 0;
                    Integer num2 = c2.getNumeroCuota() != null ? c2.getNumeroCuota() : 0;
                    return num2.compareTo(num1); // Orden Descendente
                })
                .collect(Collectors.toList());

        if (!cargosRetiro.isEmpty()) {
            cargoEstudianteRepo.delete(cargosRetiro.get(0));
        }

        EstadoMateriaEstudiante estadoCursando = estadoMateriaEstudianteRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("estado cursando no configurado."));
        inscripcion.setEstadoMateria(estadoCursando);
        inscripcionRepo.save(inscripcion);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "el proceso de retiro ha sido cancelado. sigues cursando " + capitalizarTexto(inscripcion.getGrupo().getMateria().getNombreMateria()));
        return response;
    }

    private String capitalizarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) return texto;
        String lower = texto.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}