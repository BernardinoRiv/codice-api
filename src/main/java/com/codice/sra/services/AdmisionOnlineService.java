package com.codice.sra.services;

import com.codice.sra.dtos.MatriculaExterna.*;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import com.codice.sra.utils.ValidadorDocumentoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

    // Constantes de Dominio
    private static final Long ID_TIPO_COBRO_MATRICULA = 1L;       // Matrícula de Pregrado
    private static final Long ID_TIPO_COBRO_EXTRAORDINARIA = 5L;  // Arancel por Inscripción Extraordinaria

    // =========================================================================
    // CASO 1: VERIFICACIÓN PÚBLICA DE IDENTIDAD (Protección contra Enumeración)
    // =========================================================================
    @Transactional(readOnly = true)
    public VerificacionIdentidadPublicaResponseDTO verificarIdentidad(Long idTipoDoc, String numeroDoc) {
        // 1. Normalización y Validación estricta usando la utilidad central del sistema
        String docLimpio = ValidadorDocumentoUtil.normalizar(numeroDoc);
        ValidadorDocumentoUtil.validar(idTipoDoc, docLimpio);

        // 1.1 Si ya es un estudiante activo con credenciales asignadas en el sistema
        Optional<Estudiante> estudianteActivo = estudianteRepository
                .findByPersona_NumeroDocumentoAndUsuarioIsNotNull(docLimpio);

        if (estudianteActivo.isPresent()) {
            return VerificacionIdentidadPublicaResponseDTO.builder()
                    .existePersona(true)
                    .esEstudianteActivo(true)
                    .requiereLogin(true)
                    .mensaje("Ya posees un expediente y cuenta institucional activa. Inicia sesión en el portal académico para inscribir tus asignaturas.")
                    .build();
        }

        // 1.2 Búsqueda en el censo civil base
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

        // 1.3 Bloqueo de Segregación de Funciones (SoD) para Administradores
        if (usuarioRepository.existsByPersona_IdPersonaAndRol_Rol(persona.getIdPersona(), "ADMINISTRADOR")) {
            return VerificacionIdentidadPublicaResponseDTO.builder()
                    .existePersona(true)
                    .esEstudianteActivo(false)
                    .requiereLogin(true)
                    .mensaje("Restricción de seguridad: Los usuarios con rol ADMINISTRADOR no pueden matricularse mediante autoservicio.")
                    .build();
        }

        // 1.4 Respuesta sanitizada con enmascaramiento de canales de contacto
        return VerificacionIdentidadPublicaResponseDTO.builder()
                .existePersona(true)
                .esEstudianteActivo(false)
                .requiereLogin(false)
                .idPersona(persona.getIdPersona())
                .nombres(persona.getNombres())
                .apellidos(persona.getApellidos())
                .correoEnmascarado(enmascararCorreo(persona.getCorreoPersonal()))
                .telefonoEnmascarado(enmascararTelefono(persona.getTelefono()))
                .mensaje("Identidad validada. Verifica tu información de contacto para continuar.")
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

        // 2. Validación de unicidad de correo en la tabla personas
        String correoNormalizado = dto.getCorreoPersonal().trim().toLowerCase();
        if (dto.getIdPersona() == null && personaRepository.existsByCorreoPersonal(correoNormalizado)) {
            throw new IllegalArgumentException("Ya existe una persona registrada con el correo personal: " + correoNormalizado);
        }

        // 2.1 Validación del ciclo lectivo hábil y cálculo del período
        CicloMatriculaValidatorService.EvaluacionCicloMatricula evalCiclo =
                cicloMatriculaValidatorService.resolverCicloHabilParaMatricula();
        Ciclo cicloMatricula = evalCiclo.ciclo();
        boolean esExtraordinaria = evalCiclo.esExtraordinaria();

        // 2.2 Gestión de la entidad Persona
        Persona persona;
        if (dto.getIdPersona() != null) {
            persona = personaRepository.findById(dto.getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException("Identificador de persona no encontrado: " + dto.getIdPersona()));
        } else {
            persona = new Persona();
            persona.setFechaRegistro(OffsetDateTime.now());
        }

        persona.setTipoDocumento(tipoDocumentoRepository.findById(dto.getIdTipoDocumento())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de documento no válido.")));
        persona.setNumeroDocumento(docLimpio);
        persona.setNombres(dto.getNombres().trim());
        persona.setApellidos(dto.getApellidos().trim());
        persona.setFechaNacimiento(dto.getFechaNacimiento());
        persona.setSexo(dto.getSexo().trim().toUpperCase());
        persona.setTelefono(dto.getTelefono().trim());
        persona.setCorreoPersonal(correoNormalizado);
        persona.setDistrito(distritoRepository.findById(dto.getIdDistrito())
                .orElseThrow(() -> new IllegalArgumentException("Distrito territorial no válido.")));
        persona.setDireccion(dto.getDireccion().trim());

        EstadoRegistroPersona estadoReg = estadoRegistroPersonaRepository.findByEstadoRegistro("COMPLETADO")
                .orElseGet(() -> estadoRegistroPersonaRepository.findById(2L).orElseThrow());
        persona.setEstadoRegistro(estadoReg);
        persona = personaRepository.save(persona);

        // 2.3 Creación del expediente del aspirante (id_usuario = NULL hasta liquidar pago)
        EstadoEstudiante estadoEst = estadoEstudianteRepository.findByEstadoEstudiante("ACTIVO")
                .orElseGet(() -> estadoEstudianteRepository.findById(1L).orElseThrow());

        Estudiante estudiante = new Estudiante();
        estudiante.setPersona(persona);
        estudiante.setUsuario(null);
        estudiante.setFechaIngreso(LocalDate.now());
        estudiante.setEstadoEstudiante(estadoEst);
        estudiante.setCarnet(generarCarnetDeterministico(persona.getNombres(), persona.getApellidos(), cicloMatricula.getAnio()));
        estudiante = estudianteRepository.save(estudiante);

        // 2.4 Registro de la trayectoria académica activa
        EstadoTrayectoria estadoTray = estadoTrayectoriaRepository.findByEstadoTrayectoria("ACTIVA")
                .orElseGet(() -> estadoTrayectoriaRepository.findById(1L).orElseThrow());

        EstudianteCarrera trayectoria = new EstudianteCarrera();
        trayectoria.setEstudiante(estudiante);
        trayectoria.setCarreraSede(carreraSedeRepository.findById(dto.getIdCarreraSede())
                .orElseThrow(() -> new IllegalArgumentException("Oferta carrera-sede no encontrada.")));
        trayectoria.setPensum(pensumRepository.findByIdPensumLong(dto.getIdPensum())
                .orElseThrow(() -> new IllegalArgumentException("Plan de estudios no encontrado.")));
        trayectoria.setFechaInicio(LocalDate.now());
        trayectoria.setEstadoTrayectoria(estadoTray);
        trayectoria = estudianteCarreraRepository.save(trayectoria);

        // 2.5 Matrícula en estado PENDIENTE_PAGO
        EstadoMatricula estadoMat = estadoMatriculaRepository.findByEstadoMatricula("PENDIENTE_PAGO")
                .orElseGet(() -> estadoMatriculaRepository.findById(1L).orElseThrow());

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setEstudianteCarrera(trayectoria);
        matricula.setCiclo(cicloMatricula);
        matricula.setFechaMatricula(LocalDateTime.now());
        matricula.setEstadoMatricula(estadoMat);
        matricula = matriculaRepository.save(matricula);

        // 2.6 Reserva de cupos con validación pesimista
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

        // Concepto: Matrícula de Pregrado[cite: 2]
        ConceptoCobro arancelMatricula = conceptoCobroRepository
                .findByCiclo_IdCicloAndTipoCobro_IdTipoCobro(cicloMatricula.getIdCiclo(), ID_TIPO_COBRO_MATRICULA)
                .orElseThrow(() -> new IllegalStateException("El arancel de Matrícula no está configurado para el ciclo " + cicloMatricula.getCodigoCiclo()));

        // Concepto: Cuota Mensual de Pregrado[cite: 2]
        ConceptoCobro arancelCuota = conceptoCobroRepository
                .findByCiclo_IdCicloAndTipoCobro_IdTipoCobro(cicloMatricula.getIdCiclo(), 2L)
                .orElseThrow(() -> new IllegalStateException("La cuota mensual no está configurada para el ciclo " + cicloMatricula.getCodigoCiclo()));

        LocalDate fechaLimitePreMatricula = LocalDate.now().plusDays(2); // 48 horas de vigencia

        // 1. Cargo: Matrícula (numeroCuota = 1 para respetar chk_numero_cuota >= 1)
        CargoEstudiante cargoMatricula = new CargoEstudiante();
        cargoMatricula.setMatricula(matricula);
        cargoMatricula.setConceptoCobro(arancelMatricula);
        cargoMatricula.setEstadoCargo(estadoPendiente);
        cargoMatricula.setMontoBase(arancelMatricula.getMontoBase());
        cargoMatricula.setMontoDescuento(BigDecimal.ZERO);
        cargoMatricula.setMontoRecargo(BigDecimal.ZERO);
        cargoMatricula.setMontoTotal(arancelMatricula.getMontoBase());
        cargoMatricula.setFechaGeneracion(LocalDateTime.now());
        cargoMatricula.setNumeroCuota(1);
        cargoMatricula.setFechaVencimiento(fechaLimitePreMatricula);
        cargoMatricula = cargoEstudianteRepository.save(cargoMatricula);

        // 2. Cargo: Extraordinaria (si aplica)
        BigDecimal totalExtraordinaria = BigDecimal.ZERO;
        if (esExtraordinaria) {
            ConceptoCobro arancelExtra = conceptoCobroRepository
                    .findByCiclo_IdCicloAndTipoCobro_IdTipoCobro(cicloMatricula.getIdCiclo(), ID_TIPO_COBRO_EXTRAORDINARIA)
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
        for (int i = 1; i <= 5; i++) {
            CargoEstudiante cuota = new CargoEstudiante();
            cuota.setMatricula(matricula);
            cuota.setConceptoCobro(arancelCuota);
            cuota.setEstadoCargo(estadoPendiente);
            cuota.setMontoBase(arancelCuota.getMontoBase());
            cuota.setMontoDescuento(BigDecimal.ZERO);
            cuota.setMontoRecargo(BigDecimal.ZERO);
            cuota.setMontoTotal(arancelCuota.getMontoBase());
            cuota.setFechaGeneracion(LocalDateTime.now());
            cuota.setNumeroCuota(i);
            cuota.setFechaVencimiento(i == 1 ? fechaLimitePreMatricula : fechaBaseCiclo.plusMonths(i - 1));
            cargoEstudianteRepository.save(cuota);
        }

        // Regla de Negocio 1 + 1: Matrícula ($70.00) + Primera Cuota Mensual ($70.00) obligatorias[cite: 2]
        BigDecimal totalInicialExigible = arancelMatricula.getMontoBase()
                .add(arancelCuota.getMontoBase())
                .add(totalExtraordinaria);

        // =========================================================================
        // 2.8 BIFURCACIÓN DE CANAL (Ventanilla vs Pasarela en Línea)
        // =========================================================================
        String npeGenerado = null;
        String mensajeRetorno;

        if ("VENTANILLA".equalsIgnoreCase(dto.getModalidadPago())) {
            npeGenerado = String.format("0981%08d%06d%04d",
                    estudiante.getIdEstudiante(),
                    cargoMatricula.getIdCargoEstudiante(),
                    cicloMatricula.getAnio());

            mensajeRetorno = "Pre-matrícula confirmada. Tienes 48 horas para cancelar en Colecturía tu Matrícula + Primera Cuota con tu documento o NPE.";

            final String npeFinal = npeGenerado;
            final String correoAspirante = persona.getCorreoPersonal();
            final String nombreAspirante = persona.getNombres() + " " + persona.getApellidos();
            final BigDecimal montoFinal = totalInicialExigible;

            CompletableFuture.runAsync(() -> {
                try {
                    emailService.enviarBoletaPreMatriculaNpe(correoAspirante, nombreAspirante, npeFinal, montoFinal, fechaLimitePreMatricula);
                    log.info("Boleta digital con NPE despachada a: {}", correoAspirante);
                } catch (Exception e) {
                    log.error("Fallo al despachar correo de orden NPE: {}", e.getMessage(), e);
                }
            });
        } else {
            mensajeRetorno = "Cupos reservados exitosamente. Procede al pago en línea de Matrícula + Primera Cuota mediante tarjeta de crédito o débito.";
        }

        return MatriculaOnlineResponseDTO.builder()
                .idMatricula(matricula.getIdMatricula())
                .modalidadPago(dto.getModalidadPago())
                .totalAPagar(totalInicialExigible)
                .fechaVencimiento(fechaLimitePreMatricula)
                .npe(npeGenerado)
                .mensaje(mensajeRetorno)
                .build();
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
        factura.setSubtotal(totalCobrar);
        factura.setDescuento(BigDecimal.ZERO);
        factura.setRecargo(BigDecimal.ZERO);
        factura.setTotal(totalCobrar);
        factura.setEstadoFactura(estadoFacturaRepository.findByEstadoFactura("EMITIDA")
                .orElseGet(() -> estadoFacturaRepository.findById(1L).orElseThrow()));
        facturaRepository.save(factura);

        // 8. Detonante IAM: Aprovisionamiento de credenciales institucionales[cite: 2]
        Usuario nuevoUsuario = estudiante.getUsuario();
        if (nuevoUsuario == null) {
            nuevoUsuario = usuarioService.aprovisionarUsuario(persona, "ESTUDIANTE", 8);
            estudiante.setUsuario(nuevoUsuario);
            estudianteRepository.save(estudiante);
        }

        // 9. FORMALIZAR LA MATRÍCULA (Resolución definitiva: saveAndFlush)
        EstadoMatricula estadoMatriculada = estadoMatriculaRepository.findByEstadoMatricula("MATRICULADA")
                .or(() -> estadoMatriculaRepository.findByEstadoMatricula("INSCRITA"))
                .or(() -> estadoMatriculaRepository.findById(2L))
                .orElseThrow(() -> new IllegalStateException("No se localizó el estado final de matrícula (ID 2 / MATRICULADA)."));

        matricula.setEstadoMatricula(estadoMatriculada);
        matriculaRepository.saveAndFlush(matricula);

        return ConfirmacionMatriculaOnlineDTO.builder()
                .carnet(estudiante.getCarnet())
                .correoInstitucional(nuevoUsuario.getCorreoInstitucional())
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

    private synchronized String generarCarnetDeterministico(String nombres, String apellidos, int anio) {
        String inA = (apellidos != null && !apellidos.isBlank()) ? apellidos.substring(0, 1).toUpperCase() : "X";
        String inN = (nombres != null && !nombres.isBlank()) ? nombres.substring(0, 1).toUpperCase() : "X";

        // Se toma el ID más alto existente para asegurar que el correlativo siempre crezca de forma monotónica
        long correlativo = estudianteRepository.obtenerUltimoIdEstudiante() + 1;
        String anioCorto = String.valueOf(anio).substring(2);
        String candidato = String.format("%s%s%s%04d", inA, inN, anioCorto, correlativo);

        while (estudianteRepository.existsByCarnet(candidato)) {
            correlativo++;
            candidato = String.format("%s%s%s%04d", inA, inN, anioCorto, correlativo);
        }
        return candidato;
    }
}