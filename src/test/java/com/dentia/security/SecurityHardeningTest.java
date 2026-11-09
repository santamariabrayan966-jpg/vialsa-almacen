package com.dentia.security;

import com.dentia.dao.interfaces.PasswordResetTokenDao;
import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.auth.PasswordResetToken;
import com.dentia.model.interno.Usuario;
import com.dentia.service.auth.PasswordResetService;
import com.dentia.service.interno.UsuarioService;
import com.dentia.service.shared.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecurityHardeningTest {

    @Test
    void rechazaImagenDisfrazadaYAdmitePngReal() throws Exception {
        var falso = new MockMultipartFile("foto", "logo.svg", "image/png",
                "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> FileStorageService.extensionPermitida(falso));

        var bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes));
        var real = new MockMultipartFile("foto", "cualquier.html", "image/png", bytes.toByteArray());
        assertEquals(".png", FileStorageService.extensionPermitida(real));
        assertThrows(IllegalArgumentException.class, () -> FileStorageService.extensionPermitida(
                new MockMultipartFile("foto", "grande.png", "image/png", new byte[5 * 1024 * 1024 + 1])));
    }

    @Test
    void limitaIntentosDeLoginPorIp() throws Exception {
        var filtro = new LoginRateLimitFilter();
        for (int i = 0; i < 30; i++) {
            var request = new MockHttpServletRequest("POST", "/interno/login");
            request.setServletPath("/interno/login");
            request.setRemoteAddr("203.0.113.4");
            var response = new MockHttpServletResponse();
            filtro.doFilter(request, response, new MockFilterChain());
            assertNotEquals(429, response.getStatus());
        }
        var request = new MockHttpServletRequest("POST", "/interno/login");
        request.setServletPath("/interno/login");
        request.setRemoteAddr("203.0.113.4");
        var response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        assertEquals(429, response.getStatus());
        assertEquals("300", response.getHeader("Retry-After"));
    }

    @Test
    void codigoDeRecuperacionSeGuardaFirmadoYSeConsumeUnaSolaVez() {
        UsuarioDao usuarios = mock(UsuarioDao.class);
        PasswordResetTokenDao tokens = mock(PasswordResetTokenDao.class);
        JavaMailSender correo = mock(JavaMailSender.class);
        UsuarioService servicioUsuarios = mock(UsuarioService.class);
        var servicio = new PasswordResetService(usuarios, tokens, correo, servicioUsuarios,
                "clave-secreta-de-pruebas-con-mas-de-32-caracteres", "acceso@dentia.test");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(17);
        usuario.setIdRol(1);
        usuario.setIdEstadoUsuario(1);
        when(usuarios.findByCorreo("persona@example.com")).thenReturn(Optional.of(usuario));
        var token = org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);
        var mensaje = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);

        servicio.enviarCorreoRecuperacion("Persona@Example.com", "203.0.113.5");
        verify(tokens).guardarToken(token.capture());
        verify(correo).send(mensaje.capture());
        assertEquals("acceso@dentia.test", mensaje.getValue().getFrom());
        String codigo = mensaje.getValue().getText().replaceAll("(?s).*?([0-9]{4}).*", "$1");
        assertTrue(codigo.matches("[0-9]{4}"));
        assertNotEquals(codigo, token.getValue().getCodigo());
        assertEquals(64, token.getValue().getCodigo().length());

        token.getValue().setIdToken(45);
        when(tokens.buscarPorCodigo(token.getValue().getCodigo())).thenReturn(Optional.of(token.getValue()));
        when(tokens.consumirToken(45)).thenReturn(true, false);
        assertFalse(servicio.codigoValido("otro@example.com", codigo, "203.0.113.5"));
        assertTrue(servicio.codigoValido("persona@example.com", codigo, "203.0.113.5"));
        assertFalse(servicio.restablecerPassword("otro@example.com", codigo, "Fuerte123!", "203.0.113.5"));
        assertFalse(servicio.restablecerPassword("persona@example.com", codigo, "debil", "203.0.113.5"));
        assertTrue(servicio.restablecerPassword("persona@example.com", codigo, "Fuerte123!", "203.0.113.5"));
        assertFalse(servicio.restablecerPassword("persona@example.com", codigo, "Fuerte123!", "203.0.113.5"));
        verify(servicioUsuarios, times(1)).restablecerContrasena(17, "Fuerte123!");
    }
    @Test
    void limiteDeSolicitudesDevuelve429EnLugarDeSimularEnvio() {
        UsuarioDao usuarios = mock(UsuarioDao.class);
        PasswordResetTokenDao tokens = mock(PasswordResetTokenDao.class);
        JavaMailSender correo = mock(JavaMailSender.class);
        UsuarioService servicioUsuarios = mock(UsuarioService.class);
        var servicio = new PasswordResetService(usuarios, tokens, correo, servicioUsuarios,
                "clave-secreta-de-pruebas-con-mas-de-32-caracteres", "acceso@dentia.test");
        Usuario usuario = new Usuario(); usuario.setIdUsuario(17); usuario.setIdRol(1); usuario.setIdEstadoUsuario(1);
        when(usuarios.findByCorreo("persona@example.com")).thenReturn(Optional.of(usuario));
        for (int i = 0; i < 3; i++) servicio.enviarCorreoRecuperacion("persona@example.com", "203.0.113.5");
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> servicio.enviarCorreoRecuperacion("persona@example.com", "203.0.113.5"));
        assertEquals(429, error.getStatusCode().value());
        verify(correo, times(3)).send(any(SimpleMailMessage.class));
    }

}
