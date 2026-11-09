package com.dentia.service.auth;

import com.dentia.dao.interfaces.PasswordResetTokenDao;
import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.auth.PasswordResetToken;
import com.dentia.model.interno.Usuario;
import com.dentia.service.interno.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(15);

    private final UsuarioDao usuarioDao;
    private final PasswordResetTokenDao tokenDao;
    private final JavaMailSender mailSender;
    private final UsuarioService usuarioService;
    private final byte[] signingKey;
    private final String mailFromAddress;
    private final ConcurrentHashMap<String, Window> limits = new ConcurrentHashMap<>();

    public PasswordResetService(UsuarioDao usuarioDao, PasswordResetTokenDao tokenDao,
                                JavaMailSender mailSender, UsuarioService usuarioService,
                                @Value("${dentia.security.password-reset-key:}") String configuredKey,
                                @Value("${dentia.mail.from-address:${spring.mail.username:}}") String mailFromAddress) {
        this.usuarioDao = usuarioDao;
        this.tokenDao = tokenDao;
        this.mailSender = mailSender;
        this.usuarioService = usuarioService;
        this.mailFromAddress = mailFromAddress == null ? "" : mailFromAddress.trim();
        if (configuredKey == null || configuredKey.isBlank()) {
            this.signingKey = new byte[32];
            RANDOM.nextBytes(this.signingKey);
            log.warn("DENTIA_PASSWORD_RESET_KEY no configurada; los codigos caducaran al reiniciar.");
        } else {
            if (configuredKey.length() < 32) {
                throw new IllegalStateException("DENTIA_PASSWORD_RESET_KEY debe tener al menos 32 caracteres.");
            }
            this.signingKey = configuredKey.getBytes(StandardCharsets.UTF_8);
        }
    }

    public void enviarCorreoRecuperacion(String correo, String ip) {
        if (correo == null || correo.isBlank() || correo.length() > 254) return;
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (!permitir("solicitud-ip:" + ip, 20, Duration.ofHours(1))
                || !permitir("solicitud-correo:" + normalizado, 3, Duration.ofHours(1)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes de recuperación.");

        Usuario usuario = usuarioDao.findByCorreo(normalizado).orElse(null);
        if (usuario == null || usuario.getIdRol() == 8 || !Integer.valueOf(1).equals(usuario.getIdEstadoUsuario())) return;
        if (mailFromAddress.isBlank()) {
            throw new IllegalStateException("El envío de correos de recuperación no está configurado.");
        }

        String codigo = null;
        PasswordResetToken token = new PasswordResetToken();
        token.setIdUsuario(usuario.getIdUsuario());
        token.setFechaCreacion(LocalDateTime.now());
        token.setFechaExpiracion(LocalDateTime.now().plus(CODE_LIFETIME));
        token.setUsado(false);
        tokenDao.invalidarTokensUsuario(usuario.getIdUsuario());
        for (int intento = 0; intento < 5; intento++) {
            codigo = String.format(Locale.ROOT, "%04d", RANDOM.nextInt(10_000));
            token.setCodigo(firmar(codigo));
            try {
                tokenDao.guardarToken(token);
                break;
            } catch (DuplicateKeyException ex) {
                if (intento == 4) throw ex;
            }
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(mailFromAddress);
        mensaje.setTo(normalizado);
        mensaje.setSubject("Recuperacion de contrasena - DentIA");
        mensaje.setText("Tu codigo de recuperacion es: " + codigo
                + "\nExpira en 15 minutos. Si no solicitaste este cambio, ignora el mensaje.");
        try {
            mailSender.send(mensaje);
        } catch (MailException ex) {
            tokenDao.invalidarTokensUsuario(usuario.getIdUsuario());
            log.warn("No se pudo enviar el correo de recuperacion: {}", ex.getClass().getSimpleName());
            throw ex;
        }
    }

    @Transactional
    public boolean restablecerPassword(String correo, String codigo, String nuevaPassword, String ip) {
        if (!permitirVerificacion(correo, ip) || !contrasenaSegura(nuevaPassword)) return false;
        PasswordResetToken token = tokenValido(correo, codigo);
        if (token == null || !tokenDao.consumirToken(token.getIdToken())) return false;
        usuarioService.restablecerContrasena(token.getIdUsuario(), nuevaPassword);
        return true;
    }

    public boolean codigoValido(String correo, String codigo, String ip) {
        return permitirVerificacion(correo, ip) && tokenValido(correo, codigo) != null;
    }

    private boolean permitirVerificacion(String correo, String ip) {
        if (correo == null || correo.isBlank() || correo.length() > 254) return false;
        return permitir("verificacion-ip:" + ip, 10, CODE_LIFETIME)
                && permitir("verificacion-correo:" + correo.trim().toLowerCase(Locale.ROOT), 10, CODE_LIFETIME);
    }

    private PasswordResetToken tokenValido(String correo, String codigo) {
        if (correo == null || correo.isBlank() || codigo == null || !codigo.matches("\\d{4}")) return null;
        Usuario usuario = usuarioDao.findByCorreo(correo.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (usuario == null || usuario.getIdRol() == 8 || !Integer.valueOf(1).equals(usuario.getIdEstadoUsuario())) return null;
        PasswordResetToken token = tokenDao.buscarPorCodigo(firmar(codigo)).orElse(null);
        if (token == null || Boolean.TRUE.equals(token.getUsado())
                || !token.getFechaExpiracion().isAfter(LocalDateTime.now())
                || !usuario.getIdUsuario().equals(token.getIdUsuario())) return null;
        return token;
    }

    private boolean contrasenaSegura(String valor) {
        return valor != null && valor.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,80}$");
    }

    private String firmar(String codigo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo verificar el codigo de recuperacion.", ex);
        }
    }

    private boolean permitir(String clave, int maximo, Duration plazo) {
        if (limits.size() > 10_000) {
            Instant ahora = Instant.now();
            limits.entrySet().removeIf(entry -> entry.getValue().inicio.plus(plazo).isBefore(ahora));
        }
        Instant ahora = Instant.now();
        Window ventana = limits.compute(clave, (key, actual) -> {
            if (actual == null || actual.inicio.plus(plazo).isBefore(ahora)) return new Window(ahora, 1);
            return new Window(actual.inicio, actual.intentos + 1);
        });
        return ventana.intentos <= maximo;
    }

    private record Window(Instant inicio, int intentos) {}
}
