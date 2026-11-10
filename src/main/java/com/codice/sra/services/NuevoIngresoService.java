package com.codice.sra.services;

import com.codice.sra.dtos.NuevoIngresoRequestDTO;
import com.codice.sra.models.*;
import com.codice.sra.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

    private final BeneficioRepository beneficioRepository;
    private final EstudianteBeneficioRepository estudianteBeneficioRepository;
    private final EstadoBeneficioEstudianteRepository estadoBeneficioEstudianteRepository;

    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final EstadoFacturaRepository estadoFacturaRepository;
    private final PagoCargoRepository pagoCargoRepository;
    private final CanalPagoRepository canalPagoRepository;

    @Transactional
    public Map<String, Object> procesarPreMatricula(NuevoIngresoRequestDTO dto) {

        if (dto.getIdGruposSeleccionados() == null || dto.getIdGruposSeleccionados().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos un grupo para matricular.");
        }

        Long idPrimerGrupo = dto.getIdGruposSeleccionados().get(0);
        Grupo primerGrupo = grupoRepository.findById(idPrimerGrupo)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado."));
        Ciclo cicloActivo = primerGrupo.getCiclo();

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

        Estudiante estudiante = new Estudiante();
        estudiante.setPersona(persona);
        estudiante.setUsuario(null);
        estudiante.setFechaIngreso(LocalDate.now());
        estudiante.setCarnet(generarCarnet(persona.getNombres(), persona.getApellidos(), cicloActivo.getAnio()));
        estudiante.setEstadoEstudiante(estadoEstudianteRepository.findById(1L).orElseThrow());
        estudiante = estudianteRepository.save(estudiante);

        EstudianteCarrera trayectoria = new EstudianteCarrera();
        trayectoria.setEstudiante(estudiante);
        trayectoria.setCarreraSede(carreraSedeRepository.findById(dto.getIdCarreraSede()).orElseThrow());
        trayectoria.setPensum(pensumRepository.findById(dto.getIdPensum().intValue()).orElseThrow());
        trayectoria.setFechaInicio(LocalDate.now());
        trayectoria.setEstadoTrayectoria(estadoTrayectoriaRepository.findById(1L).orElseThrow());
        trayectoria = estudianteCarreraRepository.save(trayectoria);

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setEstudianteCarrera(trayectoria);
        matricula.setCiclo(cicloActivo);
        matricula.setFechaMatricula(LocalDateTime.now());
        matricula.setEstadoMatricula(estadoMatriculaRepository.findById(1L).orElseThrow());
        matricula = matriculaRepository.save(matricula);

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

        // =====================================================================
        // LÓGICA FINANCIERA
        // =====================================================================
        List<ConceptoCobro> conceptosDelCiclo = conceptoCobroRepository.findByCiclo_IdCiclo(cicloActivo.getIdCiclo());

        Integer tipoBecaId = (dto.getTipoBeca() != null) ? dto.getTipoBeca() : 1;

        Beneficio beneficioSeleccionado = null;
        BigDecimal porcentajeDescuento = BigDecimal.ZERO;
        BigDecimal montoFijoDescuento = BigDecimal.ZERO;
        boolean esFeriaNovembrina = false;

        boolean esFranjaBecaria = (tipoBecaId == 2);

        if (tipoBecaId > 1) {
            beneficioSeleccionado = beneficioRepository.findById(tipoBecaId.longValue())
                    .orElseThrow(() -> new RuntimeException("Beneficio no encontrado."));

            String nombreBeneficio = beneficioSeleccionado.getBeneficio().toLowerCase();
            if (nombreBeneficio.contains("feria novembrina")) esFeriaNovembrina = true;

            // VALIDACIÓN ESTRICTA DE FECHAS PARA PROMOCIONES
            LocalDate hoy = LocalDate.now();
            boolean beneficioVigente = true;

            if (beneficioSeleccionado.getFechaInicio() != null && hoy.isBefore(beneficioSeleccionado.getFechaInicio())) {
                beneficioVigente = false;
            }
            if (beneficioSeleccionado.getFechaFin() != null && hoy.isAfter(beneficioSeleccionado.getFechaFin())) {
                beneficioVigente = false;
            }

            if (beneficioVigente) {
                if (beneficioSeleccionado.getPorcentaje() != null) porcentajeDescuento = beneficioSeleccionado.getPorcentaje();
                if (beneficioSeleccionado.getMonto() != null) montoFijoDescuento = beneficioSeleccionado.getMonto();
            }

            EstudianteBeneficio eb = new EstudianteBeneficio();
            eb.setEstudiante(estudiante);
            eb.setBeneficio(beneficioSeleccionado);
            eb.setEstadoBeneficioEstudiante(estadoBeneficioEstudianteRepository.findById(1L).orElseThrow());
            eb.setFechaSolicitud(LocalDateTime.now());
            estudianteBeneficioRepository.save(eb);
        }

        ConceptoCobro conceptoCuotaFinal = null;
        for (ConceptoCobro c : conceptosDelCiclo) {
            String nombreCobro = c.getTipoCobro().getTipoCobro().toLowerCase();
            if (esFranjaBecaria) {
                if (nombreCobro.contains("franja becaria") || nombreCobro.contains("cuota mensual franja")) {
                    conceptoCuotaFinal = c;
                    break;
                }
            } else {
                if (nombreCobro.contains("cuota mensual") && !nombreCobro.contains("franja")) {
                    conceptoCuotaFinal = c;
                    break;
                }
            }
        }

        if (conceptoCuotaFinal == null) {
            final boolean isFranja = esFranjaBecaria;
            conceptoCuotaFinal = conceptosDelCiclo.stream()
                    .filter(c -> isFranja
                            ? c.getTipoCobro().getTipoCobro().toLowerCase().contains("franja")
                            : (c.getTipoCobro().getTipoCobro().toLowerCase().contains("cuota") && !c.getTipoCobro().getTipoCobro().toLowerCase().contains("franja")))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No se encontró un concepto de cobro válido para las cuotas."));
        }

        ConceptoCobro conceptoMatriculaFinal = conceptosDelCiclo.stream()
                .filter(c -> c.getTipoCobro().getIdTipoCobro() == 1L)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró el concepto de Matrícula en este ciclo."));

        int mesInicio = (cicloActivo.getNumeroCiclo() != null && cicloActivo.getNumeroCiclo() == 2) ? 7 : 1;
        int anioCiclo = cicloActivo.getAnio() != null ? cicloActivo.getAnio() : LocalDate.now().getYear();

        EstadoCargo estadoPendiente = estadoCargoRepository.findById(1L).orElseThrow();
        EstadoCargo estadoPagado = estadoCargoRepository.findById(2L).orElseThrow();

        List<CargoEstudiante> cargosGenerados = new ArrayList<>();

        // --- 1. GENERAR MATRÍCULA ---
        BigDecimal montoMatriculaBase = conceptoMatriculaFinal.getMontoBase();
        BigDecimal descuentoMatricula = BigDecimal.ZERO;

        if (tipoBecaId == 3) {
            descuentoMatricula = montoMatriculaBase;
        }

        CargoEstudiante cargoMatricula = new CargoEstudiante();
        cargoMatricula.setMatricula(matricula);
        cargoMatricula.setConceptoCobro(conceptoMatriculaFinal);
        cargoMatricula.setMontoBase(montoMatriculaBase);
        cargoMatricula.setMontoDescuento(descuentoMatricula);
        cargoMatricula.setMontoRecargo(BigDecimal.ZERO);
        cargoMatricula.setMontoTotal(montoMatriculaBase.subtract(descuentoMatricula).max(BigDecimal.ZERO));
        cargoMatricula.setFechaGeneracion(LocalDateTime.now());
        cargoMatricula.setNumeroCuota(1);
        cargoMatricula.setFechaVencimiento(LocalDate.now().plusDays(3));
        cargoMatricula.setEstadoCargo(tipoBecaId == 3 ? estadoPagado : estadoPendiente);
        cargosGenerados.add(cargoEstudianteRepository.save(cargoMatricula));

        // --- 2. GENERAR CUOTAS ---
        BigDecimal montoCuotaBase = conceptoCuotaFinal.getMontoBase(); // Automáticamente será $70 o $53 según la BD

        for (int numCuota = 1; numCuota <= 6; numCuota++) {
            BigDecimal descuentoCuota = BigDecimal.ZERO;

            if (tipoBecaId == 3) {
                descuentoCuota = montoCuotaBase;
            } else if (esFeriaNovembrina) {
                descuentoCuota = (numCuota == 1) ? calcularDescuento(porcentajeDescuento, montoFijoDescuento, montoCuotaBase) : BigDecimal.ZERO;
            }

            BigDecimal montoTotal = montoCuotaBase.subtract(descuentoCuota).max(BigDecimal.ZERO);

            CargoEstudiante cargoCuota = new CargoEstudiante();
            cargoCuota.setMatricula(matricula);
            cargoCuota.setConceptoCobro(conceptoCuotaFinal);
            cargoCuota.setMontoBase(montoCuotaBase);
            cargoCuota.setMontoDescuento(descuentoCuota);
            cargoCuota.setMontoRecargo(BigDecimal.ZERO);
            cargoCuota.setMontoTotal(montoTotal);
            cargoCuota.setFechaGeneracion(LocalDateTime.now());
            cargoCuota.setNumeroCuota(numCuota);
            int mesActual = mesInicio + (numCuota - 1);
            cargoCuota.setFechaVencimiento(LocalDate.of(anioCiclo, mesActual, 15));
            cargoCuota.setEstadoCargo(tipoBecaId == 3 ? estadoPagado : estadoPendiente);

            cargosGenerados.add(cargoEstudianteRepository.save(cargoCuota));
        }

        // =====================================================================
        // SI ES BECA COMPLETA (100%), GENERAMOS LA FACTURA CON MERMANTE
        // =====================================================================
        if (tipoBecaId == 3) {
            EstadoEstudiante estadoActivo = estadoEstudianteRepository.findById(2L)
                    .orElseThrow(() -> new RuntimeException("Estado ACTIVO no configurado"));
            estudiante.setEstadoEstudiante(estadoActivo);
            estudianteRepository.save(estudiante);

            EstadoMatricula estadoMatriculado = estadoMatriculaRepository.findById(2L)
                    .orElseThrow(() -> new RuntimeException("Estado MATRICULADO no configurado"));
            matricula.setEstadoMatricula(estadoMatriculado);
            matriculaRepository.save(matricula);

            BigDecimal montoTotalCubierto = cargosGenerados.stream()
                    .map(CargoEstudiante::getMontoBase)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            MetodoPago metodoPago = metodoPagoRepository.findById(1L).orElseThrow();
            EstadoPago estadoAprobado = estadoPagoRepository.findById(2L).orElseThrow();

            Pago pagoBeca = new Pago();
            pagoBeca.setMontoPagado(montoTotalCubierto);
            pagoBeca.setFechaPago(LocalDateTime.now());
            pagoBeca.setReferencia("BECA-UMA-" + System.currentTimeMillis());
            pagoBeca.setEstudiante(estudiante);
            pagoBeca.setMetodoPago(metodoPago);
            pagoBeca.setEstadoPago(estadoAprobado);

            CanalPago canal = new CanalPago();
            canal.setIdCanalPago(1L);
            pagoBeca.setCanalPago(canal);

            pagoBeca = pagoRepository.save(pagoBeca);

            for (CargoEstudiante cargo : cargosGenerados) {
                PagoCargo pc = new PagoCargo();
                pc.setPago(pagoBeca);
                pc.setCargo(cargo);
                pc.setMontoAplicado(cargo.getMontoBase());
                pagoCargoRepository.save(pc);
            }

            Factura factura = new Factura();
            factura.setPago(pagoBeca);
            factura.setNumeroFactura("FAC-BECA-" + System.currentTimeMillis());
            factura.setFechaEmision(LocalDateTime.now());
            factura.setNombreReceptor(persona.getNombres() + " " + persona.getApellidos());
            factura.setCorreoReceptor(persona.getCorreoPersonal() != null ? persona.getCorreoPersonal() : "sin-correo@uma.edu.sv");
            factura.setDocumentoReceptor(persona.getNumeroDocumento());
            factura.setDireccionReceptor(persona.getDireccion() != null ? persona.getDireccion() : "Santa Ana");

            factura.setSubtotal(montoTotalCubierto);
            factura.setDescuento(montoTotalCubierto);
            factura.setRecargo(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);

            EstadoFactura estadoFactura = estadoFacturaRepository.findById(1L).orElseThrow();
            factura.setEstadoFactura(estadoFactura);

            facturaRepository.save(factura);
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Pre-matrícula generada con éxito.");

        if (tipoBecaId == 3) {
            respuesta.put("instrucciones", "Beca Institucional aplicada. El estudiante ha sido activado automáticamente y su factura con el mermante ha sido emitida.");
        } else {
            respuesta.put("instrucciones", "Presente su documento en Colectaduría para procesar el pago.");
        }

        return respuesta;
    }

    private BigDecimal calcularDescuento(BigDecimal porcentaje, BigDecimal montoFijo, BigDecimal montoBase) {
        BigDecimal descuento = BigDecimal.ZERO;
        if (porcentaje != null && porcentaje.compareTo(BigDecimal.ZERO) > 0) {
            descuento = montoBase.multiply(porcentaje).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if (montoFijo != null && montoFijo.compareTo(BigDecimal.ZERO) > 0) {
            descuento = montoFijo;
        }
        if (descuento.compareTo(montoBase) > 0) descuento = montoBase;
        return descuento;
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