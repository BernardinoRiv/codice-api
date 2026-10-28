package com.codice.sra.services;

import com.codice.sra.dtos.AlertaSeguridadDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCredenciales(String destinatario, String nombreCompleto, String correoInstitucional, String passwordTemporal) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Bienvenido al Sistema de Registro Académico (SRA) - UMA");

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f7; margin: 0; padding: 0; }
                        .email-container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
                        .email-header { background-color: #111111; text-align: center; padding: 25px; border-bottom: 4px solid #b30000; }
                        .email-header img { max-width: 90px; height: auto; }
                        .email-body { padding: 30px; color: #333333; line-height: 1.6; }
                        .email-body h2 { color: #b30000; margin-top: 0; }
                        .credentials-box { background-color: #f9f9f9; border-left: 4px solid #b30000; padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .credentials-box p { margin: 5px 0; font-size: 15px; }
                        .email-footer { background-color: #f4f4f7; text-align: center; padding: 15px; font-size: 12px; color: #777777; border-top: 1px solid #e0e0e0; }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="email-header">
                            <img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA">
                        </div>
                        <div class="email-body">
                            <h2>Bienvenido/a al Sistema SRA</h2>
                            <p>Estimado/a <strong>%s</strong>,</p>
                            <p>Su cuenta institucional dentro del Sistema de Registro Académico de la Universidad Modular Abierta ha sido configurada con éxito.</p>
                            
                            <div class="credentials-box">
                                <p><strong>Correo Institucional:</strong> %s</p>
                                <p><strong>Contraseña Temporal:</strong> <span style="color: #b30000; font-family: monospace; font-size: 16px;">%s</span></p>
                            </div>
                            
                            <p>Por motivos de seguridad, le solicitamos iniciar sesión en la plataforma y modificar su contraseña temporal a la brevedad posible.</p>
                            <p>Atentamente,<br><strong>Dirección de Tecnologías - UMA</strong></p>
                        </div>
                        <div class="email-footer">
                            <p>&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                    nombreCompleto, correoInstitucional, passwordTemporal
            );

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo electrónico con HTML", e);
        }
    }

    public void enviarAlertaSeguridad(AlertaSeguridadDTO alerta) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(alerta.getCorreoDestinatario());
            helper.setSubject("⚠️ Alerta de Seguridad: Nuevo inicio de sesión en SRA");

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f7; margin: 0; padding: 0; }
                        .email-container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
                        .email-header { background-color: #b30000; text-align: center; padding: 25px; color: white; }
                        .email-body { padding: 30px; color: #333333; line-height: 1.6; }
                        .alert-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
                        .detail-label { font-weight: bold; color: #555; }
                        .email-footer { background-color: #f4f4f7; text-align: center; padding: 15px; font-size: 12px; color: #777777; border-top: 1px solid #e0e0e0; }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="email-header">
                            <h2>⚠️ Alerta de seguridad</h2>
                        </div>
                        <div class="email-body">
                            <p>Estimado/a <strong>%s</strong>,</p>
                            <p>Hemos detectado un inicio de sesión en su cuenta del Sistema de Registro Académico (SRA) desde un dispositivo o ubicación que no reconocemos.</p>
                            
                            <div class="alert-box">
                                <div class="detail-row">
                                    <span class="detail-label">Fecha y Hora:</span>
                                    <span>%s</span>
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Dirección IP:</span>
                                    <span style="font-family: monospace;">%s</span>
                                </div>
                                <div class="detail-row" style="border-bottom: none;">
                                    <span class="detail-label">Dispositivo:</span>
                                    <span>%s</span>
                                </div>
                            </div>
                            
                            <p><strong>¿Fue usted?</strong><br>
                            Si usted realizó este inicio de sesión, puede ignorar este correo. Su cuenta está segura.</p>
                            
                            <p><strong>¿No fue usted?</strong><br>
                            Le recomendamos cambiar su contraseña inmediatamente y contactar a la Dirección de Tecnologías.</p>
                            
                            <p>Atentamente,<br><strong>Dirección de Tecnologías - UMA</strong></p>
                        </div>
                        <div class="email-footer">
                            <p>&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                    alerta.getNombreDestinatario(),
                    alerta.getFechaEvento().toString().replace('T', ' '),
                    alerta.getDireccionIpSospechosa(),
                    alerta.getDispositivoSospechoso()
            );

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar la alerta de seguridad", e);
        }
    }

    public void enviarComprobantePago(String destinatario, String nombreEstudiante, String numeroFactura, byte[] pdfBytes) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // El 'true' aquí es clave: indica que el correo será "multipart" (permite adjuntos)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Comprobante de Pago Electrónico SRA - " + numeroFactura);

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f7; margin: 0; padding: 0; }
                        .email-container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1); }
                        .email-header { background-color: #111111; text-align: center; padding: 25px; border-bottom: 4px solid #b30000; }
                        .email-header img { max-width: 90px; height: auto; }
                        .email-body { padding: 30px; color: #333333; line-height: 1.6; }
                        .email-body h2 { color: #b30000; margin-top: 0; }
                        .info-box { background-color: #f9f9f9; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .email-footer { background-color: #f4f4f7; text-align: center; padding: 15px; font-size: 12px; color: #777777; border-top: 1px solid #e0e0e0; }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="email-header">
                            <img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA">
                        </div>
                        <div class="email-body">
                            <h2>Pago Procesado Exitosamente</h2>
                            <p>Estimado/a <strong>%s</strong>,</p>
                            <p>Le confirmamos que hemos recibido y procesado su pago en la ventanilla financiera de la universidad.</p>
                            
                            <div class="info-box">
                                <p>Adjunto a este correo encontrará su comprobante electrónico en formato PDF (Factura: <strong>%s</strong>).</p>
                                <p>Puede guardarlo para sus registros personales o cualquier trámite académico futuro.</p>
                            </div>
                            
                            <p>Atentamente,<br><strong>Departamento de Finanzas - UMA</strong></p>
                        </div>
                        <div class="email-footer">
                            <p>&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                    nombreEstudiante, numeroFactura
            );

            helper.setText(htmlContent, true);

            // Adjuntar el PDF
            ByteArrayResource pdfAdjunto = new ByteArrayResource(pdfBytes);
            helper.addAttachment("Comprobante_" + numeroFactura + ".pdf", pdfAdjunto);

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el comprobante de pago con adjunto", e);
        }
    }

    public void enviarCodigoRecuperacion(String correoDestino, String nombreUsuario, String codigoCrudo) {
        String asunto = "Código de Recuperación de Contraseña - Códice UMA";
        String cuerpo = "<div style='font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eaeaea; border-radius: 10px;'>"
                + "<h2 style='color: #000;'>Recuperación de Acceso</h2>"
                + "<p>Hola, <b>" + nombreUsuario + "</b>,</p>"
                + "<p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en Códice. Ingresa el siguiente código de 6 dígitos en la aplicación:</p>"
                + "<div style='background-color: #f4f4f4; padding: 15px; text-align: center; border-radius: 8px; margin: 20px 0;'>"
                + "<span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #2E7D32;'>" + codigoCrudo + "</span>"
                + "</div>"
                + "<p style='color: #666; font-size: 13px;'>Este código es válido únicamente por <b>15 minutos</b>.</p>"
                + "<p style='color: #666; font-size: 13px;'>Si no solicitaste este cambio, ignora este correo de forma segura.</p>"
                + "</div>";

        try {
            jakarta.mail.internet.MimeMessage mensaje = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(correoDestino);
            helper.setSubject(asunto);
            helper.setText(cuerpo, true);

            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el correo con el código de recuperación.", e);
        }
    }
    @Async
    public void enviarRecordatorioPago(String correoDestino, String nombreEstudiante, String concepto, String fechaLimite, String monto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("Códice SRA - Recordatorio de Vencimiento de Cuota");

            String html = String.format(
                    "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">" +
                            "  <div style=\"background-color: #0f3b6c; padding: 20px; text-align: center; color: white;\">" +
                            "    <h2 style=\"margin: 0;\">Recordatorio de Pago</h2>" +
                            "  </div>" +
                            "  <div style=\"padding: 30px; color: #334155; background-color: #f8fafc;\">" +
                            "    <p>Hola <strong>%s</strong>,</p>" +
                            "    <p>Te recordamos que tu pago correspondiente a <strong>%s</strong> está próximo a vencer.</p>" +
                            "    <div style=\"background-color: #fffbeb; border-left: 4px solid #d97706; padding: 15px; margin: 20px 0;\">" +
                            "      <p style=\"margin: 0; color: #92400e;\"><strong>Monto a cancelar:</strong> $%s</p>" +
                            "      <p style=\"margin: 5px 0 0 0; color: #92400e;\"><strong>Fecha límite sin mora:</strong> %s</p>" +
                            "    </div>" +
                            "    <p style=\"font-size: 13px; color: #64748b;\">Para evitar recargos por mora ($5.00), te invitamos a realizar tu pago en la ventanilla de la universidad antes de la fecha indicada.</p>" +
                            "  </div>" +
                            "</div>",
                    nombreEstudiante, concepto, monto, fechaLimite
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Recordatorio de pago enviado a {}", correoDestino);
        } catch (Exception e) {
            log.error("Error al enviar recordatorio de pago a {}: {}", correoDestino, e.getMessage());
        }
    }

    @Async
    public void enviarNotificacionNota(String correoDestino, String nombreEstudiante, String materia, String evaluacion, String nota) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("Códice SRA - Nueva Calificación Publicada");

            // Lógica para colores dinámicos basados en la nota (Aprobado >= 6.0)
            double notaValor = 0.0;
            try {
                notaValor = Double.parseDouble(nota);
            } catch (NumberFormatException e) {
                log.warn("La nota no es un número válido para parsear: {}", nota);
            }

            String colorFondoHeader = notaValor >= 6.0 ? "#15803d" : "#b91c1c"; // Verde oscuro o Rojo oscuro
            String colorTextoNota = notaValor >= 6.0 ? "#16a34a" : "#dc2626";   // Verde o Rojo
            String colorFondoNota = notaValor >= 6.0 ? "#dcfce7" : "#fee2e2";   // Fondo claro Verde o Rojo
            String mensajeFeedback = notaValor >= 6.0 ? "¡Sigue así con tu buen rendimiento!" : "Te animamos a esforzarte más en las próximas evaluaciones.";

            String html = String.format(
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset=\"UTF-8\"></head>" +
                            "<body style=\"background-color: #f4f5f7; padding: 30px 10px; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 0;\">" +
                            "  <div style=\"max-width: 550px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">" +
                            "    <div style=\"background-color: %s; padding: 25px; text-align: center; color: #ffffff;\">" +
                            "      <h2 style=\"margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px;\">Calificación Publicada</h2>" +
                            "    </div>" +
                            "    <div style=\"padding: 35px 30px; color: #334155;\">" +
                            "      <p style=\"font-size: 16px; margin-top: 0;\">Hola <strong>%s</strong>,</p>" +
                            "      <p style=\"font-size: 15px; line-height: 1.6; color: #475569;\">Tu catedrático ha publicado de manera oficial una nueva nota en el sistema para la materia de <strong style=\"color: #0f172a;\">%s</strong>.</p>" +
                            "      <div style=\"text-align: center; margin: 40px 0;\">" +
                            "        <p style=\"font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 800; letter-spacing: 1.5px; margin-bottom: 12px;\">%s</p>" +
                            "        <span style=\"font-size: 42px; font-weight: 900; color: %s; background-color: %s; padding: 15px 40px; border-radius: 16px; display: inline-block; border: 2px solid %s; box-shadow: 0 4px 6px rgba(0,0,0,0.02);\">%s</span>" +
                            "      </div>" +
                            "      <p style=\"font-size: 14px; text-align: center; color: #64748b; font-style: italic; margin-bottom: 30px;\">%s</p>" +
                            "      <hr style=\"border: 0; border-top: 1px dashed #cbd5e1; margin: 0 0 20px 0;\" />" +
                            "      <p style=\"font-size: 12px; color: #94a3b8; text-align: center; margin: 0;\">Puedes revisar el detalle de tu rendimiento ingresando a tu portal estudiantil Códice.</p>" +
                            "    </div>" +
                            "  </div>" +
                            "</body>" +
                            "</html>",
                    colorFondoHeader, nombreEstudiante, materia, evaluacion, colorTextoNota, colorFondoNota, colorFondoNota, nota, mensajeFeedback
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Notificación de nota enviada a {}", correoDestino);
        } catch (Exception e) {
            log.error("Error al enviar notificación de nota a {}: {}", correoDestino, e.getMessage());
        }
    }

    @Async
    public void enviarRecordatorioDocenteNotas(String correoDestino, String nombreDocente, String materia, String evaluacion, String fechaLimite, int notasBorrador) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("⚠️ Códice SRA - Recordatorio de Cierre de Actas");

            String html = String.format(
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset=\"UTF-8\"></head>" +
                            "<body style=\"background-color: #f4f5f7; padding: 30px 10px; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 0;\">" +
                            "  <div style=\"max-width: 550px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">" +
                            "    <div style=\"background-color: #d97706; padding: 25px; text-align: center; color: #ffffff;\">" +
                            "      <h2 style=\"margin: 0; font-size: 20px; font-weight: 700; letter-spacing: 0.5px;\">Cierre de Actas Próximo</h2>" +
                            "    </div>" +
                            "    <div style=\"padding: 35px 30px; color: #334155;\">" +
                            "      <p style=\"font-size: 16px; margin-top: 0;\">Estimado/a <strong>%s</strong>,</p>" +
                            "      <p style=\"font-size: 15px; line-height: 1.6; color: #475569;\">El sistema le notifica que faltan <strong>3 días</strong> para el cierre oficial de la evaluación <strong style=\"color: #0f172a;\">%s</strong> en la materia de <strong style=\"color: #0f172a;\">%s</strong>.</p>" +
                            "      <div style=\"background-color: #fffbeb; border-left: 4px solid #f59e0b; padding: 20px; margin: 25px 0; border-radius: 6px;\">" +
                            "        <p style=\"margin: 0 0 10px 0; font-size: 14px;\"><strong>Fecha límite de publicación:</strong> %s</p>" +
                            "        <p style=\"margin: 0; font-size: 14px; color: #b45309;\"><strong>Notas pendientes en Borrador:</strong> %d registros</p>" +
                            "      </div>" +
                            "      <p style=\"font-size: 14px; line-height: 1.6; color: #475569;\">Recuerde ingresar al portal docente y dar clic en <strong>\"Publicar Activas\"</strong> antes de la fecha límite. Las notas en estado \"Borrador\" no son visibles para los estudiantes.</p>" +
                            "      <hr style=\"border: 0; border-top: 1px dashed #cbd5e1; margin: 30px 0 20px 0;\" />" +
                            "      <p style=\"font-size: 12px; color: #94a3b8; text-align: center; margin: 0;\">Este es un mensaje automático del Sistema de Registro Académico (SRA).</p>" +
                            "    </div>" +
                            "  </div>" +
                            "</body>" +
                            "</html>",
                    nombreDocente, evaluacion, materia, fechaLimite, notasBorrador
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Recordatorio docente enviado a {}", correoDestino);
        } catch (Exception e) {
            log.error("Error al enviar recordatorio docente a {}: {}", correoDestino, e.getMessage());
        }
    }
    public void enviarCredencialesYBienvenida(String correoDestino, String nombreCompleto, String carnet, String correoInstitucional, String passwordTemporal) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("¡Bienvenido a UMA! - Credenciales de Acceso");

            String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #333;'>"
                    + "<h2 style='color: #2E7D32;'>¡Bienvenido a la Universidad Modular Abierta!</h2>"
                    + "<p>Hola <strong>" + nombreCompleto + "</strong>,</p>"
                    + "<p>Tu proceso de matrícula ha finalizado con éxito. Oficialmente eres parte de nuestra comunidad estudiantil.</p>"
                    + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 8px; border-left: 4px solid #2E7D32;'>"
                    + "<p style='margin: 0 0 10px 0;'><strong>Tu Carnet de Estudiante:</strong> " + carnet + "</p>"
                    + "<p style='margin: 0 0 10px 0;'><strong>Correo Institucional:</strong> " + correoInstitucional + "</p>"
                    + "<p style='margin: 0;'><strong>Contraseña Temporal:</strong> " + passwordTemporal + "</p>"
                    + "</div>"
                    + "<p>Por seguridad, te recomendamos iniciar sesión en el portal académico y cambiar tu contraseña inmediatamente.</p>"
                    + "<p>Saludos cordiales,<br><strong>Departamento de Administración Académica</strong></p>"
                    + "</div>";

            helper.setText(htmlMsg, true);
            mailSender.send(mensaje);

            log.info("Correo de bienvenida y credenciales enviado a: {}", correoDestino);

        } catch (MessagingException e) {
            log.error("Error al enviar correo de bienvenida a {}: {}", correoDestino, e.getMessage());
        }
    }

    public void enviarBoletaPreMatriculaNpe(
            String correoDestinatario,
            String nombreAspirante,
            String npe,
            BigDecimal monto,
            LocalDate fechaVencimiento) {

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(correoDestinatario);
            helper.setSubject("Orden de Pre-Matrícula - Universidad Modular Abierta");

            String fechaFormateada = fechaVencimiento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            String contenidoHtml = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
                    <div style="background-color: #0d47a1; color: white; padding: 20px; text-align: center;">
                        <h2 style="margin: 0;">Universidad Modular Abierta</h2>
                        <p style="margin: 5px 0 0 0; font-size: 14px;">Módulo de Admisiones y Matrícula en Línea</p>
                    </div>
                    <div style="padding: 24px; color: #333333; line-height: 1.6;">
                        <p>Estimado/a <strong>%s</strong>,</p>
                        <p>Tu solicitud de pre-matrícula ha sido registrada exitosamente. Para asegurar tus cupos en los grupos seleccionados, debes efectuar el pago correspondiente antes de la fecha límite.</p>
                        
                        <div style="background-color: #f5f5f5; border-left: 4px solid #0d47a1; padding: 16px; margin: 20px 0;">
                            <p style="margin: 0 0 8px 0;"><strong>Número de Pago Electrónico (NPE):</strong></p>
                            <p style="margin: 0; font-size: 20px; font-weight: bold; letter-spacing: 2px; color: #0d47a1;">%s</p>
                            <hr style="border: 0; border-top: 1px solid #ddd; margin: 12px 0;">
                            <p style="margin: 0 0 4px 0;"><strong>Monto Total a Cancelar:</strong> $%s</p>
                            <p style="margin: 0; color: #d32f2f;"><strong>Fecha Límite de Pago:</strong> %s (48 horas hábiles)</p>
                        </div>

                        <p><strong>Instrucciones para Colecturía / Agencias Bancarias:</strong></p>
                        <ol style="padding-left: 20px;">
                            <li>Presenta este correo o tu documento de identidad en ventanilla de Colecturía.</li>
                            <li>Indica al cajero el código NPE provisto arriba.</li>
                            <li>Una vez acreditado el pago, el sistema activará tu matrícula y enviará tus credenciales institucionales de acceso a este correo.</li>
                        </ol>

                        <p style="font-size: 12px; color: #777777; margin-top: 24px;">
                            <strong>Aviso Importante:</strong> Si el abono no se registra antes del %s, la pre-matrícula y los cupos reservados serán liberados de manera automática.
                        </p>
                    </div>
                    <div style="background-color: #eeeeee; padding: 12px; text-align: center; font-size: 11px; color: #888888;">
                        Este es un mensaje generado de forma automática. Por favor no responder a este remitente.
                    </div>
                </div>
            """.formatted(nombreAspirante, npe, monto.toString(), fechaFormateada, fechaFormateada);

            helper.setText(contenidoHtml, true);
            mailSender.send(mensaje);
            log.info("Correo con NPE despachado con éxito a {}", correoDestinatario);

        } catch (MessagingException e) {
            log.error("Fallo al construir o enviar el correo de boleta NPE a {}: {}", correoDestinatario, e.getMessage(), e);
            throw new RuntimeException("Error en el envío de la notificación de pre-matrícula.", e);
        }
    }
}