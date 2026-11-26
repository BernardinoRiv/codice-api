package com.codice.sra.services;

import com.codice.sra.dtos.ComprobantePagoDTO;
import com.codice.sra.dtos.MatriculaExterna.*;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.utils.ValidadorDocumentoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdmisionOnlineService {

    // Repositorios de Dominio
    private final PersonaRepository personaRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final GrupoRepository grupoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final MatriculaRepository matriculaRepository;
    private final EstudianteCarreraRepository estudianteCarreraRepository;
    private final CargoEstudianteRepository cargoEstudianteRepository;
    private final ConceptoCobroRepository conceptoCobroRepository;
    private final BeneficioRepository beneficioRepository;
    private final EstudianteBeneficioRepository estudianteBeneficioRepository;
    private final EstadoBeneficioEstudianteRepository estadoBeneficioEstudianteRepository;
    private final PagoRepository pagoRepository;
    private final PagoCargoRepository pagoCargoRepository;
    private final FacturaRepository facturaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final CanalPagoRepository canalPagoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final EstadoFacturaRepository estadoFacturaRepository;


    // Repositorios de Catálogos
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

    // Servicios de Negocio Orquestados
    private final CicloMatriculaValidatorService cicloMatriculaValidatorService;
    private final UsuarioService usuarioService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ComprobantePdfService comprobantePdfService;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<Long, CodigoAdmision> codigosAdmision = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, AutorizacionAdmision> autorizaciones = new ConcurrentHashMap<>();


    // Constantes de Dominio
    private static final Long ID_TIPO_COBRO_MATRICULA = 1L;       // Matrícula de Pregrado
    private static final Long ID_TIPO_COBRO_EXTRAORDINARIA = 5L;  // Arancel por Inscripción Extraordinaria

    // =========================================================================
    // CASO 1: VERIFICACIÓN PÚBLICA DE IDENTIDAD (Protección contra Enumeración)
    // =========================================================================
    @Transactional(readOnly = true)
    public VerificacionIdentidadPublicaResponseDTO verificarIdentidad(Long idTipoDoc, String numeroDoc) {
        String docLimpio = ValidadorDocumentoUtil.normalizar(numeroDoc);
        ValidadorDocumentoUtil.validar(idTipoDoc, docLimpio);

        // 1.1 Si ya es un estudiante activo con credenciales asignadas
        Optional<Estudiante> estudianteActivo = estudianteRepository
                .findByPersona_NumeroDocumentoAndUsuarioIsNotNull(docLimpio);

        if (estudianteActivo.isPresent()) {
            return VerificacionIdentidadPublicaResponseDTO.builder()
                    .existePersona(true)
                    .esEstudianteActivo(true)
                    .requiereLogin(true)
                    .mensaje("Ya posees un expediente institucional y usuario activo. Inicia sesión en el portal académico para inscribir tus asignaturas.")
                    .build();
        }

        // 1.2 Búsqueda en personas
        Optional<Persona> personaOpt = personaRepository.findByNumeroDocumento(docLimpio);
        if (personaOpt.isEmpty()) {
            return VerificacionIdentidadPublicaResponseDTO.builder()
                    .existePersona(false)
                    .esEstudianteActivo(false)
                    .requiereLogin(false)
                    .mensaje("Documento disponible. Completa el formulario para iniciar tu pre-matrícula.")
                    .build();
        }

        Persona persona = personaOpt.get();

        // 1.3 Bloqueo de Empleados / Administradores
        if (usuarioRepository.existsByPersona_IdPersonaAndRol_Rol(persona.getIdPersona(), "ADMINISTRADOR")) {
            return VerificacionIdentidadPublicaResponseDTO.builder()
                    .existePersona(true)
                    .esEstudianteActivo(false)
                    .requiereLogin(true)
                    .mensaje("Restricción: Los usuarios administrativos no pueden registrar pre-matrículas mediante este portal.")
                    .build();
        }

        // 1.4 NUEVA REGLA FAIL-FAST: ¿Ya tiene pre-matrícula en el ciclo actual?
        CicloMatriculaValidatorService.EvaluacionCicloMatricula evalCiclo =
                cicloMatriculaValidatorService.resolverCicloHabilParaMatricula();

        Optional<Estudiante> estudianteOpt = estudianteRepository.findByPersona_IdPersona(persona.getIdPersona());
        if (estudianteOpt.isPresent()) {
            boolean yaMatriculadoEnCiclo = matriculaRepository.existsByEstudiante_IdEstudianteAndCiclo_IdCiclo(
                    estudianteOpt.get().getIdEstudiante(),
                    evalCiclo.ciclo().getIdCiclo()
            );


            if (yaMatriculadoEnCiclo) {
                return VerificacionIdentidadPublicaResponseDTO.builder()
                        .existePersona(true)
                        .esEstudianteActivo(false)
                        .requiereLogin(false)
                        .requiereVerificacionCorreo(true)
                        .correoEnmascarado(
                                enmascararCorreo(persona.getCorreoPersonal())
                        )
                        .mensaje(
                                "Ya tienes una prematrícula registrada para el ciclo "
                                        + evalCiclo.ciclo().getCodigoCiclo()
                                        + ". Verifica tu correo para recuperar el trámite "
                                        + "y continuar con el pago."
                        )
                        .build();
            }

        }

        // 1.5 Si existe la persona pero no tiene matrícula vigente en este ciclo
        return VerificacionIdentidadPublicaResponseDTO.builder()
                .existePersona(true)
                .esEstudianteActivo(false)
                .requiereLogin(false)
                .requiereVerificacionCorreo(true)
                .correoEnmascarado(enmascararCorreo(persona.getCorreoPersonal()))
                .mensaje("Expediente encontrado. Se requiere verificar " + "el correo personal para continuar.")
                .build();
    }

    // =========================================================================
    // CASO 2: PROCESAMIENTO DE PRE-MATRÍCULA Y RESERVA EN LÍNEA (Fase 1)
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public MatriculaOnlineResponseDTO procesarPreMatriculaOnline(MatriculaOnlineRequestDTO dto) {

        // 1. Validación estricta del documento antes de procesar el registro civil
        String docLimpio = ValidadorDocumentoUtil.normalizar(dto.getNumeroDocumento());
        ValidadorDocumentoUtil.validar(dto.getIdTipoDocumento(), docLimpio);

        Optional<Persona> personaExistenteOpt =
                personaRepository.findByNumeroDocumento(docLimpio);

        if (personaExistenteOpt.isPresent()) {

            Persona existente = personaExistenteOpt.get();

            String token = dto.getTokenVerificacion();

            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException(
                        "Debes verificar tu correo antes de continuar."
                );
            }

            AutorizacionAdmision autorizacion = autorizaciones.get(token);

            if (autorizacion == null ||
                    !LocalDateTime.now().isBefore(autorizacion.expiracion()) ||
                    !Objects.equals(
                            autorizacion.idPersona(),
                            existente.getIdPersona()
                    )) {

                throw new IllegalArgumentException(
                        "La autorización de identidad no es válida o ha expirado."
                );
            }

            if (!Objects.equals(
                    existente.getTipoDocumento().getIdTipoDocumento(),
                    dto.getIdTipoDocumento())) {

                throw new IllegalArgumentException(
                        "El tipo de documento no coincide con el expediente."
                );
            }

            if (dto.getIdPersona() != null &&
                    !Objects.equals(
                            dto.getIdPersona(),
                            existente.getIdPersona())) {

                throw new IllegalArgumentException(
                        "La persona indicada no corresponde al documento."
                );
            }

            validarPersonaAdmisible(existente);


        } else {

            if (dto.getIdPersona() != null) {
                throw new IllegalArgumentException(
                        "La persona indicada no corresponde al documento."
                );
            }

            // Aspirante nuevo: debe verificar la posesión del correo.
            validarAutorizacionCorreoNuevo(
                    dto.getTokenVerificacion(),
                    dto.getIdTipoDocumento(),
                    docLimpio,
                    dto.getCorreoPersonal().trim().toLowerCase(Locale.ROOT)
            );
        }


        // 2. Validación de ciclo lectivo hábil y cálculo del período
        CicloMatriculaValidatorService.EvaluacionCicloMatricula evalCiclo =
                cicloMatriculaValidatorService.resolverCicloHabilParaMatricula();
        Ciclo cicloMatricula = evalCiclo.ciclo();
        boolean esExtraordinaria = evalCiclo.esExtraordinaria();

        // 2.1 Validación de unicidad de correo en la tabla personas (excluyendo a la misma persona si ya existe)
        String correoNormalizado = dto.getCorreoPersonal().trim().toLowerCase();
        Optional<Persona> personaPorCorreo = personaRepository.findByCorreoPersonal(correoNormalizado);
        if (personaPorCorreo.isPresent() && !personaPorCorreo.get().getNumeroDocumento().equalsIgnoreCase(docLimpio)) {
            throw new IllegalArgumentException("Ya existe una persona registrada con el correo personal: " + correoNormalizado);
        }

        // =============================================================
        // 2.2 GESTIÓN DE PERSONA: FIND OR CREATE
        // =============================================================

        Persona persona;

        if (personaExistenteOpt.isPresent()) {

            persona = personaExistenteOpt.get();

            // Se conservan los datos civiles existentes:
            // nombres, apellidos, fecha de nacimiento, sexo,
            // tipo de documento, número y correo personal.

            persona.setTelefono(dto.getTelefono().trim());

            persona.setDireccion(dto.getDireccion().trim());

            persona.setDistrito(
                    distritoRepository.findById(dto.getIdDistrito())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Distrito territorial no válido."
                                    )
                            )
            );

        } else {

            persona = new Persona();

            persona.setFechaRegistro(OffsetDateTime.now());

            persona.setTipoDocumento(
                    tipoDocumentoRepository
                            .findById(dto.getIdTipoDocumento())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Tipo de documento no válido."
                                    )
                            )
            );

            persona.setNumeroDocumento(docLimpio);

            persona.setNombres(dto.getNombres().trim());
            persona.setApellidos(dto.getApellidos().trim());

            persona.setFechaNacimiento(dto.getFechaNacimiento());

            persona.setSexo(
                    dto.getSexo().trim().toUpperCase()
            );

            persona.setTelefono(dto.getTelefono().trim());

            persona.setCorreoPersonal(correoNormalizado);

            persona.setDistrito(
                    distritoRepository.findById(dto.getIdDistrito())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Distrito territorial no válido."
                                    )
                            )
            );

            persona.setDireccion(dto.getDireccion().trim());
        }

        // Estado de registro

        EstadoRegistroPersona estadoRegistro =
                estadoRegistroPersonaRepository
                        .findByEstadoRegistro("PENDIENTE")
                        .orElseGet(() ->
                                estadoRegistroPersonaRepository
                                        .findById(1L)
                                        .orElseThrow()
                        );

        persona.setEstadoRegistro(estadoRegistro);


// Guardar una sola vez.
        persona = personaRepository.save(persona);

        // =========================================================================
        // 2.3 GESTIÓN DEFENSIVA DE ESTUDIANTE (PATRÓN FIND-OR-CREATE)
        // =========================================================================

        // Referencia final para utilizar dentro de las expresiones lambda
        final Persona personaGuardada = persona;

        Estudiante estudiante = estudianteRepository
                .findByPersona_IdPersona(personaGuardada.getIdPersona())
                .orElseGet(() -> {
                    EstadoEstudiante estadoEst = estadoEstudianteRepository
                            .findByEstadoEstudiante("ACTIVO")
                            .orElseGet(() -> estadoEstudianteRepository
                                    .findById(1L)
                                    .orElseThrow());

                    Estudiante nuevo = new Estudiante();
                    nuevo.setPersona(personaGuardada);
                    nuevo.setUsuario(null);
                    nuevo.setFechaIngreso(LocalDate.now());
                    nuevo.setEstadoEstudiante(estadoEst);
                    nuevo.setCarnet(generarCarnetDeterministico(
                            personaGuardada.getNombres(),
                            personaGuardada.getApellidos(),
                            cicloMatricula.getAnio()));

                    return estudianteRepository.save(nuevo);
                });


        // 2.3.1 Verificación de rol y credenciales existentes
        if (estudiante.getUsuario() != null) {
            throw new IllegalStateException("El estudiante ya posee credenciales activas. Debe realizar su inscripción desde el portal institucional.");
        }

        // =========================================================================
        // 2.3.2 CONTROL DE IDEMPOTENCIA: VALIDAR MATRÍCULA DUPLICADA EN ESTE CICLO
        // =========================================================================
        validarSinMatriculaEnCiclo(estudiante, cicloMatricula);


        // =========================================================================
        // 2.4 REGISTRO DE LA TRAYECTORIA ACADÉMICA ACTIVA
        // =========================================================================
        EstadoTrayectoria estadoTray = estadoTrayectoriaRepository.findByEstadoTrayectoria("ACTIVA")
                .orElseGet(() -> estadoTrayectoriaRepository.findById(1L).orElseThrow());

        CarreraSede carreraSede = carreraSedeRepository.findById(dto.getIdCarreraSede())
                .orElseThrow(() -> new IllegalArgumentException("Oferta carrera-sede no encontrada."));
        Pensum pensum = pensumRepository.findByIdPensumLong(dto.getIdPensum())
                .orElseThrow(() -> new IllegalArgumentException("Plan de estudios no encontrado."));

        // Se reutiliza la trayectoria si ya existe para esa carrera-sede o se crea una nueva
        EstudianteCarrera trayectoria = estudianteCarreraRepository
                .findByEstudiante_IdEstudianteAndCarreraSede_IdCarreraSede(estudiante.getIdEstudiante(), carreraSede.getIdCarreraSede())
                .orElseGet(() -> {
                    EstudianteCarrera nuevaTrayectoria = new EstudianteCarrera();
                    nuevaTrayectoria.setEstudiante(estudiante);
                    nuevaTrayectoria.setCarreraSede(carreraSede);
                    nuevaTrayectoria.setPensum(pensum);
                    nuevaTrayectoria.setFechaInicio(LocalDate.now());
                    nuevaTrayectoria.setEstadoTrayectoria(estadoTray);
                    return estudianteCarreraRepository.save(nuevaTrayectoria);
                });

        // =========================================================================
        // 2.5 CREACIÓN DE MATRÍCULA (PENDIENTE_PAGO)
        // =========================================================================
        EstadoMatricula estadoMat = estadoMatriculaRepository.findByEstadoMatricula("PENDIENTE_PAGO")
                .orElseGet(() -> estadoMatriculaRepository.findById(1L).orElseThrow());

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setEstudianteCarrera(trayectoria);
        matricula.setCiclo(cicloMatricula);
        matricula.setFechaMatricula(LocalDateTime.now());
        matricula.setEstadoMatricula(estadoMat);
        matricula = matriculaRepository.save(matricula);

        // =========================================================================
        // 2.6 RESERVA ATÓMICA DE CUPOS CON BLOQUEO PESIMISTA
        // =========================================================================
        EstadoInscripcion estadoInsc = estadoInscripcionRepository.findByEstadoInscripcion("ACTIVA")
                .orElseGet(() -> estadoInscripcionRepository.findById(1L).orElseThrow());
        EstadoMateriaEstudiante estadoMatEst = estadoMateriaEstudianteRepository.findByEstadoMateria("CURSANDO")
                .orElseGet(() -> estadoMateriaEstudianteRepository.findById(1L).orElseThrow());

        for (Long idGrupo : dto.getIdGruposSeleccionados()) {
            Grupo grupo = grupoRepository.findByIdWithLock(idGrupo)
                    .orElseThrow(() -> new IllegalArgumentException("Grupo con ID " + idGrupo + " no encontrado."));

            if (!Objects.equals(grupo.getCiclo().getIdCiclo(), cicloMatricula.getIdCiclo())) {
                throw new IllegalArgumentException(String.format(
                        "Inconsistencia académica: El grupo '%s' (%s) pertenece al ciclo %s, pero la matrícula corresponde al ciclo %s.",
                        grupo.getCodigoGrupo(),
                        grupo.getMateria().getNombreMateria(),
                        grupo.getCiclo().getCodigoCiclo(),
                        cicloMatricula.getCodigoCiclo()
                ));
            }

            long inscritosActuales = inscripcionRepository.countByGrupo_IdGrupo(grupo.getIdGrupo());
            if (inscritosActuales >= grupo.getCupoMaximo()) {
                throw new IllegalStateException("El grupo " + grupo.getCodigoGrupo() + " para la asignatura " +
                        grupo.getMateria().getNombreMateria() + " agotó sus cupos disponibles.");
            }

            Inscripcion inscripcion = new Inscripcion();
            inscripcion.setMatricula(matricula);
            inscripcion.setGrupo(grupo);
            inscripcion.setFechaInscripcion(LocalDateTime.now());
            inscripcion.setEstadoInscripcion(estadoInsc);
            inscripcion.setEstadoMateria(estadoMatEst);
            inscripcionRepository.save(inscripcion);
        }

        // =========================================================================
        // 2.7 GENERACIÓN DEL PLAN FINANCIERO DEL CICLO COMPLETO
        // =========================================================================
        EstadoCargo estadoPendiente = estadoCargoRepository.findByEstadoCargo("PENDIENTE")
                .orElseGet(() -> estadoCargoRepository.findById(1L).orElseThrow());

        // Concepto: Matrícula de Pregrado[cite: 3]
        boolean posgrado = esPosgrado(carreraSede.getCarrera());
        List<ConceptoCobro> arancelesNivel = conceptoCobroRepository.findByCiclo_IdCicloAndActivoTrue(cicloMatricula.getIdCiclo());
        ConceptoCobro arancelMatricula = arancelesNivel.stream()
                .filter(c -> nombreTipo(c).contains("matricul"))
                .filter(c -> correspondeNivel(c, posgrado))
                .filter(c -> !nombreTipo(c).contains("tesis"))
                .findFirst().orElseThrow(() -> new IllegalStateException("No hay arancel de matrícula configurado para el nivel " + carreraSede.getCarrera().getNivel().getNivel()));
        ConceptoCobro arancelCuota = arancelesNivel.stream()
                .filter(c -> nombreTipo(c).contains("cuota") || nombreTipo(c).contains("mensual"))
                .filter(c -> correspondeNivel(c, posgrado))
                .filter(c -> !nombreTipo(c).contains("tesis"))
                .findFirst().orElseThrow(() -> new IllegalStateException("No hay arancel de cuota configurado para el nivel " + carreraSede.getCarrera().getNivel().getNivel()));

        EstudianteBeneficio beneficioAsignado = asignarBeneficio(dto.getIdBeneficio(), estudiante);
        String nombreBeneficio = beneficioAsignado == null ? "" : beneficioAsignado.getBeneficio().getBeneficio().toLowerCase(Locale.ROOT);
        boolean becaCompleta = nombreBeneficio.contains("100%") || nombreBeneficio.contains("completa") || nombreBeneficio.contains("institucional")
                || (beneficioAsignado != null && beneficioAsignado.getBeneficio().getPorcentaje() != null
                && beneficioAsignado.getBeneficio().getPorcentaje().compareTo(new BigDecimal("100")) >= 0);
        boolean becaTerceros = nombreBeneficio.contains("tercero") || nombreBeneficio.contains("externa");

        LocalDate fechaLimitePreMatricula = LocalDate.now().plusDays(2); // 48 horas de vigencia

        // 1. Cargo: Matrícula
        CargoEstudiante cargoMatricula = new CargoEstudiante();
        cargoMatricula.setMatricula(matricula);
        cargoMatricula.setConceptoCobro(arancelMatricula);
        EstadoCargo estadoCargoMatricula = becaCompleta ? estadoCargoRepository.findById(2L).orElseThrow() : estadoPendiente;
        cargoMatricula.setEstadoCargo(estadoCargoMatricula);
        cargoMatricula.setMontoBase(arancelMatricula.getMontoBase());
        cargoMatricula.setMontoDescuento(BigDecimal.ZERO);
        cargoMatricula.setMontoRecargo(BigDecimal.ZERO);
        cargoMatricula.setMontoDescuento(becaCompleta ? arancelMatricula.getMontoBase() : BigDecimal.ZERO);
        cargoMatricula.setMontoTotal(becaCompleta ? BigDecimal.ZERO : arancelMatricula.getMontoBase());
        cargoMatricula.setEstudianteBeneficio(beneficioAsignado);
        cargoMatricula.setFechaGeneracion(LocalDateTime.now());
        cargoMatricula.setNumeroCuota(1);
        cargoMatricula.setFechaVencimiento(fechaLimitePreMatricula);
        cargoMatricula = cargoEstudianteRepository.save(cargoMatricula);

        // 2. Cargo: Extraordinaria (si aplica)
        BigDecimal totalExtraordinaria = BigDecimal.ZERO;
        if (esExtraordinaria) {
            ConceptoCobro arancelExtra = conceptoCobroRepository
                    .findByCiclo_IdCicloAndTipoCobro_IdTipoCobroAndActivoTrue(cicloMatricula.getIdCiclo(), ID_TIPO_COBRO_EXTRAORDINARIA)
                    .orElseThrow(() -> new IllegalStateException("Arancel de matrícula extraordinaria no configurado para este ciclo."));

            CargoEstudiante cargoExtra = new CargoEstudiante();
            cargoExtra.setMatricula(matricula);
            cargoExtra.setConceptoCobro(arancelExtra);
            cargoExtra.setEstadoCargo(estadoPendiente);
            cargoExtra.setMontoBase(arancelExtra.getMontoBase());
            cargoExtra.setMontoDescuento(BigDecimal.ZERO);
            cargoExtra.setMontoRecargo(BigDecimal.ZERO);
            cargoExtra.setMontoTotal(arancelExtra.getMontoBase());
            cargoExtra.setFechaGeneracion(LocalDateTime.now());
            cargoExtra.setNumeroCuota(1);
            cargoExtra.setFechaVencimiento(fechaLimitePreMatricula);
            cargoEstudianteRepository.save(cargoExtra);

            totalExtraordinaria = arancelExtra.getMontoBase();
        }

        // 3. Proyección de las 5 cuotas del ciclo (Cuotas 1 a 5)
        LocalDate fechaBaseCiclo = (cicloMatricula.getFechaInicio() != null) ? cicloMatricula.getFechaInicio() : LocalDate.now();
        for (int i = 1; i <= 6; i++) {
            CargoEstudiante cuota = new CargoEstudiante();
            cuota.setMatricula(matricula);
            cuota.setConceptoCobro(arancelCuota);
            cuota.setEstadoCargo(becaCompleta ? estadoCargoMatricula : estadoPendiente);
            cuota.setMontoBase(arancelCuota.getMontoBase());
            BigDecimal descuento = becaTerceros || beneficioAsignado == null ? BigDecimal.ZERO
                    : becaCompleta ? arancelCuota.getMontoBase() : calcularDescuento(beneficioAsignado.getBeneficio(), arancelCuota.getMontoBase());
            cuota.setMontoDescuento(descuento);
            cuota.setMontoRecargo(BigDecimal.ZERO);
            cuota.setMontoTotal(arancelCuota.getMontoBase().subtract(descuento).max(BigDecimal.ZERO));
            cuota.setEstudianteBeneficio(beneficioAsignado);
            cuota.setFechaGeneracion(LocalDateTime.now());
            cuota.setNumeroCuota(i);
            cuota.setFechaVencimiento(i == 1 ? fechaLimitePreMatricula : fechaBaseCiclo.plusMonths(i - 1));
            cargoEstudianteRepository.save(cuota);
        }

        // Regla de Negocio 1 + 1: Matrícula ($70.00) + Primera Cuota Mensual ($70.00) obligatorias[cite: 3]
        BigDecimal totalInicialExigible = cargoMatricula.getMontoTotal()
                .add(arancelCuota.getMontoBase().subtract(becaTerceros || beneficioAsignado == null ? BigDecimal.ZERO : becaCompleta ? arancelCuota.getMontoBase() : calcularDescuento(beneficioAsignado.getBeneficio(), arancelCuota.getMontoBase())).max(BigDecimal.ZERO))
                .add(totalExtraordinaria);

        if (becaCompleta) {
            matricula.setEstadoMatricula(estadoMatriculaRepository.findByEstadoMatricula("ACTIVA")
                    .or(() -> estadoMatriculaRepository.findByEstadoMatricula("MATRICULADA"))
                    .orElseGet(() -> estadoMatriculaRepository.findById(2L).orElseThrow()));
            matriculaRepository.save(matricula);
            EstadoInscripcion inscrito = estadoInscripcionRepository.findByEstadoInscripcion("INSCRITO")
                    .orElseGet(() -> estadoInscripcionRepository.findByEstadoInscripcion("ACTIVA").orElseThrow());
            for (Inscripcion inscripcion : inscripcionRepository.findByMatricula_IdMatricula(matricula.getIdMatricula())) {
                inscripcion.setEstadoInscripcion(inscrito);
                inscripcionRepository.save(inscripcion);
            }
            Usuario usuario = usuarioService.aprovisionarUsuarioEstudiante(persona, estudiante.getCarnet());
            estudiante.setUsuario(usuario);
            estudianteRepository.save(estudiante);
            List<ComprobantePagoDTO.DetalleComprobanteDTO> detalleBeca = new ArrayList<>();
            List<CargoEstudiante> cargosBeca = cargoEstudianteRepository.findByMatricula_IdMatricula(matricula.getIdMatricula());
            BigDecimal subtotalBeca = BigDecimal.ZERO;
            BigDecimal descuentoBeca = BigDecimal.ZERO;
            for (CargoEstudiante cargo : cargosBeca) {
                BigDecimal base = cargo.getMontoBase();
                detalleBeca.add(ComprobantePagoDTO.DetalleComprobanteDTO.builder()
                        .concepto(cargo.getConceptoCobro().getTipoCobro().getTipoCobro())
                        .montoBase(base).montoDescuento(base).montoTotal(BigDecimal.ZERO).build());
                subtotalBeca = subtotalBeca.add(base);
                descuentoBeca = descuentoBeca.add(base);
            }
            String destino = persona.getCorreoPersonal();
            String nombre = persona.getNombres() + " " + persona.getApellidos();
            String carnet = estudiante.getCarnet();
            String correoInstitucional = usuario.getCorreoInstitucional();
            String claveTemporal = carnet + "Uma*";
            String numeroFactura = "BECA-100-" + matricula.getIdMatricula();
            ComprobantePagoDTO comprobanteBeca = ComprobantePagoDTO.builder()
                    .numeroFactura(numeroFactura).fechaEmision(LocalDateTime.now())
                    .nombreCompleto(nombre).carnetEstudiante(carnet).correoEstudiante(destino)
                    .subtotal(subtotalBeca).descuento(descuentoBeca).totalCobrado(BigDecimal.ZERO)
                    .cajeroResponsable("ADJUDICACIÓN DE BECA INSTITUCIONAL 100%")
                    .detalles(detalleBeca).build();
            byte[] pdfBeca = comprobantePdfService.generarPdf(comprobanteBeca);
            if (destino != null && !destino.isBlank()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override public void afterCommit() {
                                emailService.enviarCredencialesYComprobanteBeca(destino, nombre, carnet,
                                        correoInstitucional, claveTemporal, numeroFactura, pdfBeca);
                            }
                        });
            }
        }

        // Retorno limpio sin enviar correos ni validar el canal de pago aquí
        return MatriculaOnlineResponseDTO.builder()
                .idMatricula(matricula.getIdMatricula())
                .totalAPagar(totalInicialExigible)
                .fechaVencimiento(fechaLimitePreMatricula)
                .mensaje(becaCompleta ? "Matrícula confirmada por beca institucional del 100%. Comprobante y credenciales enviados al correo."
                        : becaTerceros ? "Cupos reservados. Beneficio de terceros registrado; el arancel se paga íntegro."
                        : "Cupos reservados exitosamente. Procede a seleccionar tu método de pago.")
                .build();
    }



    // ============================================================
    // CONSULTA FINANCIERA SIN CREAR PREMATRÍCULA
    // ============================================================
    @Transactional(readOnly = true)
    public Map<String, Object> consultarPlanFinanciero(Long idCarreraSede) {

        // Obtener ciclo habilitado y período de matrícula.
        CicloMatriculaValidatorService.EvaluacionCicloMatricula evaluacion =
                cicloMatriculaValidatorService.resolverCicloHabilParaMatricula();

        Ciclo ciclo = evaluacion.ciclo();

        // Matrícula ordinaria.
        if (idCarreraSede == null) throw new IllegalArgumentException("idCarreraSede es obligatorio para calcular el arancel.");
        Carrera carrera = carreraSedeRepository.findById(idCarreraSede)
                .orElseThrow(() -> new IllegalArgumentException("Oferta carrera-sede no encontrada.")).getCarrera();
        boolean posgrado = esPosgrado(carrera);
        List<ConceptoCobro> conceptos = conceptoCobroRepository.findByCiclo_IdCicloAndActivoTrue(ciclo.getIdCiclo());
        ConceptoCobro conceptoMatricula = conceptos.stream().filter(c -> nombreTipo(c).contains("matricul"))
                .filter(c -> correspondeNivel(c, posgrado)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe arancel de matrícula para el nivel seleccionado."));

        // Cuota mensual.
        ConceptoCobro conceptoCuota = conceptos.stream().filter(c -> nombreTipo(c).contains("cuota") || nombreTipo(c).contains("mensual"))
                .filter(c -> correspondeNivel(c, posgrado)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe arancel de cuota mensual para el nivel seleccionado."));

        BigDecimal montoMatricula = conceptoMatricula.getMontoBase();
        BigDecimal montoCuota = conceptoCuota.getMontoBase();

        // Matrícula extraordinaria, cuando corresponda.
        BigDecimal montoExtraordinaria = BigDecimal.ZERO;

        if (evaluacion.esExtraordinaria()) {
            ConceptoCobro conceptoExtra = conceptoCobroRepository
                    .findByCiclo_IdCicloAndTipoCobro_IdTipoCobroAndActivoTrue(
                            ciclo.getIdCiclo(),
                            ID_TIPO_COBRO_EXTRAORDINARIA
                    )
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe arancel extraordinario para el ciclo."
                    ));

            montoExtraordinaria = conceptoExtra.getMontoBase();
        }

        int numeroCuotas = 5;

        BigDecimal totalCuotas = montoCuota.multiply(
                BigDecimal.valueOf(numeroCuotas)
        );

        BigDecimal totalCiclo = montoMatricula
                .add(totalCuotas)
                .add(montoExtraordinaria);

        BigDecimal totalInicial = montoMatricula
                .add(montoCuota)
                .add(montoExtraordinaria);

        Map<String, Object> resultado = new LinkedHashMap<>();

        resultado.put("idCiclo", ciclo.getIdCiclo());
        resultado.put("idCarrera", carrera.getIdCarrera());
        resultado.put("nivelAcademico", carrera.getNivel().getNivel());
        resultado.put("codigoCiclo", ciclo.getCodigoCiclo());
        resultado.put("esExtraordinaria", evaluacion.esExtraordinaria());
        resultado.put("montoMatricula", montoMatricula);
        resultado.put("montoCuota", montoCuota);
        resultado.put("numeroCuotas", numeroCuotas);
        resultado.put("montoExtraordinaria", montoExtraordinaria);
        resultado.put("totalCuotas", totalCuotas);
        resultado.put("totalCiclo", totalCiclo);
        resultado.put("totalInicial", totalInicial);
        resultado.put("mensaje",
                "Plan financiero calculado. No se ha creado ninguna prematrícula.");

        return resultado;
    }

    private String nombreTipo(ConceptoCobro concepto) {
        return concepto.getTipoCobro().getTipoCobro().toLowerCase(Locale.ROOT);
    }

    private boolean esPosgrado(Carrera carrera) {
        String nivel = carrera.getNivel().getNivel().toLowerCase(Locale.ROOT);
        return nivel.contains("posgrado") || nivel.contains("maestr") || nivel.contains("postgrado");
    }

    private boolean correspondeNivel(ConceptoCobro concepto, boolean posgrado) {
        String tipo = nombreTipo(concepto);
        boolean conceptoPosgrado = tipo.contains("maestr") || tipo.contains("posgrado") || tipo.contains("postgrado");
        boolean conceptoPregrado = tipo.contains("pregrado");
        if (tipo.contains("tesis")) return false;
        return posgrado ? conceptoPosgrado : conceptoPregrado && !conceptoPosgrado;
    }

    private EstudianteBeneficio asignarBeneficio(Long idBeneficio, Estudiante estudiante) {
        if (idBeneficio == null) return null;
        Beneficio beneficio = beneficioRepository.findById(idBeneficio)
                .orElseThrow(() -> new IllegalArgumentException("Beneficio no encontrado."));
        LocalDate hoy = LocalDate.now();
        if (!Boolean.TRUE.equals(beneficio.getEstadoBeneficio().getActivo())
                || beneficio.getFechaInicio().isAfter(hoy)
                || (beneficio.getFechaFin() != null && beneficio.getFechaFin().isBefore(hoy)))
            throw new IllegalArgumentException("El beneficio seleccionado no está activo.");
        EstudianteBeneficio asignacion = new EstudianteBeneficio();
        asignacion.setEstudiante(estudiante);
        asignacion.setBeneficio(beneficio);
        asignacion.setEstadoBeneficioEstudiante(estadoBeneficioEstudianteRepository
                .findByEstadoBeneficioEstudiante("ACTIVO").orElseThrow());
        asignacion.setFechaSolicitud(LocalDateTime.now());
        asignacion.setFechaAprobacion(LocalDateTime.now());
        return estudianteBeneficioRepository.save(asignacion);
    }

    private BigDecimal calcularDescuento(Beneficio beneficio, BigDecimal base) {
        BigDecimal descuento = beneficio.getPorcentaje() != null
                ? base.multiply(beneficio.getPorcentaje()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP)
                : beneficio.getMonto() != null ? beneficio.getMonto() : BigDecimal.ZERO;
        return descuento.min(base).max(BigDecimal.ZERO);
    }

    private void normalizarFranjaLegacy(CargoEstudiante cargo) {
        String tipo = cargo.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase(Locale.ROOT);
        if (tipo.contains("franja becaria") && cargo.getMontoDescuento().compareTo(BigDecimal.ZERO) == 0) {
            cargo.setMontoBase(cargo.getMontoTotal().add(new BigDecimal("17.00")));
            cargo.setMontoDescuento(new BigDecimal("17.00"));
        }
    }


    // =========================================================================
    // CASO 3: LIQUIDACIÓN EN LÍNEA, DETONANTE IAM Y FACTURACIÓN (Fase 2)
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public ConfirmacionMatriculaOnlineDTO liquidarMatriculaOnline(String idempotencyKey, PagoOnlineRequestDTO dto) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("La cabecera 'Idempotency-Key' es requerida para procesar transacciones bancarias.");
        }

        // 1. Control de Idempotencia: Transacciones concurrentes o reintentos
        if (pagoRepository.existsByReferencia(idempotencyKey)) {
            Pago pagoPrevio = pagoRepository.findByReferencia(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Error al recuperar el pago existente."));
            Factura facturaPrevia = facturaRepository.findByPago_IdPago(pagoPrevio.getIdPago())
                    .orElseThrow(() -> new IllegalStateException("Factura no localizada para el pago registrado."));

            return ConfirmacionMatriculaOnlineDTO.builder()
                    .carnet(pagoPrevio.getEstudiante().getCarnet())
                    .correoInstitucional(pagoPrevio.getEstudiante().getUsuario() != null
                            ? pagoPrevio.getEstudiante().getUsuario().getCorreoInstitucional() : "En proceso")
                    .numeroFactura(facturaPrevia.getNumeroFactura())
                    .numeroAutorizacion(pagoPrevio.getNumeroAutorizacion())
                    .montoPagado(pagoPrevio.getMontoPagado())
                    .fechaPago(pagoPrevio.getFechaPago())
                    .mensaje("Transacción liquidada previamente con éxito.")
                    .build();
        }

        Matricula matricula = matriculaRepository.findById(dto.getIdMatricula())
                .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada con ID: " + dto.getIdMatricula()));

        if (!"PENDIENTE_PAGO".equalsIgnoreCase(matricula.getEstadoMatricula().getEstadoMatricula())) {
            throw new IllegalStateException("La matrícula indicada ya se encuentra liquidada o cancelada.");
        }

        Estudiante estudiante = matricula.getEstudiante();
        Persona persona = estudiante.getPersona();

        // 2. Recuperar todos los cargos pendientes de esta matrícula
        List<CargoEstudiante> cargosPendientes = cargoEstudianteRepository
                .findByMatricula_IdMatriculaAndEstadoCargo_EstadoCargoOrderByNumeroCuotaAsc(matricula.getIdMatricula(), "PENDIENTE");

        if (cargosPendientes.isEmpty()) {
            throw new IllegalStateException("No se encontraron obligaciones financieras pendientes asociadas a la matrícula.");
        }

        // 3. Resolución de la canasta de cobro (Regla FIFO: Matrícula + Cuotas seleccionadas)
        int cuotasMensualesDeseadas = (dto.getCuotasAPagar() != null && dto.getCuotasAPagar() >= 1)
                ? Math.min(dto.getCuotasAPagar(), 5)
                : 1;

        List<CargoEstudiante> cargosALiquidar = new ArrayList<>();

        // 3.1 Identificar aranceles iniciales de ingreso (Matrícula y Extraordinaria si existe)
        List<CargoEstudiante> cargosEntrada = cargosPendientes.stream()
                .filter(c -> {
                    Long idTipo = c.getConceptoCobro().getTipoCobro().getIdTipoCobro();
                    String nombreTipo = c.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase();
                    return Objects.equals(idTipo, ID_TIPO_COBRO_MATRICULA)
                            || Objects.equals(idTipo, ID_TIPO_COBRO_EXTRAORDINARIA)
                            || nombreTipo.contains("matrícul")
                            || nombreTipo.contains("matricul");
                })
                .toList();
        cargosALiquidar.addAll(cargosEntrada);

        // 3.2 Identificar cuotas mensuales pendientes ordenadas secuencialmente (Cuotas 1 a 5)[cite: 2]
        List<CargoEstudiante> cuotasMensuales = cargosPendientes.stream()
                .filter(c -> !cargosEntrada.contains(c))
                .sorted(Comparator.comparingInt(CargoEstudiante::getNumeroCuota))
                .toList();

        // Regla de Negocio: Es obligatorio liquidar al menos la primera cuota junto con la matrícula[cite: 2]
        if (!cargosEntrada.isEmpty() && cuotasMensuales.isEmpty()) {
            throw new IllegalStateException("Inconsistencia financiera: no se encontraron las cuotas del ciclo para la matrícula.");
        }

        // Agregar las cuotas solicitadas en estricto orden cronológico (FIFO)[cite: 2]
        for (int i = 0; i < cuotasMensualesDeseadas && i < cuotasMensuales.size(); i++) {
            cargosALiquidar.add(cuotasMensuales.get(i));
        }

        // 4. Calcular el importe total exacto de los cargos seleccionados
        BigDecimal totalCobrar = cargosALiquidar.stream()
                .map(CargoEstudiante::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Asentamiento contable del Pago (Alineado a las tablas maestras de Finanzas)
        String autorizacionPasarela = "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Pago pago = new Pago();
        pago.setEstudiante(estudiante);
        pago.setMetodoPago(metodoPagoRepository.findByMetodoPago("TARJETA")
                .orElseGet(() -> metodoPagoRepository.findById(2L).orElseThrow())); // ID 2 = TARJETA
        pago.setCanalPago(canalPagoRepository.findByCanalPago("PORTAL")
                .orElseGet(() -> canalPagoRepository.findById(2L).orElseThrow()));   // ID 2 = PORTAL/WEB
        pago.setEstadoPago(estadoPagoRepository.findByEstadoPago("APROBADO")
                .orElseGet(() -> estadoPagoRepository.findById(1L).orElseThrow())); // ID 1 = APROBADO (alineado a FinanzasService)
        pago.setMontoPagado(totalCobrar);
        pago.setFechaPago(LocalDateTime.now());
        pago.setNumeroAutorizacion(autorizacionPasarela);
        pago.setReferencia(idempotencyKey);
        pago = pagoRepository.save(pago);

        // 6. Transicionar únicamente los cargos liquidados a PAGADO y registrar pagos_cargos
        EstadoCargo estadoPagado = estadoCargoRepository.findByEstadoCargo("PAGADO")
                .orElseGet(() -> estadoCargoRepository.findById(2L).orElseThrow()); // ID 2 = PAGADO

        for (CargoEstudiante cargo : cargosALiquidar) {
            normalizarFranjaLegacy(cargo);
            cargo.setEstadoCargo(estadoPagado);
            cargoEstudianteRepository.save(cargo);

            PagoCargo pc = new PagoCargo();
            pc.setPago(pago);
            pc.setCargo(cargo);
            pc.setMontoAplicado(cargo.getMontoTotal());
            pagoCargoRepository.save(pc);
        }

        // 7. Emisión de Factura Legal por el monto real cobrado
        Factura factura = new Factura();
        factura.setPago(pago);
        factura.setNumeroFactura("FAC-WEB-" + System.currentTimeMillis());
        factura.setFechaEmision(LocalDateTime.now());
        factura.setNombreReceptor(persona.getNombres() + " " + persona.getApellidos());
        factura.setDocumentoReceptor(estudiante.getCarnet() != null ? estudiante.getCarnet() : persona.getNumeroDocumento());
        factura.setCorreoReceptor(persona.getCorreoPersonal());
        factura.setDireccionReceptor(persona.getDireccion() != null && !persona.getDireccion().isBlank()
                ? persona.getDireccion() : "Santa Ana, El Salvador");
        BigDecimal subtotalFactura = cargosALiquidar.stream().map(CargoEstudiante::getMontoBase).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuentoFactura = cargosALiquidar.stream().map(CargoEstudiante::getMontoDescuento).reduce(BigDecimal.ZERO, BigDecimal::add);
        factura.setSubtotal(subtotalFactura);
        factura.setDescuento(descuentoFactura);
        factura.setRecargo(BigDecimal.ZERO);
        factura.setTotal(totalCobrar);
        factura.setEstadoFactura(estadoFacturaRepository.findByEstadoFactura("EMITIDA")
                .orElseGet(() -> estadoFacturaRepository.findById(1L).orElseThrow()));
        facturaRepository.save(factura);

        // 8. Detonante IAM: Aprovisionamiento de credenciales institucionales
        Usuario usuarioFinal = estudiante.getUsuario();

        // Solo aprovisionamos si el estudiante es de Nuevo Ingreso y no tiene usuario
        if (usuarioFinal == null) {
            // Delegamos toda la validación, seguridad y cifrado al servicio especialista
            usuarioFinal = usuarioService.aprovisionarUsuarioEstudiante(persona, estudiante.getCarnet());

            // Vinculamos la credencial al estudiante y guardamos
            estudiante.setUsuario(usuarioFinal);
            estudianteRepository.save(estudiante);

            log.info("Credenciales IAM aprovisionadas e integradas para el carnet: {}", estudiante.getCarnet());
        }

        // 9. FORMALIZAR LA MATRÍCULA (Resolución definitiva: saveAndFlush)
        EstadoMatricula estadoMatriculada = estadoMatriculaRepository.findByEstadoMatricula("MATRICULADA")
                .or(() -> estadoMatriculaRepository.findByEstadoMatricula("INSCRITA"))
                .or(() -> estadoMatriculaRepository.findById(2L))
                .orElseThrow(() -> new IllegalStateException("No se localizó el estado final de matrícula (ID 2 / MATRICULADA)."));

        matricula.setEstadoMatricula(estadoMatriculada);
        matriculaRepository.saveAndFlush(matricula);


        List<ComprobantePagoDTO.DetalleComprobanteDTO> detalles = new ArrayList<>();

        for (CargoEstudiante cargo : cargosALiquidar) {detalles.add(
                    ComprobantePagoDTO.DetalleComprobanteDTO.builder()
                            .concepto(cargo.getConceptoCobro().getTipoCobro().getTipoCobro()
                                    + (cargo.getEstudianteBeneficio() != null
                                    && (cargo.getEstudianteBeneficio().getBeneficio().getBeneficio().toLowerCase(Locale.ROOT).contains("tercero")
                                    || cargo.getEstudianteBeneficio().getBeneficio().getBeneficio().toLowerCase(Locale.ROOT).contains("externa"))
                                    ? " - Beca de Terceros / Entidad Externa" : ""))
                            .montoBase(cargo.getMontoBase()).montoDescuento(cargo.getMontoDescuento()).montoTotal(cargo.getMontoTotal())
                            .build());
        }

        String correoDestino = persona.getCorreoPersonal();
        String nombreCompleto = persona.getNombres() + " " + persona.getApellidos();

        ComprobantePagoDTO comprobante = ComprobantePagoDTO.builder()
                .numeroFactura(factura.getNumeroFactura())
                .fechaEmision(factura.getFechaEmision())
                .nombreCompleto(nombreCompleto)
                .carnetEstudiante(estudiante.getCarnet())
                .correoEstudiante(correoDestino)
                .totalCobrado(totalCobrar)
                .subtotal(subtotalFactura).descuento(descuentoFactura)
                .cajeroResponsable("PAGO EN LÍNEA (PORTAL)")
                .detalles(detalles)
                .build();

        String numeroFactura = factura.getNumeroFactura();

        // Enviar únicamente después de confirmar la transacción en PostgreSQL.
        if (org.springframework.transaction.support.TransactionSynchronizationManager
                .isSynchronizationActive()) {

            org.springframework.transaction.support.TransactionSynchronizationManager
                    .registerSynchronization(
                            new org.springframework.transaction.support.TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                                        try {
                                            byte[] pdf = comprobantePdfService
                                                    .generarPdf(comprobante);

                                            emailService.enviarComprobantePago(
                                                    correoDestino,
                                                    nombreCompleto,
                                                    numeroFactura,
                                                    pdf
                                            );

                                            log.info(
                                                    "Factura {} enviada al correo {}",
                                                    numeroFactura,
                                                    correoDestino
                                            );
                                        } catch (Exception e) {
                                            log.error(
                                                    "No se pudo enviar la factura {}: {}",
                                                    numeroFactura,
                                                    e.getMessage(),
                                                    e
                                            );
                                        }
                                    });
                                }
                            }
                    );
        }


        return ConfirmacionMatriculaOnlineDTO.builder()
                .carnet(estudiante.getCarnet())
                .correoInstitucional(usuarioFinal.getCorreoInstitucional())
                .numeroFactura(factura.getNumeroFactura())
                .numeroAutorizacion(autorizacionPasarela)
                .montoPagado(pago.getMontoPagado())
                .fechaPago(pago.getFechaPago())
                .mensaje("Matrícula procesada exitosamente. Las credenciales de acceso institucional fueron enviadas a tu correo personal.")
                .build();
    }

    // =========================================================================
    // CASO 4: DESISTIMIENTO VOLUNTARIO (Liberación Inmediata de Cupos)
    // =========================================================================
    @Transactional(rollbackFor = Exception.class)
    public void cancelarTramiteVoluntario(Long idMatricula) {
        Matricula matricula = matriculaRepository.findById(idMatricula)
                .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada con ID: " + idMatricula));

        // Validación de regla de negocio
        if (!"PENDIENTE_PAGO".equalsIgnoreCase(matricula.getEstadoMatricula().getEstadoMatricula())) {
            throw new IllegalStateException("Solo se permite cancelar solicitudes que se encuentren en estado PENDIENTE_PAGO.");
        }

        Estudiante estudiante = matricula.getEstudiante();
        if (estudiante.getUsuario() != null) {
            throw new IllegalStateException("Operación rechazada: El estudiante ya cuenta con credenciales activas.");
        }

        log.info("Cancelación voluntaria en curso para Matrícula ID: {}. Liberando recursos...", idMatricula);

        // PASO 1: Eliminar registros dependientes de la Matrícula (Hijos)
        inscripcionRepository.deleteByMatriculaId(matricula.getIdMatricula());
        cargoEstudianteRepository.deleteByMatriculaId(matricula.getIdMatricula());
        inscripcionRepository.flush();
        cargoEstudianteRepository.flush();

        // PASO 2: Eliminar la Matrícula
        matriculaRepository.delete(matricula);
        matriculaRepository.flush();

        // PASO 3: Eliminar la trayectoria académica vinculada al estudiante
        estudianteCarreraRepository.deleteByEstudiante_IdEstudiante(estudiante.getIdEstudiante());
        estudianteCarreraRepository.flush();

        // PASO 4: Eliminar el aspirante provisional
        estudianteRepository.delete(estudiante);
        estudianteRepository.flush();

        log.info("Cancelación completada: cupos y registros provisionales liberados para la matrícula {}", idMatricula);
    }

    // =========================================================================
    // CASO 5: CLEAN-UP JOB (Depuración Programada de Registros Vencidos > 3 Días)
    // =========================================================================
    @Scheduled(cron = "0 0 2 * * ?") // 02:00 AM diario
    @Transactional(rollbackFor = Exception.class)
    public void ejecutarPurgaNocturna() {
        LocalDateTime umbral = LocalDateTime.now().minusDays(3);
        List<Estudiante> aspirantesExpirados = estudianteRepository.findEstudiantesTentativosExpirados(umbral);

        if (aspirantesExpirados.isEmpty()) {
            return;
        }

        log.info("Iniciando purga nocturna. Aspirantes expirados detectados: {}", aspirantesExpirados.size());

        for (Estudiante est : aspirantesExpirados) {
            List<Matricula> matriculas = matriculaRepository.findByEstudiante_IdEstudiante(est.getIdEstudiante());

            for (Matricula mat : matriculas) {
                inscripcionRepository.deleteByMatriculaId(mat.getIdMatricula());
                cargoEstudianteRepository.deleteByMatriculaId(mat.getIdMatricula());
                matriculaRepository.delete(mat);
            }

            estudianteCarreraRepository.deleteByEstudiante_IdEstudiante(est.getIdEstudiante());
            estudianteRepository.delete(est);

            log.warn("Registro provisional {} y sus cupos fueron eliminados por falta de pago.", est.getCarnet());
        }
    }

    // --- Helpers de Sanitización y Generación ---
    private String enmascararCorreo(String email) {
        if (email == null || !email.contains("@")) return "n/a";
        int atIndex = email.indexOf('@');
        String user = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (user.length() <= 2) return "**" + domain;
        return user.charAt(0) + "****" + user.charAt(user.length() - 1) + domain;
    }

    private String enmascararTelefono(String tel) {
        if (tel == null || tel.length() < 4) return "****";
        return "****-" + tel.substring(tel.length() - 4);
    }

//    private synchronized String generarCarnetDeterministico(String nombres, String apellidos, int anio) {
//        String inA = (apellidos != null && !apellidos.isBlank()) ? apellidos.substring(0, 1).toUpperCase() : "X";
//        String inN = (nombres != null && !nombres.isBlank()) ? nombres.substring(0, 1).toUpperCase() : "X";
//
//        // Se toma el ID más alto existente para asegurar que el correlativo siempre crezca de forma monotónica
//        long correlativo = estudianteRepository.obtenerUltimoIdEstudiante() + 1;
//        String anioCorto = String.valueOf(anio).substring(2);
//        String candidato = String.format("%s%s%s%04d", inA, inN, anioCorto, correlativo);
//
//        while (estudianteRepository.existsByCarnet(candidato)) {
//            correlativo++;
//            candidato = String.format("%s%s%s%04d", inA, inN, anioCorto, correlativo);
//        }
//        return candidato;
//    }

    // --- Generación de Carnet Institucional ---
    private synchronized String generarCarnetDeterministico(String nombres, String apellidos, int anio) {
        String inicialApellido = (apellidos != null && !apellidos.trim().isEmpty()) ? apellidos.trim().substring(0, 1).toUpperCase() : "X";
        String inicialNombre = (nombres != null && !nombres.trim().isEmpty()) ? nombres.trim().substring(0, 1).toUpperCase() : "X";
        String anioCorto = String.valueOf(anio).substring(2);

        // PRÁCTICA EMPRESARIAL: Usar el correlativo más alto, nunca count(), para soportar eliminaciones de registros.
        long correlativo = estudianteRepository.obtenerUltimoIdEstudiante() + 1;
        String numeroFormateado = String.format("%04d", correlativo);
        String candidato = inicialApellido + inicialNombre + anioCorto + numeroFormateado;

        // BUCLE DE SEGURIDAD: Prevención definitiva de colisiones en concurrencia masiva
        while (estudianteRepository.existsByCarnet(candidato)) {
            correlativo++;
            numeroFormateado = String.format("%04d", correlativo);
            candidato = inicialApellido + inicialNombre + anioCorto + numeroFormateado;
        }

        return candidato;
    }


    private void validarAccesoMatricula(
            Long idMatricula,
            String tokenVerificacion
    ) {
        if (tokenVerificacion == null ||
                tokenVerificacion.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes verificar tu identidad."
            );
        }

        Matricula matricula = matriculaRepository.findById(idMatricula)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se pudo autorizar la solicitud."
                ));

        Persona persona = matricula.getEstudiante().getPersona();

        AutorizacionAdmision autorizacion =
                autorizaciones.get(tokenVerificacion);

        boolean autorizacionExistenteValida =
                autorizacion != null
                        && LocalDateTime.now().isBefore(
                        autorizacion.expiracion()
                )
                        && Objects.equals(
                        autorizacion.idPersona(),
                        persona.getIdPersona()
                );

        boolean autorizacionNuevaValida = false;

        AutorizacionCorreoNuevo autorizacionNueva =
                autorizacionesCorreoNuevo.get(tokenVerificacion);

        if (autorizacionNueva != null) {
            autorizacionNuevaValida =
                    LocalDateTime.now().isBefore(
                            autorizacionNueva.expiracion()
                    )
                            && Objects.equals(
                            autorizacionNueva.idTipoDocumento(),
                            persona.getTipoDocumento().getIdTipoDocumento()
                    )
                            && Objects.equals(
                            autorizacionNueva.numeroDocumento(),
                            ValidadorDocumentoUtil.normalizar(
                                    persona.getNumeroDocumento()
                            )
                    )
                            && Objects.equals(
                            autorizacionNueva.correoPersonal(),
                            persona.getCorreoPersonal()
                                    .trim()
                                    .toLowerCase(Locale.ROOT)
                    );
        }

        if (!autorizacionExistenteValida &&
                !autorizacionNuevaValida) {
            throw new IllegalArgumentException(
                    "No se pudo autorizar la solicitud."
            );
        }
    }


    @Transactional(readOnly = true)
    public String generarYDespacharBoletaNpe(
            Long idMatricula,
            String tokenVerificacion) {

        validarAccesoMatricula(idMatricula, tokenVerificacion);
        Matricula matricula = matriculaRepository.findById(idMatricula)
                .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada."));

        if (!"PENDIENTE_PAGO".equalsIgnoreCase(matricula.getEstadoMatricula().getEstadoMatricula())) {
            throw new IllegalStateException("La matrícula ya no se encuentra pendiente de pago.");
        }

        Estudiante estudiante = matricula.getEstudiante();
        Persona persona = estudiante.getPersona();

        // Obtener el cargo de Matrícula (ID 1) para armar el NPE
        CargoEstudiante cargoMatricula = cargoEstudianteRepository
                .findByMatricula_IdMatriculaAndEstadoCargo_EstadoCargoOrderByNumeroCuotaAsc(idMatricula, "PENDIENTE")
                .stream()
                .filter(c -> c.getNumeroCuota() == 1 && c.getConceptoCobro().getTipoCobro().getIdTipoCobro() == 1L)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cargo de matrícula base no encontrado."));

        // Calcular el monto 1+1 (Matrícula + Cuota 1)
        BigDecimal totalExigible = cargoEstudianteRepository
                .findByMatricula_IdMatriculaAndEstadoCargo_EstadoCargoOrderByNumeroCuotaAsc(idMatricula, "PENDIENTE")
                .stream()
                .filter(c -> c.getNumeroCuota() == 1)
                .map(CargoEstudiante::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String npeGenerado = String.format("0981%08d%06d%04d",
                estudiante.getIdEstudiante(),
                cargoMatricula.getIdCargoEstudiante(),
                matricula.getCiclo().getAnio());

        final String correoDestino = persona.getCorreoPersonal();
        final String nombreCompleto = persona.getNombres() + " " + persona.getApellidos();
        final String npeFinal = npeGenerado;
        final BigDecimal montoFinal = totalExigible;
        final LocalDate fechaLimite = cargoMatricula.getFechaVencimiento();

        CompletableFuture.runAsync(() -> {
            try {
                emailService.enviarBoletaPreMatriculaNpe(correoDestino, nombreCompleto, npeFinal, montoFinal, fechaLimite);
                log.info("Boleta NPE despachada con éxito a: {}", correoDestino);
            } catch (Exception e) {
                log.error("Fallo al enviar correo NPE a {}: {}", correoDestino, e.getMessage());
            }
        });

        return npeGenerado;
    }

    public void solicitarCodigoAdmision(
            Long idTipoDocumento,
            String numeroDocumento) {

        String documento = ValidadorDocumentoUtil.normalizar(numeroDocumento);
        ValidadorDocumentoUtil.validar(idTipoDocumento, documento);

        Persona persona = personaRepository
                .findByNumeroDocumento(documento)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se pudo procesar la solicitud."
                ));

        if (!Objects.equals(
                persona.getTipoDocumento().getIdTipoDocumento(),
                idTipoDocumento)) {
            throw new IllegalArgumentException(
                    "No se pudo procesar la solicitud."
            );
        }

        validarPersonaAdmisible(persona);

        String correo = persona.getCorreoPersonal();

        if (correo == null || correo.isBlank()) {
            throw new IllegalStateException(
                    "No existe un correo registrado para verificar."
            );
        }

        Long idPersona = persona.getIdPersona();
        LocalDateTime ahora = LocalDateTime.now();

        CodigoAdmision anterior = codigosAdmision.get(idPersona);

        if (anterior != null &&
                ahora.isBefore(anterior.ultimoEnvio().plusSeconds(60))) {

            throw new IllegalStateException(
                    "Debes esperar 60 segundos antes de solicitar otro código."
            );
        }

        String codigo = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        CodigoAdmision nuevoRegistro = new CodigoAdmision(
                passwordEncoder.encode(codigo),
                ahora.plusMinutes(10),
                0,
                ahora
        );

        codigosAdmision.put(idPersona, nuevoRegistro);

        try {
            emailService.enviarCodigoVerificacionAdmision(
                    correo,
                    persona.getNombres(),
                    codigo
            );

            log.info(
                    "Código OTP de admisión enviado para persona ID {}",
                    idPersona
            );

        } catch (RuntimeException ex) {

            codigosAdmision.remove(idPersona, nuevoRegistro);

            log.error(
                    "Error enviando OTP para persona ID {}",
                    idPersona,
                    ex
            );

            throw new IllegalStateException(
                    "No se pudo enviar el código de verificación."
            );
        }
    }

    public String verificarCodigoAdmision(
            Long idTipoDocumento,
            String numeroDocumento,
            String codigo) {

        String documento = ValidadorDocumentoUtil.normalizar(numeroDocumento);
        ValidadorDocumentoUtil.validar(idTipoDocumento, documento);

        if (codigo == null || !codigo.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                    "El código debe contener seis dígitos."
            );
        }

        Persona persona = personaRepository
                .findByNumeroDocumento(documento)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Verificación inválida."
                ));

        if (!Objects.equals(
                persona.getTipoDocumento().getIdTipoDocumento(),
                idTipoDocumento)) {
            throw new IllegalArgumentException(
                    "Verificación inválida."
            );
        }

        validarPersonaAdmisible(persona);

        Long idPersona = persona.getIdPersona();
        LocalDateTime ahora = LocalDateTime.now();

        // Sincroniza la verificación para evitar que dos solicitudes
        // utilicen simultáneamente el mismo código.
        synchronized (codigosAdmision) {

            CodigoAdmision registro = codigosAdmision.get(idPersona);

            if (registro == null) {
                throw new IllegalArgumentException(
                        "No existe un código de verificación vigente."
                );
            }

            if (!ahora.isBefore(registro.expiracion())) {
                codigosAdmision.remove(idPersona);

                throw new IllegalArgumentException(
                        "El código ha expirado."
                );
            }

            if (registro.intentos() >= 5) {
                codigosAdmision.remove(idPersona);

                throw new IllegalArgumentException(
                        "Se superó el número de intentos permitidos."
                );
            }

            if (!passwordEncoder.matches(codigo, registro.hash())) {

                int intentos = registro.intentos() + 1;

                if (intentos >= 5) {
                    codigosAdmision.remove(idPersona);

                    throw new IllegalArgumentException(
                            "Se superó el número de intentos permitidos."
                    );
                }

                codigosAdmision.put(
                        idPersona,
                        new CodigoAdmision(
                                registro.hash(),
                                registro.expiracion(),
                                intentos,
                                registro.ultimoEnvio()
                        )
                );

                throw new IllegalArgumentException(
                        "Código incorrecto."
                );
            }

            // Código de un solo uso.
            codigosAdmision.remove(idPersona);
        }

        String token = UUID.randomUUID().toString();

        autorizaciones.put(
                token,
                new AutorizacionAdmision(
                        idPersona,
                        ahora.plusMinutes(30)
                )
        );

        log.info(
                "Identidad verificada mediante OTP para persona ID {}",
                idPersona
        );

        return token;
    }


    private void validarPersonaAdmisible(Persona persona) {

        if (usuarioRepository.existsByPersona_IdPersonaAndRol_Rol(
                persona.getIdPersona(),
                "ADMINISTRADOR"
        )) {
            throw new IllegalStateException(
                    "Esta cuenta no puede realizar trámites públicos de admisión."
            );
        }

        Optional<Estudiante> estudianteOpt =
                estudianteRepository.findByPersona_IdPersona(
                        persona.getIdPersona()
                );

        if (estudianteOpt.isPresent()) {
            Estudiante estudiante = estudianteOpt.get();

            if (estudiante.getUsuario() != null) {
                throw new IllegalStateException(
                        "Ya posees un usuario activo. Inicia sesión en el portal estudiantil."
                );
            }
        }
    }


    private void validarSinMatriculaEnCiclo(
            Estudiante estudiante,
            Ciclo ciclo
    ) {
        boolean existeMatricula =
                matriculaRepository
                        .existsByEstudiante_IdEstudianteAndCiclo_IdCiclo(
                                estudiante.getIdEstudiante(),
                                ciclo.getIdCiclo()
                        );

        if (existeMatricula) {
            throw new IllegalStateException(
                    "Ya existe una matrícula o prematrícula para el ciclo "
                            + ciclo.getCodigoCiclo()
                            + ". Debes recuperar el trámite existente."
            );
        }
    }



    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDatosVerificados(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Token de verificación requerido."
            );
        }

        AutorizacionAdmision autorizacion = autorizaciones.get(token);

        if (autorizacion == null ||
                !LocalDateTime.now().isBefore(autorizacion.expiracion())) {

            autorizaciones.remove(token);

            throw new IllegalArgumentException(
                    "La verificación ha expirado o no es válida."
            );
        }

        Persona persona = personaRepository
                .findById(autorizacion.idPersona())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La persona verificada ya no existe."
                ));

        validarPersonaAdmisible(persona);

        Map<String, Object> datos = new HashMap<>();

        datos.put("idPersona", persona.getIdPersona());
        datos.put("nombres", persona.getNombres());
        datos.put("apellidos", persona.getApellidos());
        datos.put("fechaNacimiento", persona.getFechaNacimiento());
        datos.put("sexo", persona.getSexo());
        datos.put("telefono", persona.getTelefono());
        datos.put("correoPersonal", persona.getCorreoPersonal());
        datos.put("direccion", persona.getDireccion());

        if (persona.getDistrito() != null) {
            datos.put(
                    "idDistrito",
                    persona.getDistrito().getIdDistrito()
            );

            // Ajusta este getter si tu entidad Distrito utiliza
            // otro nombre para la relación con Departamento.
            if (persona.getDistrito().getDepartamento() != null) {
                datos.put(
                        "idDepartamento",
                        persona.getDistrito()
                                .getDepartamento()
                                .getIdDepartamento()
                );
            }
        }

        return datos;
    }

    private void validarAutorizacionCorreoNuevo(
            String token,
            Long idTipoDocumento,
            String numeroDocumento,
            String correoPersonal
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes verificar tu correo electrónico antes de continuar."
            );
        }

        AutorizacionCorreoNuevo autorizacion =
                autorizacionesCorreoNuevo.get(token);

        if (autorizacion == null ||
                !LocalDateTime.now().isBefore(autorizacion.expiracion()) ||
                !Objects.equals(
                        autorizacion.idTipoDocumento(),
                        idTipoDocumento
                ) ||
                !Objects.equals(
                        autorizacion.numeroDocumento(),
                        numeroDocumento
                ) ||
                !Objects.equals(
                        autorizacion.correoPersonal(),
                        correoPersonal
                )) {

            throw new IllegalArgumentException(
                    "La verificación del correo no es válida o ha expirado."
            );
        }
    }

    private record CodigoAdmision(
            String hash,
            LocalDateTime expiracion,
            int intentos,
            LocalDateTime ultimoEnvio
    ) {}

    private record AutorizacionAdmision(
            Long idPersona,
            LocalDateTime expiracion
    ) {}

    private record CodigoCorreoNuevo(
            String hash,
            LocalDateTime expiracion,
            int intentos,
            LocalDateTime ultimoEnvio
    ) {}
    private record AutorizacionCorreoNuevo(
            Long idTipoDocumento,
            String numeroDocumento,
            String correoPersonal,
            LocalDateTime expiracion
    ) {}


    private final ConcurrentMap<String, AutorizacionCorreoNuevo> autorizacionesCorreoNuevo =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<String, CodigoCorreoNuevo> codigosCorreoNuevo =
            new ConcurrentHashMap<>();

    private String claveCorreoNuevo(
            Long idTipoDocumento,
            String numeroDocumento,
            String correoPersonal
    ) {
        return idTipoDocumento + ":" +
                numeroDocumento + ":" +
                correoPersonal;
    }


    public void solicitarCodigoCorreoNuevo(
            Long idTipoDocumento,
            String numeroDocumento,
            String correoPersonal,
            String nombres
    ) {
        String documento = ValidadorDocumentoUtil.normalizar(numeroDocumento);
        ValidadorDocumentoUtil.validar(idTipoDocumento, documento);

        if (correoPersonal == null || correoPersonal.isBlank()) {
            throw new IllegalArgumentException("Ingresa un correo válido.");
        }

        String correo = correoPersonal.trim().toLowerCase(Locale.ROOT);

        if (!correo.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$"
        )) {
            throw new IllegalArgumentException("El formato del correo es inválido.");
        }

        String clave = claveCorreoNuevo(idTipoDocumento, documento, correo);
        LocalDateTime ahora = LocalDateTime.now();

        synchronized (codigosCorreoNuevo) {
            CodigoCorreoNuevo anterior = codigosCorreoNuevo.get(clave);

            if (anterior != null &&
                    ahora.isBefore(anterior.ultimoEnvio().plusSeconds(60))) {
                throw new IllegalStateException(
                        "Debes esperar 60 segundos antes de solicitar otro código."
                );
            }

            // Nunca utilizar este flujo para recuperar un expediente existente.
            if (personaRepository.findByNumeroDocumento(documento).isPresent()) {
                throw new IllegalArgumentException(
                        "No se pudo procesar la solicitud."
                );
            }

            // Respuesta genérica: no revelar si el correo pertenece a otra persona.
            if (personaRepository.findByCorreoPersonal(correo).isPresent()) {
                return;
            }

            String codigo = String.format(
                    "%06d",
                    secureRandom.nextInt(1_000_000)
            );

            CodigoCorreoNuevo registro = new CodigoCorreoNuevo(
                    passwordEncoder.encode(codigo),
                    ahora.plusMinutes(10),
                    0,
                    ahora
            );

            codigosCorreoNuevo.put(clave, registro);

            try {
                emailService.enviarCodigoVerificacionAdmision(
                        correo,
                        nombres == null || nombres.isBlank()
                                ? "Aspirante"
                                : nombres.trim(),
                        codigo
                );
            } catch (RuntimeException ex) {
                codigosCorreoNuevo.remove(clave, registro);
                throw new IllegalStateException(
                        "No se pudo enviar el código de verificación."
                );
            }
        }
    }


    public String verificarCodigoCorreoNuevo(
            Long idTipoDocumento,
            String numeroDocumento,
            String correoPersonal,
            String codigo
    ) {
        String documento = ValidadorDocumentoUtil.normalizar(numeroDocumento);
        ValidadorDocumentoUtil.validar(idTipoDocumento, documento);

        if (correoPersonal == null || correoPersonal.isBlank()
                || codigo == null || !codigo.matches("\\d{6}")) {
            throw new IllegalArgumentException("Verificación inválida.");
        }

        String correo = correoPersonal.trim().toLowerCase(Locale.ROOT);
        String clave = claveCorreoNuevo(idTipoDocumento, documento, correo);

        synchronized (codigosCorreoNuevo) {
            CodigoCorreoNuevo registro = codigosCorreoNuevo.get(clave);
            LocalDateTime ahora = LocalDateTime.now();

            if (registro == null || !ahora.isBefore(registro.expiracion())) {
                codigosCorreoNuevo.remove(clave);
                throw new IllegalArgumentException(
                        "El código no existe o ha expirado."
                );
            }

            if (registro.intentos() >= 5) {
                codigosCorreoNuevo.remove(clave);
                throw new IllegalArgumentException(
                        "Se superó el número de intentos permitidos."
                );
            }

            if (!passwordEncoder.matches(codigo, registro.hash())) {
                int intentos = registro.intentos() + 1;

                if (intentos >= 5) {
                    codigosCorreoNuevo.remove(clave);
                } else {
                    codigosCorreoNuevo.put(
                            clave,
                            new CodigoCorreoNuevo(
                                    registro.hash(),
                                    registro.expiracion(),
                                    intentos,
                                    registro.ultimoEnvio()
                            )
                    );
                }

                throw new IllegalArgumentException(
                        "Código incorrecto o verificación inválida."
                );
            }

            if (personaRepository.findByNumeroDocumento(documento).isPresent()
                    || personaRepository.findByCorreoPersonal(correo).isPresent()) {
                codigosCorreoNuevo.remove(clave);
                throw new IllegalArgumentException(
                        "No es posible continuar con los datos proporcionados."
                );
            }

            codigosCorreoNuevo.remove(clave);

            String token = UUID.randomUUID().toString();

            autorizacionesCorreoNuevo.put(
                    token,
                    new AutorizacionCorreoNuevo(
                            idTipoDocumento,
                            documento,
                            correo,
                            ahora.plusMinutes(30)
                    )
            );

            return token;
        }
    }


    @Transactional(readOnly = true)
    public Map<String, Object> consultarEstadoSolicitud(
            Long idMatricula,
            String tokenVerificacion) {

        validarAccesoMatricula(idMatricula, tokenVerificacion);

        Matricula matricula = matriculaRepository.findById(idMatricula)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la solicitud."));

        String estado = matricula.getEstadoMatricula().getEstadoMatricula();

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("idMatricula", matricula.getIdMatricula());
        respuesta.put("estado", estado);

        if ("PENDIENTE_PAGO".equalsIgnoreCase(estado)) {

            List<CargoEstudiante> cargos = cargoEstudianteRepository
                    .findByMatricula_IdMatriculaAndEstadoCargo_EstadoCargoOrderByNumeroCuotaAsc(
                            idMatricula, "PENDIENTE");

            CargoEstudiante cargoMatricula = cargos.stream()
                    .filter(c -> c.getNumeroCuota() == 1
                            && Objects.equals(
                            c.getConceptoCobro()
                                    .getTipoCobro()
                                    .getIdTipoCobro(),
                            ID_TIPO_COBRO_MATRICULA))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró el cargo de matrícula."));

            BigDecimal totalInicial = cargos.stream()
                    .filter(c -> c.getNumeroCuota() == 1)
                    .map(CargoEstudiante::getMontoTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String npe = String.format(
                    "0981%08d%06d%04d",
                    matricula.getEstudiante().getIdEstudiante(),
                    cargoMatricula.getIdCargoEstudiante(),
                    matricula.getCiclo().getAnio()
            );

            respuesta.put("npe", npe);
            respuesta.put("totalAPagar", totalInicial);
            respuesta.put(
                    "fechaVencimiento",
                    cargoMatricula.getFechaVencimiento()
            );
        }

        if ("MATRICULADA".equalsIgnoreCase(estado)) {
            Estudiante estudiante = matricula.getEstudiante();

            respuesta.put("carnet", estudiante.getCarnet());

            if (estudiante.getUsuario() != null) {
                respuesta.put(
                        "correoInstitucional",
                        estudiante.getUsuario().getCorreoInstitucional()
                );
            }
        }

        return respuesta;
    }




}
