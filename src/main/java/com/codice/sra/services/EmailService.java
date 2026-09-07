package com.codice.sra.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
}