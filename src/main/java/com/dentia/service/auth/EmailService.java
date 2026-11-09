package com.dentia.service.auth;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreo(String para, String asunto, String contenidoHtml) {

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(para);
            helper.setSubject(asunto);

            helper.setText(contenidoHtml, true); // true = HTML

            mailSender.send(mensaje);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    // OPCIONAL: prueba simple
    public void enviarPrueba(String destino) {

        String html = """
                <h2>Correo de prueba</h2>
                <p>Este mensaje confirma que el servicio de correo funciona correctamente.</p>
                """;

        enviarCorreo(destino, "Prueba exitosa", html);
    }
}
