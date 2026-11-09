package com.dentia.controller.auth;

import com.dentia.service.auth.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/recuperar-password")
    @ResponseBody
    public ResponseEntity<String> enviarCorreoRecuperacion(@RequestParam("correo") String correo, HttpServletRequest request) {
        try {
            passwordResetService.enviarCorreoRecuperacion(correo, request.getRemoteAddr());
            // Misma respuesta para correos existentes o desconocidos.
            return ResponseEntity.ok("OK");
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 429) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Demasiadas solicitudes. Inténtalo más tarde.");
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("El servicio de recuperacion de acceso no esta disponible: {}", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Correo no disponible temporalmente");
        }
    }

    @PostMapping("/validar-codigo-password")
    @ResponseBody
    public String validarCodigoPassword(@RequestParam("correo") String correo,
                                       @RequestParam("codigo") String codigo, HttpServletRequest request) {
        boolean valido = passwordResetService.codigoValido(correo, codigo, request.getRemoteAddr());
        return String.valueOf(valido);
    }

    @PostMapping("/restablecer-password")
    @ResponseBody
    public String restablecerPassword(
            @RequestParam("correo") String correo,
            @RequestParam("codigo") String codigo,
            @RequestParam("password") String password,
            HttpServletRequest request
    ) {
        boolean actualizado = passwordResetService.restablecerPassword(correo, codigo, password, request.getRemoteAddr());
        return actualizado ? "OK" : "ERROR";
    }
}
