package com.codice.sra.schedulers;

import com.codice.sra.models.CargoEstudiante;
import com.codice.sra.repositories.CargoEstudianteRepository;
import com.codice.sra.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionesScheduler {

    private final CargoEstudianteRepository cargoEstudianteRepository;
    private final EmailService emailService;

    /**
     * Expresión CRON: "0 0 8 5 2-6,8-12 ?"
     * Se ejecuta a las 08:00 AM, el día 5 del mes.
     * Solo en los meses: Febrero a Junio (2-6) y Agosto a Diciembre (8-12).
     */
    @Scheduled(cron = "0 0 8 5 2-6,8-12 ?")
    @Transactional(readOnly = true)
    public void enviarAlertasVencimientoCuotas() {
        log.info("Iniciando envío automático de recordatorios de pago...");

        // Obtenemos el mes y año actual para buscar los cargos que vencen este mes
        LocalDate hoy = LocalDate.now();
        int mesActual = hoy.getMonthValue();
        int anioActual = hoy.getYear();

        // Buscar cargos "PENDIENTE" cuya fecha de vencimiento coincida con el mes y año en curso
        List<CargoEstudiante> cargosPendientes = cargoEstudianteRepository
                .findCargosPendientesPorMesYAnio("PENDIENTE", mesActual, anioActual);

        int correosEnviados = 0;
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (CargoEstudiante cargo : cargosPendientes) {
            String correo = cargo.getMatricula().getEstudiante().getPersona().getCorreoPersonal();

            if (correo != null && !correo.trim().isEmpty()) {
                String nombreCompleto = cargo.getMatricula().getEstudiante().getPersona().getNombres();
                String concepto = cargo.getConceptoCobro().getTipoCobro().getTipoCobro() + " (Cuota " + cargo.getNumeroCuota() + ")";
                String fechaLimite = cargo.getFechaVencimiento().format(formatoFecha);
                String montoTotal = cargo.getMontoBase().subtract(cargo.getMontoDescuento()).toString();

                emailService.enviarRecordatorioPago(correo, nombreCompleto, concepto, fechaLimite, montoTotal);
                correosEnviados++;
            }
        }

        log.info("Proceso finalizado. Se enviaron {} alertas de pago.", correosEnviados);
    }
}