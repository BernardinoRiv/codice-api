package com.codice.sra.services;

import com.codice.sra.dtos.*;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ====================================================================
    // NUEVO: MÉTODO PARA CLONAR ARANCELES AL ABRIR UN NUEVO CICLO
    // ====================================================================
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
            nuevo.setTipoCobro(viejo.getTipoCobro()); // Mantiene el mismo nombre/tipo
            nuevo.setMontoBase(viejo.getMontoBase()); // Copia el precio viejo (se podrá editar luego en el CRUD)
            // Si tienes otros campos en ConceptoCobro (como aplicaMora), cópialos aquí.

            conceptoCobroRepository.save(nuevo);
            clonados++;
        }

        return clonados;
    }

    // ====================================================================
    // ACTUALIZADO: GENERACIÓN MASIVA BASADA EN EL DTO DEL FRONTEND
    // ====================================================================
    @Transactional(rollbackFor = Exception.class)
    public int generarCobrosMatriculaAperturaCiclo(GenerarCobrosMasivosRequestDTO request) {

        Ciclo ciclo = cicloRepository.findById(request.getIdCiclo())
                .orElseThrow(() -> new RuntimeException("El ciclo especificado no existe."));

        EstadoCargo estadoPendiente = estadoCargoRepository.findByEstadoCargo("PENDIENTE")
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
                    continue; // Si el frontend no mandó IDs para esta modalidad, la omitimos
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
    public EstadoCuentaResponseDTO consultarEstadoCuentaPorCarnet(String carnet) {
        Estudiante estudiante = estudianteRepository.findByCarnet(carnet)
                .orElseThrow(() -> new RuntimeException("No se encontró ningún estudiante con el carnet: " + carnet));

        boolean esBecado = estudianteBeneficioRepository
                .existsByEstudiante_IdEstudianteAndEstadoBeneficioEstudiante_EstadoBeneficioEstudiante(estudiante.getIdEstudiante(), "ACTIVO");

        EstudianteCajaDTO estudianteDTO = EstudianteCajaDTO.builder()
                .idEstudiante(estudiante.getIdEstudiante())
                .carnet(estudiante.getCarnet())
                .nombreCompleto(estudiante.getPersona().getNombres() + " " + estudiante.getPersona().getApellidos())
                .esBecado(esBecado)
                .build();

        List<CargoEstudiante> cargosBd = cargoEstudianteRepository
                .findByMatricula_Estudiante_CarnetAndEstadoCargo_EstadoCargoOrderByFechaVencimientoAsc(carnet, "PENDIENTE");

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
    public Map<String, Object> procesarPagoVentanilla(ProcesarPagoRequestDTO request, String usuarioCajero, Long idSedeCajero) {
        // [Este método se mantiene intacto, tal cual lo probamos para los pagos en ventanilla y factura PDF]
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
                CargoEstudiante cargoBd = cargosPendientes.get(i);
                Long idSolicitado = request.getIdsCargosAPagar().get(i);

                if (!cargoBd.getIdCargoEstudiante().equals(idSolicitado)) {
                    throw new RuntimeException("Error de seguridad: Pagos intercalados no permitidos.");
                }

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

            EstadoCargo estadoPagado = estadoCargoRepository.findByEstadoCargo("PAGADO")
                    .orElseThrow(() -> new RuntimeException("Estado PAGADO no configurado."));

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

        PuntoRecaudo punto = puntoRecaudoRepository.findFirstBySede_IdSedeAndActivoTrue(idSedeCajero)
                .orElseThrow(() -> new RuntimeException("No se encontró una caja activa para procesar el pago."));

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
        canal.setIdCanalPago(1L);
        nuevoPago.setCanalPago(canal);

        EstadoPago estado = new EstadoPago();
        estado.setIdEstadoPago(1L);
        nuevoPago.setEstadoPago(estado);

        nuevoPago.setPuntoRecaudo(punto);

        Usuario usuarioCajeroEntity = usuarioRepository.findByCorreoInstitucional(usuarioCajero)
                .orElseThrow(() -> new RuntimeException("Usuario cajero no encontrado."));
        nuevoPago.setUsuarioRegistro(usuarioCajeroEntity);

        pagoRepository.save(nuevoPago);

        EstadoCargo estadoPagado = estadoCargoRepository.findByEstadoCargo("PAGADO")
                .orElseThrow(() -> new RuntimeException("Estado PAGADO no configurado."));

        for (CargoEstudiante cargo : cargosProcesados) {
            if (!cargo.getEstadoCargo().getEstadoCargo().equals("PAGADO")) {
                cargo.setEstadoCargo(estadoPagado);
                cargoEstudianteRepository.save(cargo);
            }

            PagoCargo pagoCargo = new PagoCargo();
            pagoCargo.setPago(nuevoPago);
            pagoCargo.setCargo(cargo);
            pagoCargo.setMontoAplicado(cargo.getMontoTotal());
            pagoCargoRepository.save(pagoCargo);
        }

        Factura factura = new Factura();
        factura.setPago(nuevoPago);
        factura.setNumeroFactura("FAC-" + System.currentTimeMillis());
        factura.setFechaEmision(LocalDateTime.now());

        factura.setNombreReceptor(personaEstudiante.getNombres() + " " + personaEstudiante.getApellidos());
        factura.setCorreoReceptor(personaEstudiante.getCorreoPersonal() != null && !personaEstudiante.getCorreoPersonal().isEmpty() ? personaEstudiante.getCorreoPersonal() : "sin-correo@uma.edu.sv");
        factura.setDocumentoReceptor(personaEstudiante.getNumeroDocumento() != null ? personaEstudiante.getNumeroDocumento() : estudiantePagador.getCarnet());
        factura.setDireccionReceptor(personaEstudiante.getDireccion() != null && !personaEstudiante.getDireccion().trim().isEmpty() ? personaEstudiante.getDireccion() : "Santa Ana, El Salvador");

        factura.setSubtotal(totalSubtotal);
        factura.setDescuento(totalDescuento);
        factura.setRecargo(totalRecargo);
        factura.setTotal(totalEsperado);

        EstadoFactura estadoFactura = new EstadoFactura();
        estadoFactura.setIdEstadoFactura(1L);
        factura.setEstadoFactura(estadoFactura);

        facturaRepository.save(factura);

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
        String nombreCajero = usuarioCajeroEntity.getPersona().getNombres() + " " + usuarioCajeroEntity.getPersona().getApellidos();
        String carnetEstudiante = estudiantePagador.getCarnet();
        String nroFactura = factura.getNumeroFactura();
        LocalDateTime fechaEmision = factura.getFechaEmision();

        ComprobantePagoDTO comprobanteDTO = ComprobantePagoDTO.builder()
                .numeroFactura(nroFactura)
                .fechaEmision(fechaEmision)
                .nombreCompleto(nombreCompletoEst)
                .carnetEstudiante(carnetEstudiante)
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
}