package com.codice.sra.services;

import com.codice.sra.dtos.NuevoIngresoRequestDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NuevoIngresoService {

    private final PersonaRepository personaRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstudianteCarreraRepository estudianteCarreraRepository;
    private final MatriculaRepository matriculaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CargoEstudianteRepository cargoEstudianteRepository;
    private final ConceptoCobroRepository conceptoCobroRepository;
    private final GrupoRepository grupoRepository;
    private final CicloRepository cicloRepository;

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final DistritoRepository distritoRepository;
    private final CarreraSedeRepository carreraSedeRepository;
    private final PensumRepository pensumRepository;
    private final EstadoRegistroPersonaRepository estadoRegistroPersonaRepository;
    private final EstadoMatriculaRepository estadoMatriculaRepository;
    private final EstadoEstudianteRepository estadoEstudianteRepository;
    private final EstadoCargoRepository estadoCargoRepository;
    private final EstadoInscripcionRepository estadoInscripcionRepository;
    private final EstadoMateriaEstudianteRepository estadoMateriaEstudianteRepository;
    private final EstadoTrayectoriaRepository estadoTrayectoriaRepository;

    @Transactional
    public Map<String, Object> procesarPreMatricula(NuevoIngresoRequestDTO dto) {

        Ciclo cicloActivo = cicloRepository.findCicloActivo()
                .orElseThrow(() -> new RuntimeException("No hay ciclo activo para matricular."));

        // 1. CREAR O ACTUALIZAR PERSONA
        Persona persona;
        if (dto.getIdPersona() != null) {
            persona = personaRepository.findById(dto.getIdPersona())
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada."));
        } else {
            persona = new Persona();
            persona.setFechaRegistro(OffsetDateTime.now());
        }

        persona.setTipoDocumento(tipoDocumentoRepository.findById(dto.getIdTipoDocumento()).orElseThrow());
        persona.setNumeroDocumento(dto.getNumeroDocumento());
        persona.setNombres(dto.getNombres());
        persona.setApellidos(dto.getApellidos());
        persona.setFechaNacimiento(dto.getFechaNacimiento());
        persona.setSexo(dto.getSexo());
        persona.setTelefono(dto.getTelefono());
        persona.setCorreoPersonal(dto.getCorreoPersonal());
        persona.setDistrito(distritoRepository.findById(dto.getIdDistrito()).orElseThrow());
        persona.setDireccion(dto.getDireccion());
        persona.setEstadoRegistro(estadoRegistroPersonaRepository.findById(1L).orElseThrow());
        persona = personaRepository.save(persona);

        // 2. CREAR ESTUDIANTE (ASPIRANTE) Y GENERAR SU CARNET REAL
        Estudiante estudiante = new Estudiante();
        estudiante.setPersona(persona);
        estudiante.setUsuario(null);
        estudiante.setFechaIngreso(LocalDate.now());

        // Generamos el carnet definitivo y lo guardamos en BD
        estudiante.setCarnet(generarCarnet(persona.getNombres(), persona.getApellidos(), cicloActivo.getAnio()));
        estudiante.setEstadoEstudiante(estadoEstudianteRepository.findById(1L).orElseThrow());
        estudiante = estudianteRepository.save(estudiante);

        // 3. CREAR TRAYECTORIA ACADÉMICA
        EstudianteCarrera trayectoria = new EstudianteCarrera();
        trayectoria.setEstudiante(estudiante);
        trayectoria.setCarreraSede(carreraSedeRepository.findById(dto.getIdCarreraSede()).orElseThrow());
        trayectoria.setPensum(pensumRepository.findById(dto.getIdPensum().intValue()).orElseThrow());
        trayectoria.setFechaInicio(LocalDate.now());
        trayectoria.setEstadoTrayectoria(estadoTrayectoriaRepository.findById(1L).orElseThrow());
        trayectoria = estudianteCarreraRepository.save(trayectoria);

        // 4. CREAR MATRÍCULA
        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setEstudianteCarrera(trayectoria);
        matricula.setCiclo(cicloActivo);
        matricula.setFechaMatricula(LocalDateTime.now());
        matricula.setEstadoMatricula(estadoMatriculaRepository.findById(1L).orElseThrow());
        matricula = matriculaRepository.save(matricula);

        // 5. CREAR INSCRIPCIONES (Reservas de cupo)
        for (Long idGrupo : dto.getIdGruposSeleccionados()) {
            Grupo grupo = grupoRepository.findById(idGrupo)
                    .orElseThrow(() -> new RuntimeException("Grupo " + idGrupo + " no encontrado."));

            Inscripcion inscripcion = new Inscripcion();
            inscripcion.setMatricula(matricula);
            inscripcion.setGrupo(grupo);
            inscripcion.setFechaInscripcion(LocalDateTime.now());
            inscripcion.setEstadoInscripcion(estadoInscripcionRepository.findById(1L).orElseThrow());
            inscripcion.setEstadoMateria(estadoMateriaEstudianteRepository.findById(1L).orElseThrow());

            inscripcionRepository.save(inscripcion);
        }

        // 6. GENERAR CARGOS FINANCIEROS COMPLETOS
        List<ConceptoCobro> conceptosDelCiclo = conceptoCobroRepository.findByCiclo_IdCiclo(cicloActivo.getIdCiclo());
        List<Long> idCobrosNuevos = List.of(1L, 2L);

        // Determinar el mes y año de inicio según el ciclo
        int mesInicio = (cicloActivo.getNumeroCiclo() != null && cicloActivo.getNumeroCiclo() == 2) ? 7 : 1;
        int anioCiclo = cicloActivo.getAnio() != null ? cicloActivo.getAnio() : LocalDate.now().getYear();

        EstadoCargo estadoPendiente = estadoCargoRepository.findById(1L).orElseThrow();

        for (ConceptoCobro concepto : conceptosDelCiclo) {
            Long idTipoCobro = concepto.getTipoCobro().getIdTipoCobro();

            if (!idCobrosNuevos.contains(idTipoCobro)) {
                continue;
            }

            if (idTipoCobro == 1L) { // Generar Matrícula (Única)
                CargoEstudiante cargoMatricula = new CargoEstudiante();
                cargoMatricula.setMatricula(matricula);
                cargoMatricula.setConceptoCobro(concepto);
                cargoMatricula.setEstadoCargo(estadoPendiente);
                cargoMatricula.setMontoBase(concepto.getMontoBase());
                cargoMatricula.setMontoDescuento(BigDecimal.ZERO);
                cargoMatricula.setMontoRecargo(BigDecimal.ZERO);
                cargoMatricula.setMontoTotal(concepto.getMontoBase());
                cargoMatricula.setFechaGeneracion(LocalDateTime.now());
                cargoMatricula.setNumeroCuota(1);
                cargoMatricula.setFechaVencimiento(LocalDate.now().plusDays(3)); // Vence en 3 días para nuevos

                cargoEstudianteRepository.save(cargoMatricula);

            } else if (idTipoCobro == 2L) { // Generar las 6 Cuotas Mensuales del Ciclo
                for (int numCuota = 1; numCuota <= 6; numCuota++) {
                    CargoEstudiante cargoCuota = new CargoEstudiante();
                    cargoCuota.setMatricula(matricula);
                    cargoCuota.setConceptoCobro(concepto);
                    cargoCuota.setEstadoCargo(estadoPendiente);
                    cargoCuota.setMontoBase(concepto.getMontoBase());
                    cargoCuota.setMontoDescuento(BigDecimal.ZERO);
                    cargoCuota.setMontoRecargo(BigDecimal.ZERO);
                    cargoCuota.setMontoTotal(concepto.getMontoBase());
                    cargoCuota.setFechaGeneracion(LocalDateTime.now());

                    cargoCuota.setNumeroCuota(numCuota);

                    // Asignar mes de vencimiento (Día 15 de cada mes)
                    int mesActual = mesInicio + (numCuota - 1);
                    cargoCuota.setFechaVencimiento(LocalDate.of(anioCiclo, mesActual, 15));

                    cargoEstudianteRepository.save(cargoCuota);
                }
            }
        }

        // 7. ARMAR RESPUESTA
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Pre-matrícula generada con éxito.");
        respuesta.put("instrucciones", "Presente su documento de identidad (DUI/NIT) en Colectaduría. Sus credenciales y carnet oficial se enviarán a su correo personal al procesar el pago.");

        return respuesta;
    }

    private String generarCarnet(String nombres, String apellidos, int anio) {
        String inicialApellido = apellidos != null && !apellidos.isEmpty() ? apellidos.substring(0, 1).toUpperCase() : "X";
        String inicialNombre = nombres != null && !nombres.isEmpty() ? nombres.substring(0, 1).toUpperCase() : "X";

        long correlativo = estudianteRepository.count() + 1;
        String numeroFormateado = String.format("%04d", correlativo);
        String anioCorto = String.valueOf(anio).substring(2);

        return inicialApellido + inicialNombre + anioCorto + numeroFormateado;
    }
}