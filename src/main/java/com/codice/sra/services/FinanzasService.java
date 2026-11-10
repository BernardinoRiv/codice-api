package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanzasService {

    private final CicloRepository cicloRepository;
    private final EstudianteRepository estudianteRepository;
    private final ConceptoCobroRepository conceptoCobroRepository;
    private final CargoEstudianteRepository cargoEstudianteRepository;
    private final EstudianteBeneficioRepository estudianteBeneficioRepository;
    private final EstadoCargoRepository estadoCargoRepository;
    private final MatriculaRepository matriculaRepository;
    private final PagoRepository pagoRepository;
    private final PagoCargoRepository pagoCargoRepository;
    private final FacturaRepository facturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final PuntoRecaudoRepository puntoRecaudoRepository;
    private final ComprobantePdfService comprobantePdfService;
    private final EmailService emailService;
    private final EstudianteCarreraRepository estudianteCarreraRepository;
    private final TipoCobroRepository tipoCobroRepository;

    private final EstadoEstudianteRepository estadoEstudianteRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;

    private final EstadoMatriculaRepository estadoMatriculaRepository;

    // Repositorios requeridos para el retiro automático de materias
    private final InscripcionRepository inscripcionRepository;
    private final EstadoMateriaEstudianteRepository estadoMateriaEstudianteRepository;

    @Transactional(rollbackFor = Exception.class)
    public int clonarArancelesCicloAnterior(Long idCicloAnterior, Long idCicloNuevo) {
        Ciclo cicloNuevo = cicloRepository.findById(idCicloNuevo)
                .orElseThrow(() -> new RuntimeException("El ciclo destino no existe."));

        List<ConceptoCobro> arancelesViejos = conceptoCobroRepository.findByCiclo_IdCiclo(idCicloAnterior);

        if (arancelesViejos.isEmpty()) {
            throw new RuntimeException("El ciclo anterior no tiene aranceles registrados para clonar.");
        }

        int clonados = 0;
        for (ConceptoCobro viejo : arancelesViejos) {
            ConceptoCobro nuevo = new ConceptoCobro();
            nuevo.setCiclo(cicloNuevo);
            nuevo.setTipoCobro(viejo.getTipoCobro());
            nuevo.setMontoBase(viejo.getMontoBase());

            conceptoCobroRepository.save(nuevo);
            clonados++;
        }

        return clonados;
    }

    @Transactional(rollbackFor = Exception.class)
    public int generarCobrosMatriculaAperturaCiclo(GenerarCobrosMasivosRequestDTO request) {

        Ciclo ciclo = cicloRepository.findById(request.getIdCiclo())
                .orElseThrow(() -> new RuntimeException("El ciclo especificado no existe."));

        EstadoCargo estadoPendiente = estadoCargoRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("El estado PENDIENTE no existe."));

        List<Estudiante> estudiantesProcesar = estudianteRepository
                .findByEstadoEstudiante_EstadoEstudianteIn(List.of("ACTIVO", "EGRESADO"));

        int cobrosGenerados = 0;
        int mesInicio = (ciclo.getNumeroCiclo() != null && ciclo.getNumeroCiclo() == 2) ? 7 : 1;
        int anioCiclo = ciclo.getAnio() != null ? ciclo.getAnio() : LocalDate.now().getYear();

        for (Estudiante estudiante : estudiantesProcesar) {
            Optional<Matricula> matriculaOpt = matriculaRepository.findByEstudianteIdEstudianteAndCicloIdCiclo(
                    estudiante.getIdEstudiante(), request.getIdCiclo());

            if (matriculaOpt.isPresent()) {
                Matricula matricula = matriculaOpt.get();
                String estadoActual = estudiante.getEstadoEstudiante().getEstadoEstudiante().toUpperCase();

                String nombreCarrera = estudianteCarreraRepository
                        .findNombreCarreraByEstudianteId(estudiante.getIdEstudiante())
                        .orElse("").toUpperCase();

                Long idConceptoMatriculaAplicar;
                Long idConceptoCuotaAplicar;

                if (estadoActual.equals("EGRESADO")) {
                    idConceptoMatriculaAplicar = request.getIdMatriculaTesis();
                    idConceptoCuotaAplicar = request.getIdCuotaTesis();
                } else if (nombreCarrera.contains("MAESTRÍA") || nombreCarrera.contains("MAESTRIA")) {
                    idConceptoMatriculaAplicar = request.getIdMatriculaMaestria();
                    idConceptoCuotaAplicar = request.getIdCuotaMaestria();
                } else {
                    idConceptoMatriculaAplicar = request.getIdMatriculaCarrera();
                    idConceptoCuotaAplicar = request.getIdCuotaCarrera();
                }

                if (idConceptoMatriculaAplicar == null || idConceptoCuotaAplicar == null) {
                    continue;
                }

                ConceptoCobro conceptoMatricula = conceptoCobroRepository.findById(idConceptoMatriculaAplicar)
                        .orElseThrow(() -> new RuntimeException("Arancel de matrícula no encontrado. ID: " + idConceptoMatriculaAplicar));
                ConceptoCobro conceptoMensualidad = conceptoCobroRepository.findById(idConceptoCuotaAplicar)
                        .orElseThrow(() -> new RuntimeException("Arancel de mensualidad no encontrado. ID: " + idConceptoCuotaAplicar));

                boolean existeMatricula = cargoEstudianteRepository.existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobroAndNumeroCuota(
                        matricula.getIdMatricula(), conceptoMatricula.getIdConceptoCobro(), 1);

                if (!existeMatricula) {
                    LocalDate vencimientoMatricula = (ciclo.getFechaInicio() != null) ? ciclo.getFechaInicio().plusDays(15) : LocalDate.now().plusDays(15);
                    crearCargo(matricula, conceptoMatricula, estadoPendiente, estudiante, 1, vencimientoMatricula);
                    cobrosGenerados++;
                }

                for (int numCuota = 1; numCuota <= 6; numCuota++) {
                    int mesActual = mesInicio + (numCuota - 1);
                    LocalDate fechaVencimiento = LocalDate.of(anioCiclo, mesActual, 15);

                    boolean existeCuota = cargoEstudianteRepository.existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobroAndNumeroCuota(
                            matricula.getIdMatricula(), conceptoMensualidad.getIdConceptoCobro(), numCuota);

                    if (!existeCuota) {
                        crearCargo(matricula, conceptoMensualidad, estadoPendiente, estudiante, numCuota, fechaVencimiento);
                        cobrosGenerados++;
                    }
                }
            }
        }
        return cobrosGenerados;
    }

    private void crearCargo(Matricula matricula, ConceptoCobro concepto, EstadoCargo estado, Estudiante estudiante, int numeroCuota, LocalDate fechaVencimiento) {
        CargoEstudiante nuevoCargo = new CargoEstudiante();
        nuevoCargo.setMatricula(matricula);
        nuevoCargo.setConceptoCobro(concepto);
        nuevoCargo.setEstadoCargo(estado);
        nuevoCargo.setFechaGeneracion(LocalDateTime.now());
        nuevoCargo.setNumeroCuota(numeroCuota);
        nuevoCargo.setFechaVencimiento(fechaVencimiento);

        BigDecimal montoBase = concepto.getMontoBase();
        BigDecimal descuento = BigDecimal.ZERO;

        Optional<EstudianteBeneficio> beneficioOpt = estudianteBeneficioRepository
                .findByEstudiante_IdEstudianteAndEstadoBeneficioEstudiante_EstadoBeneficioEstudiante(estudiante.getIdEstudiante(), "ACTIVO");

        if (beneficioOpt.isPresent()) {
            nuevoCargo.setEstudianteBeneficio(beneficioOpt.get());
            descuento = new BigDecimal("17.00");
        }

        nuevoCargo.setMontoBase(montoBase);
        nuevoCargo.setMontoDescuento(descuento);
        nuevoCargo.setMontoRecargo(BigDecimal.ZERO);
        nuevoCargo.setMontoTotal(montoBase.subtract(descuento));

        cargoEstudianteRepository.save(nuevoCargo);
    }

    @Transactional(readOnly = true)
    public EstadoCuentaResponseDTO consultarEstadoCuentaPorCarnetODocumento(String identificador) {

        Estudiante estudiante = estudianteRepository.findByCarnet(identificador)
                .orElseGet(() -> estudianteRepository.findByPersona_NumeroDocumento(identificador)
                        .orElseThrow(() -> new RuntimeException("No se encontró ningún estudiante o aspirante con el identificador proporcionado: " + identificador)));

        boolean esBecado = estudianteBeneficioRepository
                .existsByEstudiante_IdEstudianteAndEstadoBeneficioEstudiante_EstadoBeneficioEstudiante(estudiante.getIdEstudiante(), "ACTIVO");

        String carnetMostrar = (estudiante.getUsuario() == null)
                ? "Aspirante (En proceso)"
                : estudiante.getCarnet();

        EstudianteCajaDTO estudianteDTO = EstudianteCajaDTO.builder()
                .idEstudiante(estudiante.getIdEstudiante())
                .carnet(carnetMostrar)
                .nombreCompleto(estudiante.getPersona().getNombres() + " " + estudiante.getPersona().getApellidos())
                .esBecado(esBecado)
                .build();

        List<CargoEstudiante> cargosBd = cargoEstudianteRepository
                .findByMatricula_Estudiante_IdEstudianteAndEstadoCargo_EstadoCargoOrderByFechaVencimientoAsc(estudiante.getIdEstudiante(), "PENDIENTE");

        cargosBd.sort((c1, c2) -> {
            boolean isMat1 = c1.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase().contains("matrícul") ||
                    c1.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase().contains("matricul");
            boolean isMat2 = c2.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase().contains("matrícul") ||
                    c2.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase().contains("matricul");

            if (isMat1 && !isMat2) return -1;
            if (!isMat1 && isMat2) return 1;

            if (c1.getFechaVencimiento() != null && c2.getFechaVencimiento() != null) {
                return c1.getFechaVencimiento().compareTo(c2.getFechaVencimiento());
            }
            return 0;
        });

        List<CargoPendienteDTO> listaCargosDTO = new ArrayList<>();
        BigDecimal totalDeudaAcumulada = BigDecimal.ZERO;
        LocalDate hoy = LocalDate.now();

        for (CargoEstudiante cargo : cargosBd) {
            BigDecimal recargoDinamico = BigDecimal.ZERO;
            boolean aplicaMora = false;

            String nombreCobro = cargo.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase();
            boolean esMensualidad = !nombreCobro.contains("matrícula") && !nombreCobro.contains("matricula");

            if (esMensualidad && cargo.getFechaVencimiento() != null && hoy.isAfter(cargo.getFechaVencimiento())) {
                recargoDinamico = new BigDecimal("5.00");
                aplicaMora = true;
            }

            BigDecimal montoTotalConMora = cargo.getMontoBase()
                    .subtract(cargo.getMontoDescuento())
                    .add(recargoDinamico);

            CargoPendienteDTO cargoDTO = CargoPendienteDTO.builder()
                    .idCargo(cargo.getIdCargoEstudiante())
                    .ciclo(cargo.getMatricula().getCiclo().getCodigoCiclo())
                    .concepto(cargo.getConceptoCobro().getTipoCobro().getTipoCobro())
                    .numeroCuota(cargo.getNumeroCuota())
                    .montoBase(cargo.getMontoBase())
                    .montoDescuento(cargo.getMontoDescuento())
                    .montoRecargo(recargoDinamico)
                    .montoTotal(montoTotalConMora)
                    .fechaVencimiento(cargo.getFechaVencimiento())
                    .aplicaMora(aplicaMora)
                    .build();

            listaCargosDTO.add(cargoDTO);
            totalDeudaAcumulada = totalDeudaAcumulada.add(montoTotalConMora);
        }

        return EstadoCuentaResponseDTO.builder()
                .estudiante(estudianteDTO)
                .cargosPendientes(listaCargosDTO)
                .totalDeuda(totalDeudaAcumulada)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerCatalogoArancelesExtras() {
        List<ConceptoCobro> todosLosConceptos = conceptoCobroRepository.findAll();
        Map<String, Map<String, Object>> arancelesUnicos = new HashMap<>();

        for (ConceptoCobro cc : todosLosConceptos) {
            String nombreOriginal = cc.getTipoCobro().getTipoCobro();
            String nombreLower = nombreOriginal.toLowerCase();

            if (!nombreLower.contains("matrícula") && !nombreLower.contains("matricula")
                    && !nombreLower.contains("cuota mensual") && !nombreLower.contains("doce cuotas")) {

                if (!arancelesUnicos.containsKey(nombreOriginal)) {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("idConcepto", cc.getIdConceptoCobro());
                    dto.put("nombre", nombreOriginal);
                    dto.put("monto", cc.getMontoBase());
                    arancelesUnicos.put(nombreOriginal, dto);
                }
            }
        }
        return new ArrayList<>(arancelesUnicos.values());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> procesarPagoGral(ProcesarPagoRequestDTO request, String correoUsuarioEjecutor, Long idSede, Long idCanalPago) {
        boolean tieneFijos = request.getIdsCargosAPagar() != null && !request.getIdsCargosAPagar().isEmpty();
        boolean tieneAdicionales = request.getArancelesAdicionales() != null && !request.getArancelesAdicionales().isEmpty();

        if (!tieneFijos && !tieneAdicionales) {
            throw new RuntimeException("Debe seleccionar al menos un cargo o arancel para procesar el cobro.");
        }

        Estudiante estudiantePagador = estudianteRepository.findById(request.getIdEstudiante())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado."));
        Persona personaEstudiante = estudiantePagador.getPersona();

        BigDecimal totalEsperado = BigDecimal.ZERO;
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalDescuento = BigDecimal.ZERO;
        BigDecimal totalRecargo = BigDecimal.ZERO;

        LocalDate hoy = LocalDate.now();
        List<CargoEstudiante> cargosProcesados = new ArrayList<>();

        if (tieneFijos) {
            List<CargoEstudiante> cargosPendientes = cargoEstudianteRepository
                    .findByMatricula_Estudiante_IdEstudianteAndEstadoCargo_EstadoCargoOrderByFechaVencimientoAsc(
                            request.getIdEstudiante(), "PENDIENTE");

            for (int i = 0; i < request.getIdsCargosAPagar().size(); i++) {
                Long idSolicitado = request.getIdsCargosAPagar().get(i);
                CargoEstudiante cargoBd = cargosPendientes.stream()
                        .filter(c -> c.getIdCargoEstudiante().equals(idSolicitado))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("El cargo solicitado no existe o ya fue pagado."));

                String nombreCobro = cargoBd.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase();
                boolean esMensualidad = !nombreCobro.contains("matrícula") && !nombreCobro.contains("matricula");

                BigDecimal recargo = BigDecimal.ZERO;
                if (esMensualidad && cargoBd.getFechaVencimiento() != null && hoy.isAfter(cargoBd.getFechaVencimiento())) {
                    recargo = new BigDecimal("5.00");
                    cargoBd.setMontoRecargo(recargo);
                }

                BigDecimal totalCuota = cargoBd.getMontoBase().subtract(cargoBd.getMontoDescuento()).add(recargo);
                cargoBd.setMontoTotal(totalCuota);
                totalEsperado = totalEsperado.add(totalCuota);

                totalSubtotal = totalSubtotal.add(cargoBd.getMontoBase() != null ? cargoBd.getMontoBase() : BigDecimal.ZERO);
                totalDescuento = totalDescuento.add(cargoBd.getMontoDescuento() != null ? cargoBd.getMontoDescuento() : BigDecimal.ZERO);
                totalRecargo = totalRecargo.add(recargo);

                cargosProcesados.add(cargoBd);
            }
        }

        if (tieneAdicionales) {
            Matricula matriculaActiva = matriculaRepository.findFirstByEstudiante_IdEstudianteOrderByFechaMatriculaDesc(request.getIdEstudiante())
                    .orElseThrow(() -> new RuntimeException("El estudiante no posee una matrícula registrada para asociarle el arancel."));

            EstadoCargo estadoPagado = estadoCargoRepository.findById(2L)
                    .orElseThrow(() -> new RuntimeException("Estado PAGADO no configurado (ID 2)."));

            Map<Long, Integer> cuotasAsignadasEnMemoria = new HashMap<>();

            for (Long idConcepto : request.getArancelesAdicionales()) {
                ConceptoCobro concepto = conceptoCobroRepository.findById(idConcepto)
                        .orElseThrow(() -> new RuntimeException("El arancel solicitado no existe en el catálogo."));

                CargoEstudiante nuevoCargo = new CargoEstudiante();
                nuevoCargo.setMatricula(matriculaActiva);
                nuevoCargo.setConceptoCobro(concepto);
                nuevoCargo.setEstadoCargo(estadoPagado);
                nuevoCargo.setFechaGeneracion(LocalDateTime.now());
                nuevoCargo.setFechaVencimiento(hoy);

                int nextCuota = cuotasAsignadasEnMemoria.getOrDefault(idConcepto, 0);
                if (nextCuota == 0) {
                    nextCuota = 1;
                    while (cargoEstudianteRepository.existsByMatricula_IdMatriculaAndConceptoCobro_IdConceptoCobroAndNumeroCuota(
                            matriculaActiva.getIdMatricula(), concepto.getIdConceptoCobro(), nextCuota)) {
                        nextCuota++;
                    }
                } else {
                    nextCuota++;
                }
                cuotasAsignadasEnMemoria.put(idConcepto, nextCuota);

                nuevoCargo.setNumeroCuota(nextCuota);
                BigDecimal monto = concepto.getMontoBase();
                nuevoCargo.setMontoBase(monto);
                nuevoCargo.setMontoDescuento(BigDecimal.ZERO);
                nuevoCargo.setMontoRecargo(BigDecimal.ZERO);
                nuevoCargo.setMontoTotal(monto);

                cargoEstudianteRepository.save(nuevoCargo);

                totalEsperado = totalEsperado.add(monto);
                totalSubtotal = totalSubtotal.add(monto);
                cargosProcesados.add(nuevoCargo);
            }
        }

        if (request.getMontoRecibido().compareTo(totalEsperado) < 0) {
            throw new RuntimeException("Monto insuficiente. Se esperaban $" + totalEsperado + " pero se recibieron $" + request.getMontoRecibido());
        }

        MetodoPago metodoPago = metodoPagoRepository.findById(Long.valueOf(request.getIdMetodoPago().toString()))
                .orElseThrow(() -> new RuntimeException("El método de pago proporcionado no es válido."));

        Pago nuevoPago = new Pago();
        nuevoPago.setMontoPagado(totalEsperado);
        nuevoPago.setFechaPago(LocalDateTime.now());

        String refInput = request.getNumeroReferencia();
        if (refInput == null || refInput.trim().isEmpty() || refInput.equalsIgnoreCase("PAGO-EFECTIVO")) {
            nuevoPago.setReferencia("EFE-" + System.currentTimeMillis());
        } else {
            nuevoPago.setReferencia(refInput);
        }

        nuevoPago.setEstudiante(estudiantePagador);
        nuevoPago.setMetodoPago(metodoPago);

        CanalPago canal = new CanalPago();
        canal.setIdCanalPago(idCanalPago);
        nuevoPago.setCanalPago(canal);

        EstadoPago estado = new EstadoPago();
        estado.setIdEstadoPago(2L);
        nuevoPago.setEstadoPago(estado);

        if (idCanalPago == 1L) {
            PuntoRecaudo punto = puntoRecaudoRepository.findFirstBySede_IdSedeAndActivoTrue(idSede)
                    .orElseThrow(() -> new RuntimeException("No se encontró una caja activa para procesar el pago."));
            nuevoPago.setPuntoRecaudo(punto);
        } else {
            nuevoPago.setPuntoRecaudo(null);
        }

        Usuario usuarioEjecutorEntity = usuarioRepository.findByCorreoInstitucional(correoUsuarioEjecutor)
                .orElseThrow(() -> new RuntimeException("Usuario ejecutor no encontrado."));
        nuevoPago.setUsuarioRegistro(usuarioEjecutorEntity);

        pagoRepository.save(nuevoPago);

        EstadoCargo estadoPagado = estadoCargoRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("Estado PAGADO no configurado (ID 2)."));

        for (CargoEstudiante cargo : cargosProcesados) {
            if (cargo.getEstadoCargo().getIdEstadoCargo() != 2L) {
                cargo.setEstadoCargo(estadoPagado);
                cargoEstudianteRepository.save(cargo);
            }

            PagoCargo pagoCargo = new PagoCargo();
            pagoCargo.setPago(nuevoPago);
            pagoCargo.setCargo(cargo);
            pagoCargo.setMontoAplicado(cargo.getMontoTotal());
            pagoCargoRepository.save(pagoCargo);

            String nombreCobro = cargo.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase();
            if (nombreCobro.contains("matrícula") || nombreCobro.contains("matricula")) {
                Matricula matriculaPagada = cargo.getMatricula();

                if (matriculaPagada != null && matriculaPagada.getEstadoMatricula() != null &&
                        matriculaPagada.getEstadoMatricula().getIdEstadoMatricula() == 1L) {

                    EstadoMatricula estadoMatriculada = estadoMatriculaRepository.findById(2L)
                            .orElseThrow(() -> new RuntimeException("Estado de matrícula MATRICULADA no configurado (ID 2)."));

                    matriculaPagada.setEstadoMatricula(estadoMatriculada);
                    matriculaRepository.save(matriculaPagada);
                    log.info("Matrícula ID {} actualizada exitosamente a MATRICULADA tras efectuar el pago.", matriculaPagada.getIdMatricula());
                }
            }

            // =====================================================================
            // LÓGICA AUTOMÁTICA DE RETIRO DE MATERIA AL PAGAR EL ARANCEL
            // =====================================================================
            if (nombreCobro.contains("retiro") && nombreCobro.contains("materia")) {
                List<Inscripcion> materiasPendientesRetiro = inscripcionRepository
                        .findByMatriculaAndEstadoRetiroPendiente(cargo.getMatricula().getIdMatricula());

                if (!materiasPendientesRetiro.isEmpty()) {
                    Inscripcion inscripcionARetirar = materiasPendientesRetiro.get(0);

                    // Buscamos el estado por NOMBRE, no por ID quemado
                    EstadoMateriaEstudiante estadoRetirado = estadoMateriaEstudianteRepository
                            .findByEstadoMateriaIgnoreCase("RETIRADA")
                            .orElseThrow(() -> new RuntimeException("El estado 'RETIRADA' no existe en la base de datos."));

                    inscripcionARetirar.setEstadoMateria(estadoRetirado);
                    inscripcionARetirar.setFechaRetiro(LocalDateTime.now()); // Seteamos la fecha de retiro
                    inscripcionRepository.save(inscripcionARetirar);

                    log.info("Materia {} retirada automáticamente tras efectuar el pago del arancel.", inscripcionARetirar.getGrupo().getMateria().getCodigoMateria());
                }
            }
            // =====================================================================
        }

        Factura factura = new Factura();
        factura.setPago(nuevoPago);
        factura.setNumeroFactura("FAC-" + System.currentTimeMillis());
        factura.setFechaEmision(LocalDateTime.now());

        factura.setNombreReceptor(personaEstudiante.getNombres() + " " + personaEstudiante.getApellidos());
        factura.setCorreoReceptor(personaEstudiante.getCorreoPersonal() != null && !personaEstudiante.getCorreoPersonal().isEmpty() ? personaEstudiante.getCorreoPersonal() : "sin-correo@uma.edu.sv");

        String docFactura = (estudiantePagador.getUsuario() == null)
                ? personaEstudiante.getNumeroDocumento()
                : estudiantePagador.getCarnet();
        factura.setDocumentoReceptor(docFactura);
        factura.setDireccionReceptor(personaEstudiante.getDireccion() != null && !personaEstudiante.getDireccion().trim().isEmpty() ? personaEstudiante.getDireccion() : "Santa Ana, El Salvador");

        factura.setSubtotal(totalSubtotal);
        factura.setDescuento(totalDescuento);
        factura.setRecargo(totalRecargo);
        factura.setTotal(totalEsperado);

        EstadoFactura estadoFactura = new EstadoFactura();
        estadoFactura.setIdEstadoFactura(1L);
        factura.setEstadoFactura(estadoFactura);

        facturaRepository.save(factura);

        boolean esNuevoIngreso = (estudiantePagador.getUsuario() == null);
        String correoInstitucionalGenerado = null;
        String claveTemporal = null;

        if (esNuevoIngreso) {
            correoInstitucionalGenerado = estudiantePagador.getCarnet().toLowerCase() + "@uma.edu.sv";
            claveTemporal = estudiantePagador.getCarnet() + "Uma*";

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setPersona(personaEstudiante);
            nuevoUsuario.setCorreoInstitucional(correoInstitucionalGenerado);
            nuevoUsuario.setPasswordHash(passwordEncoder.encode(claveTemporal));

            Rol rolEstudiante = rolRepository.findById(2L)
                    .orElseThrow(() -> new RuntimeException("Rol ESTUDIANTE no configurado (ID 2)."));
            nuevoUsuario.setRol(rolEstudiante);

            EstadoUsuario estadoActivoUsuario = estadoUsuarioRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Estado de usuario ACTIVO no configurado (ID 1)."));
            nuevoUsuario.setEstadoUsuario(estadoActivoUsuario);

            usuarioRepository.save(nuevoUsuario);

            estudiantePagador.setUsuario(nuevoUsuario);
            estudianteRepository.save(estudiantePagador);

            log.info("Nuevo usuario institucional creado exitosamente para: {}", correoInstitucionalGenerado);
        }

        final BigDecimal totalCobradoFinal = totalEsperado;

        List<ComprobantePagoDTO.DetalleComprobanteDTO> detallesComprobante = new ArrayList<>();
        for (CargoEstudiante cargo : cargosProcesados) {
            String descripcion = cargo.getConceptoCobro().getTipoCobro().getTipoCobro();
            String nombreLower = descripcion.toLowerCase();

            if (nombreLower.contains("cuota") && cargo.getFechaVencimiento() != null) {
                String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
                int mesVencimiento = cargo.getFechaVencimiento().getMonthValue();
                descripcion += " - Mes de " + meses[mesVencimiento - 1];
            }

            detallesComprobante.add(ComprobantePagoDTO.DetalleComprobanteDTO.builder()
                    .concepto(descripcion)
                    .montoTotal(cargo.getMontoTotal())
                    .build());
        }

        String correoReal = personaEstudiante.getCorreoPersonal();
        String nombreCompletoEst = personaEstudiante.getNombres() + " " + personaEstudiante.getApellidos();

        String nombreCajero = (idCanalPago == 2L) ? "PAGO EN LÍNEA (PORTAL)" : (usuarioEjecutorEntity.getPersona().getNombres() + " " + usuarioEjecutorEntity.getPersona().getApellidos());

        String carnetComprobante = estudiantePagador.getCarnet();
        String nroFactura = factura.getNumeroFactura();
        LocalDateTime fechaEmision = factura.getFechaEmision();

        final String correoInstHilo = correoInstitucionalGenerado;
        final String claveHilo = claveTemporal;

        ComprobantePagoDTO comprobanteDTO = ComprobantePagoDTO.builder()
                .numeroFactura(nroFactura)
                .fechaEmision(fechaEmision)
                .nombreCompleto(nombreCompletoEst)
                .carnetEstudiante(carnetComprobante)
                .correoEstudiante(correoReal != null && !correoReal.trim().isEmpty() ? correoReal : "N/A")
                .totalCobrado(totalCobradoFinal)
                .cajeroResponsable(nombreCajero)
                .detalles(detallesComprobante)
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                byte[] pdfBytes = comprobantePdfService.generarPdf(comprobanteDTO);

                if (correoReal != null && !correoReal.trim().isEmpty()) {
                    emailService.enviarComprobantePago(
                            correoReal,
                            nombreCompletoEst,
                            nroFactura,
                            pdfBytes
                    );

                    if (esNuevoIngreso) {
                        emailService.enviarCredencialesYBienvenida(
                                correoReal,
                                nombreCompletoEst,
                                carnetComprobante,
                                correoInstHilo,
                                claveHilo
                        );
                    }
                } else {
                    log.warn("El estudiante no tiene correo electrónico registrado.");
                }
            } catch (Exception e) {
                log.error("Fallo al generar el comprobante de pago: {}", e.getMessage(), e);
            }
        });

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Pago procesado exitosamente");
        respuesta.put("factura", factura.getNumeroFactura());
        respuesta.put("totalCobrado", totalEsperado);

        return respuesta;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerArancelesPorCiclo(Long idCiclo) {
        List<ConceptoCobro> conceptos = conceptoCobroRepository.findByCiclo_IdCiclo(idCiclo);
        List<Map<String, Object>> lista = new ArrayList<>();

        for (ConceptoCobro c : conceptos) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("idConcepto", c.getIdConceptoCobro());
            String nombreOriginal = c.getTipoCobro().getTipoCobro().toLowerCase();
            String nombreCapitalizado = nombreOriginal.substring(0, 1).toUpperCase() + nombreOriginal.substring(1);

            dto.put("concepto", nombreCapitalizado);
            dto.put("montoBase", c.getMontoBase());

            lista.add(dto);
        }

        return lista;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> actualizarPrecioArancel(Long idConcepto, BigDecimal nuevoMonto) {
        ConceptoCobro concepto = conceptoCobroRepository.findById(idConcepto)
                .orElseThrow(() -> new RuntimeException("El arancel solicitado no existe."));

        String estadoCiclo = concepto.getCiclo().getEstadoCiclo().getEstadoCiclo().toUpperCase();

        if (!estadoCiclo.equals("PLANIFICACION") && !estadoCiclo.equals("PLANIFICADO")) {
            throw new RuntimeException("Acción denegada. El ciclo actual se encuentra en estado " + estadoCiclo.toLowerCase() + ", por lo que sus aranceles son de solo lectura.");
        }

        if (nuevoMonto.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El monto del arancel no puede ser negativo.");
        }

        concepto.setMontoBase(nuevoMonto);
        conceptoCobroRepository.save(concepto);

        Map<String, Object> response = new HashMap<>();
        response.put("exito", true);
        response.put("mensaje", "Precio actualizado correctamente.");
        response.put("nuevoMonto", nuevoMonto);

        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> crearNuevoArancelEnCiclo(Long idCiclo, String nombreConcepto, BigDecimal montoBase) {
        Ciclo ciclo = cicloRepository.findById(idCiclo)
                .orElseThrow(() -> new RuntimeException("El ciclo especificado no existe."));

        String estadoCiclo = ciclo.getEstadoCiclo().getEstadoCiclo().toUpperCase();

        if (!estadoCiclo.equals("PLANIFICACION") && !estadoCiclo.equals("PLANIFICADO")) {
            throw new RuntimeException("Acción denegada. Solo se pueden agregar aranceles a ciclos en planificación.");
        }

        if (montoBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El monto del arancel no puede ser negativo.");
        }

        String nombreLimpio = nombreConcepto.trim().toUpperCase();

        TipoCobro tipoCobro = tipoCobroRepository.findByTipoCobroIgnoreCase(nombreLimpio)
                .orElseGet(() -> {
                    TipoCobro nuevoTipo = new TipoCobro();
                    nuevoTipo.setTipoCobro(nombreLimpio);
                    return tipoCobroRepository.save(nuevoTipo);
                });

        boolean yaExiste = conceptoCobroRepository.existsByCiclo_IdCicloAndTipoCobro_IdTipoCobro(idCiclo, tipoCobro.getIdTipoCobro());
        if (yaExiste) {
            throw new RuntimeException("Este arancel ya se encuentra registrado para el ciclo actual.");
        }

        ConceptoCobro nuevoConcepto = new ConceptoCobro();
        nuevoConcepto.setCiclo(ciclo);
        nuevoConcepto.setTipoCobro(tipoCobro);
        nuevoConcepto.setMontoBase(montoBase);

        conceptoCobroRepository.save(nuevoConcepto);

        Map<String, Object> response = new HashMap<>();
        response.put("exito", true);
        response.put("mensaje", "Arancel registrado exitosamente.");
        response.put("idConcepto", nuevoConcepto.getIdConceptoCobro());

        return response;
    }

    @Transactional(readOnly = true)
    public EstadoCuentaCicloDTO consultarEstadoCuentaPorCiclo(String identificador, Long idCiclo) {

        Estudiante estudiante = estudianteRepository.findByCarnet(identificador)
                .orElseGet(() -> estudianteRepository.findByPersona_NumeroDocumento(identificador)
                        .orElseThrow(() -> new RuntimeException("No se encontró ningún estudiante con el identificador: " + identificador)));

        List<CargoEstudiante> cargosDelCiclo = cargoEstudianteRepository
                .findByMatricula_Estudiante_IdEstudianteAndMatricula_Ciclo_IdCiclo(estudiante.getIdEstudiante(), idCiclo);

        if (cargosDelCiclo.isEmpty()) {
            throw new RuntimeException("No se encontraron registros financieros para el estudiante en el ciclo seleccionado.");
        }

        Matricula matricula = cargosDelCiclo.get(0).getMatricula();

        String nombreCarrera = estudianteCarreraRepository
                .findNombreCarreraByEstudianteId(estudiante.getIdEstudiante())
                .orElse("Carrera no asignada");

        EstadoCuentaCicloDTO.EstudianteInfo infoEstudiante = EstadoCuentaCicloDTO.EstudianteInfo.builder()
                .idEstudiante(estudiante.getIdEstudiante())
                .carnet(estudiante.getCarnet())
                .nombreCompleto(estudiante.getPersona().getNombres() + " " + estudiante.getPersona().getApellidos())
                .carrera(nombreCarrera)
                .build();

        BigDecimal deudaTotal = BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;
        BigDecimal saldoPendiente = BigDecimal.ZERO;

        List<EstadoCuentaCicloDTO.CargoDetalle> listaPendientes = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        String[] nombresMeses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

        for (CargoEstudiante cargo : cargosDelCiclo) {
            BigDecimal recargoMora = BigDecimal.ZERO;
            String estadoCargo = cargo.getEstadoCargo().getEstadoCargo().toUpperCase();
            String nombreCobro = cargo.getConceptoCobro().getTipoCobro().getTipoCobro().toLowerCase();
            boolean esMensualidad = nombreCobro.contains("cuota");

            if (estadoCargo.equals("PENDIENTE") && esMensualidad && cargo.getFechaVencimiento() != null && hoy.isAfter(cargo.getFechaVencimiento())) {
                recargoMora = new BigDecimal("5.00");
            }

            BigDecimal montoBaseNeto = cargo.getMontoBase().subtract(cargo.getMontoDescuento());
            BigDecimal montoFinalCargo = montoBaseNeto.add(recargoMora);

            deudaTotal = deudaTotal.add(montoFinalCargo);

            String mesAsignado = cargo.getFechaVencimiento() != null ? nombresMeses[cargo.getFechaVencimiento().getMonthValue() - 1] : "N/A";

            if (estadoCargo.equals("PENDIENTE")) {
                saldoPendiente = saldoPendiente.add(montoFinalCargo);

                listaPendientes.add(EstadoCuentaCicloDTO.CargoDetalle.builder()
                        .idCargo(cargo.getIdCargoEstudiante())
                        .concepto(cargo.getConceptoCobro().getTipoCobro().getTipoCobro())
                        .mes(mesAsignado)
                        .fechaVencimiento(cargo.getFechaVencimiento())
                        .monto(montoFinalCargo)
                        .montoBase(montoBaseNeto)
                        .montoRecargo(recargoMora)
                        .build());
            }
        }

        List<PagoCargo> pagosAplicados = pagoCargoRepository.findByCargo_Matricula_IdMatricula(matricula.getIdMatricula());
        List<EstadoCuentaCicloDTO.PagoDetalle> listaPagos = new ArrayList<>();

        for (PagoCargo pc : pagosAplicados) {
            totalPagado = totalPagado.add(pc.getMontoAplicado());
            Pago pago = pc.getPago();
            String conceptoPagado = pc.getCargo().getConceptoCobro().getTipoCobro().getTipoCobro();

            if (conceptoPagado.toLowerCase().contains("cuota") && pc.getCargo().getFechaVencimiento() != null) {
                conceptoPagado += " (" + nombresMeses[pc.getCargo().getFechaVencimiento().getMonthValue() - 1] + ")";
            }

            listaPagos.add(EstadoCuentaCicloDTO.PagoDetalle.builder()
                    .idPago(pago.getIdPago())
                    .numeroRecibo(pago.getReferencia())
                    .fechaPago(pago.getFechaPago().toLocalDate())
                    .concepto(conceptoPagado)
                    .metodoPago(pago.getMetodoPago().getMetodoPago())
                    .montoPagado(pc.getMontoAplicado())
                    .build());
        }

        EstadoCuentaCicloDTO.ResumenFinanciero resumen = EstadoCuentaCicloDTO.ResumenFinanciero.builder()
                .deudaTotal(deudaTotal)
                .totalPagado(totalPagado)
                .saldoPendiente(saldoPendiente)
                .build();

        return EstadoCuentaCicloDTO.builder()
                .estudiante(infoEstudiante)
                .resumen(resumen)
                .cargosPendientes(listaPendientes)
                .historialPagos(listaPagos)
                .build();
    }

    // ... aquí arriba está el resto de tus métodos (obtenerArancelesPorCiclo, etc) ...

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerCiclosPorEstudiante(String carnet) {
        Estudiante estudiante = estudianteRepository.findByCarnet(carnet)
                .orElseGet(() -> estudianteRepository.findByPersona_NumeroDocumento(carnet)
                        .orElseThrow(() -> new RuntimeException("Estudiante no encontrado.")));

        // Obtenemos todos los ciclos ordenados del más reciente al más antiguo
        List<Ciclo> todosLosCiclos = cicloRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "idCiclo"));

        // AQUÍ ESTABA EL ERROR: Faltaba el Map<String, Object>
        List<Map<String, Object>> ciclosDelEstudiante = new ArrayList<>();

        for (Ciclo ciclo : todosLosCiclos) {
            // Verificamos si el estudiante está matriculado en este ciclo
            Optional<Matricula> matricula = matriculaRepository.findByEstudianteIdEstudianteAndCicloIdCiclo(
                    estudiante.getIdEstudiante(), ciclo.getIdCiclo());

            if (matricula.isPresent()) {
                Map<String, Object> map = new HashMap<>();
                map.put("idCiclo", ciclo.getIdCiclo());
                map.put("codigoCiclo", ciclo.getCodigoCiclo());
                // Opcional: Si quieres que el frontend sepa el estado, puedes agregarlo aquí
                // map.put("estadoCiclo", ciclo.getEstadoCiclo().getEstadoCiclo());
                ciclosDelEstudiante.add(map);
            }
        }
        return ciclosDelEstudiante;
    }

    @Transactional(readOnly = true)
    public byte[] descargarComprobanteHistorico(Long idPago) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado."));

        Estudiante estudiante = pago.getEstudiante();
        Persona persona = estudiante.getPersona();

        List<PagoCargo> pagosCargos = pagoCargoRepository.findByPago_IdPago(idPago);

        List<ComprobantePagoDTO.DetalleComprobanteDTO> detalles = new ArrayList<>();
        for (PagoCargo pc : pagosCargos) {
            String concepto = pc.getCargo().getConceptoCobro().getTipoCobro().getTipoCobro();
            detalles.add(ComprobantePagoDTO.DetalleComprobanteDTO.builder()
                    .concepto(concepto)
                    .montoTotal(pc.getMontoAplicado())
                    .build());
        }

        String cajero = "Pago en línea";
        if (pago.getUsuarioRegistro() != null) {
            cajero = pago.getUsuarioRegistro().getPersona().getNombres() + " " + pago.getUsuarioRegistro().getPersona().getApellidos();
        }

        ComprobantePagoDTO comprobanteDTO = ComprobantePagoDTO.builder()
                .numeroFactura(pago.getReferencia())
                .fechaEmision(pago.getFechaPago())
                .nombreCompleto(persona.getNombres() + " " + persona.getApellidos())
                .carnetEstudiante(estudiante.getCarnet())
                .correoEstudiante(persona.getCorreoPersonal() != null ? persona.getCorreoPersonal() : "N/A")
                .totalCobrado(pago.getMontoPagado())
                .cajeroResponsable(cajero)
                .detalles(detalles)
                .build();

        return comprobantePdfService.generarPdf(comprobanteDTO);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerPagosPorFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        List<Pago> pagos = pagoRepository.findByFechaPagoBetweenOrderByFechaPagoDesc(inicio, fin);
        List<Map<String, Object>> listaDto = new ArrayList<>();

        for (Pago p : pagos) {
            Map<String, Object> map = new HashMap<>();
            map.put("idPago", p.getIdPago());
            map.put("referencia", p.getReferencia());
            map.put("fechaPago", p.getFechaPago());
            map.put("montoTotal", p.getMontoPagado());
            map.put("metodoPago", p.getMetodoPago().getMetodoPago());
            map.put("carnet", p.getEstudiante().getCarnet());
            map.put("nombreEstudiante", p.getEstudiante().getPersona().getNombres() + " " + p.getEstudiante().getPersona().getApellidos());
            listaDto.add(map);
        }
        return listaDto;
    }

    @Transactional(readOnly = true)
    public Map<String, String> reenviarComprobantePorCorreo(Long idPago) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado."));

        Estudiante estudiante = pago.getEstudiante();
        Persona persona = estudiante.getPersona();
        String correoReal = persona.getCorreoPersonal();

        if (correoReal == null || correoReal.trim().isEmpty()) {
            throw new RuntimeException("El estudiante no tiene un correo electrónico personal registrado.");
        }

        List<PagoCargo> pagosCargos = pagoCargoRepository.findByPago_IdPago(idPago);
        List<ComprobantePagoDTO.DetalleComprobanteDTO> detalles = new ArrayList<>();
        for (PagoCargo pc : pagosCargos) {
            detalles.add(ComprobantePagoDTO.DetalleComprobanteDTO.builder()
                    .concepto(pc.getCargo().getConceptoCobro().getTipoCobro().getTipoCobro())
                    .montoTotal(pc.getMontoAplicado())
                    .build());
        }

        String cajero = (pago.getUsuarioRegistro() != null) ?
                pago.getUsuarioRegistro().getPersona().getNombres() + " " + pago.getUsuarioRegistro().getPersona().getApellidos() : "Pago en línea";

        ComprobantePagoDTO comprobanteDTO = ComprobantePagoDTO.builder()
                .numeroFactura(pago.getReferencia())
                .fechaEmision(pago.getFechaPago())
                .nombreCompleto(persona.getNombres() + " " + persona.getApellidos())
                .carnetEstudiante(estudiante.getCarnet())
                .correoEstudiante(correoReal)
                .totalCobrado(pago.getMontoPagado())
                .cajeroResponsable(cajero)
                .detalles(detalles)
                .build();

        // Enviar en segundo plano para no bloquear al cliente
        CompletableFuture.runAsync(() -> {
            try {
                byte[] pdfBytes = comprobantePdfService.generarPdf(comprobanteDTO);
                emailService.enviarComprobantePago(
                        correoReal,
                        persona.getNombres() + " " + persona.getApellidos(),
                        pago.getReferencia(),
                        pdfBytes
                );
            } catch (Exception e) {
                log.error("Error al reenviar el comprobante: {}", e.getMessage());
            }
        });

        Map<String, String> res = new HashMap<>();
        res.put("mensaje", "Comprobante enviado con éxito a " + correoReal);
        return res;
    }

}