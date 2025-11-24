package com.vialsa.almacen.service;

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

    public void enviarPasswordGoogle(String correo, String passwordGenerada) {

        String html = """
            <h2>Bienvenido a VIALSA</h2>
            <p>Tu cuenta fue creada automáticamente mediante Google.</p>
            <p><b>Tu contraseña temporal es:</b></p>
            <h3 style="color:#d9534f;">""" + passwordGenerada + "</h3>" + """
            <p>Inicia sesión en la tienda y cámbiala por una nueva contraseña.</p>
            <br>
            <p>VIALSA</p>
            """;

        enviarCorreo(correo, "Tu acceso a VIALSA", html);
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
