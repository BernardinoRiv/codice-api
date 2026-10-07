package com.codice.sra.services;

import com.codice.sra.dtos.AlertaSeguridadDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;

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
}