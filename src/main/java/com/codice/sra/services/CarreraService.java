package com.codice.sra.services;

import com.codice.sra.dtos.CarreraActualizacionRequestDTO;
import com.codice.sra.dtos.CarreraCreacionRequestDTO;
import com.codice.sra.dtos.CarreraDesactivacionRequestDTO;
import com.codice.sra.dtos.CarreraResponseDTO;
import com.codice.sra.models.Carrera;
import com.codice.sra.models.EstadoCarrera;
import com.codice.sra.models.Facultad;
import com.codice.sra.models.NivelAcademico;
import com.codice.sra.repositories.CarreraRepository;
import com.codice.sra.repositories.EstadoCarreraRepository;
import com.codice.sra.repositories.FacultadRepository;
import com.codice.sra.repositories.NivelAcademicoRepository;
import com.codice.sra.utils.CodigoCarreraUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarreraService {

    private final CarreraRepository carreraRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final FacultadRepository facultadRepository;
    private final EstadoCarreraRepository estadoCarreraRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<CarreraResponseDTO> listarCarrerasOfertables(Long idSede) {
        log.info("Ejecutando resolución de carreras ofertables para sede ID: [{}]", idSede);

        List<Carrera> carreras = (idSede != null && idSede > 0)
                ? carreraRepository.findCarrerasOfertablesPorSede(idSede)
                : carreraRepository.findAllCarrerasConPensumVigente();

        log.info("Total de carreras recuperadas para sede [{}]: {}", idSede, carreras.size());

        return carreras.stream()
                .map(this::mapearADTO)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Long crearCarrera(CarreraCreacionRequestDTO dto) {
        String nombreNormalizado = dto.getNombreCarrera().trim();

        // 1. Validar unicidad previa del nombre
        if (carreraRepository.existsByNombreCarreraIgnoreCase(nombreNormalizado)) {
            throw new IllegalArgumentException("El nombre de carrera '" + nombreNormalizado + "' ya se encuentra registrado");
        }

        // 2. Validar dependencias foráneas
        Facultad facultad = facultadRepository.findById(dto.getIdFacultad())
                .orElseThrow(() -> new IllegalArgumentException("Facultad no encontrada con ID: " + dto.getIdFacultad()));

        if (facultad.getEstadoFacultad() == null || facultad.getEstadoFacultad().getIdEstadoFacultad() != 1L) {
            throw new IllegalArgumentException("No se puede asociar una carrera a una facultad inactiva");
        }

        NivelAcademico nivel = nivelAcademicoRepository.findById(dto.getIdNivel())
                .orElseThrow(() -> new IllegalArgumentException("Nivel académico no encontrado con ID: " + dto.getIdNivel()));

        EstadoCarrera estadoInicial = estadoCarreraRepository.findByEstadoCarreraIgnoreCase("PLANIFICACION")
                .orElseThrow(() -> new IllegalStateException("El catálogo estados_carrera no contiene el estado 'PLANIFICACION'"));

        // 3. Generación automática del código mediante CodigoUtils y referencia por método
        String codigoGenerado = CodigoCarreraUtils.generarCodigoCarrera(
                nombreNormalizado,
                nivel.getIdNivel(),
                carreraRepository::existsByCodigoCarreraIgnoreCase
        );

        // 4. Instanciación y persistencia
        Carrera carrera = new Carrera();
        carrera.setCodigoCarrera(codigoGenerado);
        carrera.setNombreCarrera(nombreNormalizado);
        carrera.setFacultad(facultad);
        carrera.setNivel(nivel);
        carrera.setEstadoCarrera(estadoInicial);

        carreraRepository.save(carrera);

        // 5. Auditoría Forense obligatoria
        auditoriaService.registrarEvento(
                "CREACION_CARRERA",
                "carreras",
                carrera.getIdCarrera(),
                String.format("Alta de carrera: %s con código generado automático: %s",
                        nombreNormalizado, codigoGenerado)
        );

        return carrera.getIdCarrera();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void actualizarCarrera(Long idCarrera, CarreraActualizacionRequestDTO dto) {
        if (idCarrera == null || idCarrera <= 0) {
            throw new IllegalArgumentException("El ID de la carrera debe ser un número positivo");
        }

        // 1. Obtener entidad gestionada
        Carrera carrera = carreraRepository.findById(idCarrera)
                .orElseThrow(() -> new IllegalArgumentException("Carrera no encontrada con ID: " + idCarrera));

        String nombreNormalizado = dto.getNombreCarrera().trim();

        // 2. Detección No-Op (Idempotencia)
        boolean nombreIgual = Objects.equals(carrera.getNombreCarrera(), nombreNormalizado);
        boolean facultadIgual = carrera.getFacultad() != null &&
                Objects.equals(carrera.getFacultad().getIdFacultad(), dto.getIdFacultad());
        boolean nivelIgual = carrera.getNivel() != null &&
                Objects.equals(carrera.getNivel().getIdNivel(), dto.getIdNivel());

        if (nombreIgual && facultadIgual && nivelIgual) {
            return; // Salida temprana: Petición redundante, sin UPDATE ni fila vacía en auditoría
        }

        // 3. Unicidad excluyente del nombre (carreras_nombre_carrera_key)
        if (!nombreIgual && carreraRepository.existsByNombreCarreraIgnoreCaseAndIdCarreraNot(nombreNormalizado, idCarrera)) {
            throw new IllegalArgumentException("El nombre de carrera '" + nombreNormalizado + "' ya está registrado en otra carrera");
        }

        // 4. Validación de Facultad (debe existir y estar activa)
        if (!facultadIgual) {
            Facultad nuevaFacultad = facultadRepository.findByIdConEstado(dto.getIdFacultad())
                    .orElseThrow(() -> new IllegalArgumentException("Facultad no encontrada con ID: " + dto.getIdFacultad()));

            if (nuevaFacultad.getEstadoFacultad() == null || nuevaFacultad.getEstadoFacultad().getIdEstadoFacultad() != 1L) {
                throw new IllegalArgumentException("No se puede vincular una carrera a una facultad inactiva");
            }
            carrera.setFacultad(nuevaFacultad);
        }

        // 5. Validación de Nivel Académico
        if (!nivelIgual) {
            NivelAcademico nuevoNivel = nivelAcademicoRepository.findById(dto.getIdNivel())
                    .orElseThrow(() -> new IllegalArgumentException("Nivel académico no encontrado con ID: " + dto.getIdNivel()));
            carrera.setNivel(nuevoNivel);
        }

        // 6. Mutación del nombre (el código permanece intacto)
        carrera.setNombreCarrera(nombreNormalizado);

        // 7. Auditoría Forense obligatoria
        auditoriaService.registrarEvento(
                "ACTUALIZACION_CARRERA",
                "carreras",
                carrera.getIdCarrera(),
                String.format("Actualización de carrera [%s]. Nuevo nombre: '%s', Facultad ID: %d, Nivel ID: %d",
                        carrera.getCodigoCarrera(), nombreNormalizado, dto.getIdFacultad(), dto.getIdNivel())
        );

        // Hibernate Dirty Checking sincroniza automáticamente en PostgreSQL al confirmar la transacción
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void desactivarCarrera(Long idCarrera, CarreraDesactivacionRequestDTO dto) {
        if (idCarrera == null || idCarrera <= 0) {
            throw new IllegalArgumentException("El ID de la carrera debe ser un número positivo");
        }

        // 1. Obtener entidad gestionada
        Carrera carrera = carreraRepository.findById(idCarrera)
                .orElseThrow(() -> new IllegalArgumentException("Carrera no encontrada con ID: " + idCarrera));

        // 2. Obtener dinámicamente el estado INACTIVA desde la BD
        EstadoCarrera estadoInactivo = estadoCarreraRepository.findByEstadoCarreraIgnoreCase("INACTIVA")
                .orElseThrow(() -> new IllegalStateException("El catálogo estados_carrera no contiene el estado 'INACTIVA'"));

        // 3. Comprobación No-Op (Idempotencia)
        if (carrera.getEstadoCarrera() != null &&
                Objects.equals(carrera.getEstadoCarrera().getIdEstadoCarrera(), estadoInactivo.getIdEstadoCarrera())) {
            log.info("La carrera ID [{}] ya se encuentra en estado INACTIVA. No se ejecutan cambios.", idCarrera);
            return; // Salida temprana: no hay mutación ni registro redundante en auditoría
        }

        String motivoLimpio = dto.getMotivo().trim();

        // 4. Mutación de estado de dominio
        carrera.setEstadoCarrera(estadoInactivo);

        // 5. Auditoría Forense obligatoria
        auditoriaService.registrarEvento(
                "DESACTIVACION_CARRERA",
                "carreras",
                carrera.getIdCarrera(),
                String.format("Baja lógica de carrera [%s - %s]. Transición a INACTIVA. Motivo: %s",
                        carrera.getCodigoCarrera(), carrera.getNombreCarrera(), motivoLimpio)
        );

        log.info("Carrera ID [{}] desactivada exitosamente.", idCarrera);
        // Hibernate Dirty Checking se encarga de ejecutar el UPDATE al confirmar la transacción
    }

    private CarreraResponseDTO mapearADTO(Carrera c) {
        String facultadNombre = (c.getFacultad() != null)
                ? c.getFacultad().getNombreFacultad()
                : "Facultad Institucional";

        return new CarreraResponseDTO(
                c.getIdCarrera(),
                c.getCodigoCarrera(),
                c.getNombreCarrera(),
                facultadNombre,
                null,
                null
        );
    }
}