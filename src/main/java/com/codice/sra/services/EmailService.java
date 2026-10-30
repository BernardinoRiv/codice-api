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

    // ==========================================
    // PLANTILLA BASE (Reutilizada en diseño)
    // ==========================================
    private static final String CSS_BASE = """
        body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f5f7; margin: 0; padding: 20px 0; }
        .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
        .email-header { background-color: #111111; text-align: center; padding: 25px; border-bottom: 4px solid #b30000; }
        .email-header img { max-width: 120px; height: auto; }
        .email-body { padding: 35px 30px; color: #333333; line-height: 1.6; }
        .email-body h2 { color: #b30000; margin-top: 0; font-size: 22px; }
        .email-footer { background-color: #f9fafb; text-align: center; padding: 20px; font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; }
        .highlight-box { background-color: #fafafa; border: 1px solid #eeeeee; border-left: 4px solid #b30000; padding: 20px; margin: 25px 0; border-radius: 4px; }
    """;

    // 1. Correo de Credenciales Básicas
    public void enviarCredenciales(String destinatario, String nombreCompleto, String correoInstitucional, String passwordTemporal) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Bienvenido al Sistema de Registro Académico (SRA) - UMA");

            String htmlContent = String.format("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"><style>%s</style></head><body>
                    <div class="email-container">
                        <div class="email-header"><img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA"></div>
                        <div class="email-body">
                            <h2>Acceso al Sistema SRA</h2>
                            <p>Estimado/a <strong>%s</strong>,</p>
                            <p>Su cuenta institucional dentro del Sistema de Registro Académico de la Universidad Modular Abierta ha sido configurada con éxito.</p>
                            
                            <div class="highlight-box">
                                <p style="margin: 0 0 10px 0;"><strong>Correo Institucional:</strong> <span style="color: #0056b3;">%s</span></p>
                                <p style="margin: 0;"><strong>Contraseña Temporal:</strong> <span style="font-family: monospace; font-size: 16px; font-weight: bold; color: #b30000;">%s</span></p>
                            </div>
                            
                            <p>Por motivos de seguridad, le solicitamos iniciar sesión en la plataforma y modificar su contraseña temporal a la brevedad posible.</p>
                            <p>Atentamente,<br><strong>Dirección de Tecnologías - UMA</strong></p>
                        </div>
                        <div class="email-footer">&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</div>
                    </div>
                </body></html>
                """, CSS_BASE, nombreCompleto, correoInstitucional, passwordTemporal);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo electrónico con HTML", e);
        }
    }

    // 2. Correo Especial de Bienvenida y Credenciales (NUEVO INGRESO)
    @Async
    public void enviarCredencialesYBienvenida(String correoDestino, String nombreCompleto, String carnet, String correoInstitucional, String passwordTemporal) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("¡Bienvenido/a a UMA! - Credenciales de Estudiante");

            String htmlContent = String.format("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"><style>%s</style></head><body>
                    <div class="email-container">
                        <div class="email-header"><img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA"></div>
                        <div class="email-body">
                            <h2>¡Bienvenido/a a la Universidad Modular Abierta!</h2>
                            <p>Hola <strong>%s</strong>,</p>
                            <p>Nos llena de alegría confirmarte que tu proceso de matrícula ha finalizado con éxito. A partir de hoy, eres oficialmente parte de nuestra comunidad estudiantil.</p>
                            
                            <div class="highlight-box">
                                <p style="margin: 0 0 12px 0; font-size: 15px;"><strong>Carnet Estudiantil:</strong> <span style="font-weight: bold; font-size: 16px; color: #111;">%s</span></p>
                                <p style="margin: 0 0 12px 0; font-size: 15px;"><strong>Correo Institucional:</strong> <span style="color: #0056b3;">%s</span></p>
                                <p style="margin: 0; font-size: 15px;"><strong>Contraseña Temporal:</strong> <span style="font-family: monospace; font-size: 18px; font-weight: bold; color: #b30000; background: #f0f0f0; padding: 2px 6px; border-radius: 4px;">%s</span></p>
                            </div>
                            
                            <p>Para garantizar la seguridad de tu información, te solicitamos iniciar sesión en el <strong>Portal Códice SRA</strong> y personalizar tu contraseña lo antes posible.</p>
                            <p>¡Te deseamos el mayor de los éxitos en tu carrera profesional!</p>
                            <p style="margin-top: 30px;">Saludos cordiales,<br><strong>Administración Académica - UMA</strong></p>
                        </div>
                        <div class="email-footer">&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</div>
                    </div>
                </body></html>
                """, CSS_BASE, nombreCompleto, carnet, correoInstitucional, passwordTemporal);

            helper.setText(htmlContent, true);
            mailSender.send(mensaje);
            log.info("Correo de bienvenida y credenciales enviado a: {}", correoDestino);
        } catch (MessagingException e) {
            log.error("Error al enviar correo de bienvenida a {}: {}", correoDestino, e.getMessage());
        }
    }

    // 3. Alerta de Seguridad
    public void enviarAlertaSeguridad(AlertaSeguridadDTO alerta) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(alerta.getCorreoDestinatario());
            helper.setSubject("⚠️ Alerta de Seguridad: Nuevo inicio de sesión en SRA");

            String htmlContent = String.format("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"><style>%s</style></head><body>
                    <div class="email-container">
                        <div class="email-header" style="background-color: #b30000; border-bottom: none;">
                            <h2 style="color: white; margin: 0;">⚠️ Alerta de Seguridad</h2>
                        </div>
                        <div class="email-body">
                            <p>Estimado/a <strong>%s</strong>,</p>
                            <p>Hemos detectado un inicio de sesión en su cuenta del Sistema de Registro Académico (SRA) desde un dispositivo o ubicación que no reconocemos.</p>
                            
                            <div class="highlight-box" style="border-left-color: #ffc107; background-color: #fffdf5;">
                                <div style="display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee;">
                                    <strong>Fecha y Hora:</strong> <span>%s</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee;">
                                    <strong>Dirección IP:</strong> <span style="font-family: monospace;">%s</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; padding: 8px 0;">
                                    <strong>Dispositivo:</strong> <span>%s</span>
                                </div>
                            </div>
                            
                            <p><strong>¿Fue usted?</strong><br>Si usted realizó este inicio de sesión, puede ignorar este correo.</p>
                            <p><strong>¿No fue usted?</strong><br>Le recomendamos cambiar su contraseña inmediatamente.</p>
                        </div>
                        <div class="email-footer">&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</div>
                    </div>
                </body></html>
                """, CSS_BASE, alerta.getNombreDestinatario(), alerta.getFechaEvento().toString().replace('T', ' '), alerta.getDireccionIpSospechosa(), alerta.getDispositivoSospechoso());

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar la alerta de seguridad", e);
        }
    }

    // 4. Comprobante de Pago Electrónico
    public void enviarComprobantePago(String destinatario, String nombreEstudiante, String numeroFactura, byte[] pdfBytes) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Comprobante de Pago Electrónico SRA - " + numeroFactura);

            String htmlContent = String.format("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"><style>%s</style></head><body>
                    <div class="email-container">
                        <div class="email-header"><img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA"></div>
                        <div class="email-body">
                            <h2>Pago Procesado Exitosamente</h2>
                            <p>Estimado/a <strong>%s</strong>,</p>
                            <p>Le confirmamos que hemos recibido y procesado su pago en la ventanilla financiera de la universidad.</p>
                            
                            <div class="highlight-box" style="border-left-color: #111111;">
                                <p>Adjunto a este correo encontrará su comprobante electrónico en formato PDF (Factura: <strong>%s</strong>).</p>
                                <p style="margin-bottom: 0;">Puede guardarlo para sus registros personales o cualquier trámite académico futuro.</p>
                            </div>
                            
                            <p>Atentamente,<br><strong>Departamento de Finanzas - UMA</strong></p>
                        </div>
                        <div class="email-footer">&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</div>
                    </div>
                </body></html>
                """, CSS_BASE, nombreEstudiante, numeroFactura);

            helper.setText(htmlContent, true);
            ByteArrayResource pdfAdjunto = new ByteArrayResource(pdfBytes);
            helper.addAttachment("Factura_UMA_" + numeroFactura + ".pdf", pdfAdjunto);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el comprobante de pago", e);
        }
    }

    // 5. Código de Recuperación
    public void enviarCodigoRecuperacion(String correoDestino, String nombreUsuario, String codigoCrudo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("Código de Recuperación de Contraseña - SRA UMA");

            String htmlContent = String.format("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"><style>%s</style></head><body>
                    <div class="email-container">
                        <div class="email-header"><img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA"></div>
                        <div class="email-body">
                            <h2>Recuperación de Acceso</h2>
                            <p>Hola <strong>%s</strong>,</p>
                            <p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta. Ingresa el siguiente código de seguridad:</p>
                            
                            <div style="background-color: #f4f4f7; padding: 25px; text-align: center; border-radius: 8px; margin: 30px 0; border: 1px dashed #cccccc;">
                                <span style="font-size: 38px; font-weight: 900; letter-spacing: 8px; color: #b30000; font-family: monospace;">%s</span>
                            </div>
                            
                            <p style="color: #666; font-size: 13px;">Este código es válido únicamente por <strong>15 minutos</strong>.</p>
                            <p style="color: #666; font-size: 13px;">Si no solicitaste este cambio, por favor ignora este correo.</p>
                        </div>
                        <div class="email-footer">&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</div>
                    </div>
                </body></html>
                """, CSS_BASE, nombreUsuario, codigoCrudo);

            helper.setText(htmlContent, true);
            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el código de recuperación", e);
        }
    }

    // 6. Recordatorio de Pago
    @Async
    public void enviarRecordatorioPago(String correoDestino, String nombreEstudiante, String concepto, String fechaLimite, String monto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("Códice SRA - Recordatorio de Vencimiento de Cuota");

            String htmlContent = String.format("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"><style>%s</style></head><body>
                    <div class="email-container">
                        <div class="email-header"><img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA"></div>
                        <div class="email-body">
                            <h2>Recordatorio de Pago</h2>
                            <p>Hola <strong>%s</strong>,</p>
                            <p>Te recordamos que tu arancel correspondiente a <strong>%s</strong> está próximo a vencer.</p>
                            
                            <div class="highlight-box">
                                <p style="margin: 0 0 10px 0; font-size: 16px;"><strong>Monto a cancelar:</strong> <span style="color: #b30000; font-weight: bold;">$%s</span></p>
                                <p style="margin: 0;"><strong>Fecha límite sin mora:</strong> %s</p>
                            </div>
                            
                            <p style="font-size: 13px; color: #666;">Para evitar recargos por mora ($5.00), te invitamos a realizar tu pago en la ventanilla financiera antes de la fecha indicada.</p>
                        </div>
                        <div class="email-footer">&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</div>
                    </div>
                </body></html>
                """, CSS_BASE, nombreEstudiante, concepto, monto, fechaLimite);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Recordatorio de pago enviado a {}", correoDestino);
        } catch (Exception e) {
            log.error("Error al enviar recordatorio de pago a {}: {}", correoDestino, e.getMessage());
        }
    }

    // 7. Notificación de Nota
    @Async
    public void enviarNotificacionNota(String correoDestino, String nombreEstudiante, String materia, String evaluacion, String nota) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("Códice SRA - Nueva Calificación Publicada");

            double notaValor = 0.0;
            try { notaValor = Double.parseDouble(nota); } catch (Exception e) {}

            String colorNota = notaValor >= 6.0 ? "#15803d" : "#b30000";
            String feedback = notaValor >= 6.0 ? "¡Sigue así con tu buen rendimiento!" : "Te animamos a esforzarte más en las próximas evaluaciones.";

            String htmlContent = String.format("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"><style>%s</style></head><body>
                    <div class="email-container">
                        <div class="email-header"><img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA"></div>
                        <div class="email-body">
                            <h2>Calificación Publicada</h2>
                            <p>Hola <strong>%s</strong>,</p>
                            <p>Tu catedrático ha publicado oficialmente una nueva nota en el sistema para la materia de <strong>%s</strong>.</p>
                            
                            <div style="text-align: center; margin: 35px 0;">
                                <p style="font-size: 13px; color: #666; text-transform: uppercase; font-weight: bold; margin-bottom: 10px;">%s</p>
                                <span style="font-size: 45px; font-weight: 900; color: %s; border: 3px solid %s; padding: 15px 40px; border-radius: 12px; display: inline-block;">%s</span>
                            </div>
                            
                            <p style="text-align: center; font-style: italic; color: #555;">%s</p>
                            <p style="font-size: 13px; color: #888; text-align: center; margin-top: 30px; border-top: 1px solid #eee; padding-top: 15px;">Ingresa a tu portal estudiantil para ver el detalle de tu rendimiento.</p>
                        </div>
                        <div class="email-footer">&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</div>
                    </div>
                </body></html>
                """, CSS_BASE, nombreEstudiante, materia, evaluacion, colorNota, colorNota, nota, feedback);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Notificación de nota enviada a {}", correoDestino);
        } catch (Exception e) {
            log.error("Error al enviar notificación de nota a {}: {}", correoDestino, e.getMessage());
        }
    }

    // 8. Recordatorio a Docentes (Cierre de Actas)
    @Async
    public void enviarRecordatorioDocenteNotas(String correoDestino, String nombreDocente, String materia, String evaluacion, String fechaLimite, int notasBorrador) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(correoDestino);
            helper.setSubject("⚠️ Códice SRA - Cierre de Actas Próximo");

            String htmlContent = String.format("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"><style>%s</style></head><body>
                    <div class="email-container">
                        <div class="email-header"><img src="https://www.uma.edu.sv/regionales/san-miguel/assets/logo25.png" alt="Escudo UMA"></div>
                        <div class="email-body">
                            <h2>Cierre de Actas Próximo</h2>
                            <p>Estimado/a docente <strong>%s</strong>,</p>
                            <p>El sistema le notifica que se acerca la fecha límite para el cierre de la evaluación <strong>%s</strong> en la materia de <strong>%s</strong>.</p>
                            
                            <div class="highlight-box" style="border-left-color: #111111;">
                                <p style="margin: 0 0 10px 0;"><strong>Fecha límite de publicación:</strong> %s</p>
                                <p style="margin: 0; color: #b30000; font-weight: bold;">Notas pendientes (Borrador): %d registros</p>
                            </div>
                            
                            <p>Recuerde ingresar al portal docente y dar clic en <strong>"Publicar Activas"</strong>. Las notas en estado "Borrador" no son visibles para los estudiantes ni válidas para el registro académico.</p>
                        </div>
                        <div class="email-footer">&copy; 2026 Universidad Modular Abierta (UMA). Todos los derechos reservados.</div>
                    </div>
                </body></html>
                """, CSS_BASE, nombreDocente, evaluacion, materia, fechaLimite, notasBorrador);

            helper.setText(htmlContent, true);
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