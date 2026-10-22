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

        // Generamos el carnet definitivo (Ej. VM260016) y lo guardamos en BD
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

        // 6. GENERAR CARGOS FINANCIEROS (EXACTOS POR ID)
        List<ConceptoCobro> conceptosDelCiclo = conceptoCobroRepository.findByCiclo_IdCiclo(cicloActivo.getIdCiclo());

        // IDs permitidos según tipos_cobro_rows.sql: 1 = Matrícula Pregrado, 2 = Cuota Mensual Pregrado
        List<Long> idCobrosNuevos = List.of(1L, 2L);
        int numeroCuota = 1;

        for (ConceptoCobro concepto : conceptosDelCiclo) {
            Long idTipoCobro = concepto.getTipoCobro().getIdTipoCobro();

            // Si el cobro no es el ID 1 ni el 2, se descarta inmediatamente
            if (!idCobrosNuevos.contains(idTipoCobro)) {
                continue;
            }

            CargoEstudiante cargo = new CargoEstudiante();
            cargo.setMatricula(matricula);
            cargo.setConceptoCobro(concepto);
            cargo.setEstadoCargo(estadoCargoRepository.findById(1L).orElseThrow());
            cargo.setMontoBase(concepto.getMontoBase());
            cargo.setMontoDescuento(BigDecimal.ZERO);
            cargo.setMontoRecargo(BigDecimal.ZERO);
            cargo.setMontoTotal(concepto.getMontoBase());
            cargo.setFechaGeneracion(LocalDateTime.now());

            if (idTipoCobro == 1L) { // Matrícula
                cargo.setNumeroCuota(1);
                cargo.setFechaVencimiento(LocalDate.now().plusDays(3));
            } else if (idTipoCobro == 2L) { // Cuotas mensuales
                cargo.setNumeroCuota(numeroCuota);
                cargo.setFechaVencimiento(LocalDate.now().plusMonths(numeroCuota).withDayOfMonth(15));
                numeroCuota++;
            }

            cargoEstudianteRepository.save(cargo);
        }

        // 7. ARMAR RESPUESTA CORRECTA PARA EL FRONTEND (OCULTANDO EL CARNET)
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Pre-matrícula generada con éxito.");
        // Omitimos enviar "carnetGenerado" para que el frontend no pueda mostrarlo de ninguna forma
        respuesta.put("instrucciones", "Presente su documento de identidad (DUI/NIT) en Colectaduría. Sus credenciales y carnet oficial se enviarán a su correo personal al procesar el pago.");

        return respuesta;
    }

    // MÉTODO RESTAURADO: Genera el carnet real basado en el correlativo y el ciclo
    private String generarCarnet(String nombres, String apellidos, int anio) {
        String inicialApellido = apellidos != null && !apellidos.isEmpty() ? apellidos.substring(0, 1).toUpperCase() : "X";
        String inicialNombre = nombres != null && !nombres.isEmpty() ? nombres.substring(0, 1).toUpperCase() : "X";

        long correlativo = estudianteRepository.count() + 1;
        String numeroFormateado = String.format("%04d", correlativo);
        String anioCorto = String.valueOf(anio).substring(2);

        return inicialApellido + inicialNombre + anioCorto + numeroFormateado;
    }
}