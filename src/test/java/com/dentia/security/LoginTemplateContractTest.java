package com.dentia.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginTemplateContractTest {

    @Test
    void loginPacienteUsaOAuthSinFormularioLocalObsoleto() throws IOException {
        String html = resource("templates/paciente/login.html");

        assertTrue(html.contains("th:href=\"@{/oauth2/authorization/google}\""));
        assertFalse(html.contains("th:action=\"@{/paciente/login}\""));
        assertFalse(html.contains("name=\"password\""));
        assertFalse(html.contains("login-google-demo"));
        assertTrue(html.contains("Portal del paciente"));
        assertTrue(html.contains("Ingresa con la cuenta que usarás para gestionar tus citas."));
        assertTrue(html.contains("No solicitamos tu contraseña de Google en DentIA."));
        assertTrue(html.contains("No se pudo completar el acceso con Google."));
        assertTrue(html.contains("Tu cuenta se encuentra inactiva. Comunícate con el consultorio."));
        assertTrue(html.contains("Tu cuenta está temporalmente bloqueada. Inténtalo nuevamente más tarde."));
    }

    @Test
    void portalInternoConservaControlAccesibleDePassword() throws IOException {
        String interno = resource("templates/auth/login-empleados.html");

        assertTrue(interno.contains("data-password-toggle=\"password\""));
        assertTrue(interno.contains("aria-label=\"Mostrar contraseña\""));
        assertTrue(interno.contains("<h1>Iniciar sesión</h1>"));
        assertFalse(interno.contains("login-visual"));
        assertFalse(interno.contains("Tus credenciales se transmiten de forma protegida"));
        assertTrue(interno.contains("rel=\"manifest\""));
        assertTrue(interno.contains("pwa-install.js"));
        assertTrue(interno.contains("<span>Recordarme</span>"));
        assertTrue(interno.contains("name=\"remember-device\""));
        assertFalse(interno.contains("name=\"remember-me\""));
        assertFalse(interno.contains("class=\"remember-help\""));
        assertTrue(interno.contains("class=\"brand-name\""));
        assertTrue(interno.contains("id=\"dentiaReadingToggle\""));
        assertTrue(resource("static/css/dentia/login.css").contains("dentia-clinic-login.png"));
        assertTrue(getClass().getClassLoader().getResource("static/img/dentia-clinic-login.png") != null);
        assertTrue(interno.contains("logoPrincipalUrl"));
    }

    @Test
    void panelTieneModoLecturaYBarraMovilPorPermisos() throws IOException {
        String header = resource("templates/fragments/interno/header.html");
        String css = resource("static/css/dentia/panel.css");

        assertTrue(header.contains("id=\"dentiaReadingToggle\""));
        assertTrue(header.contains("dentia-mobile-dock"));
        assertTrue(header.contains("id=\"dentiaMobileDock\""));
        assertTrue(header.contains("querySelectorAll('#dentiaSidebar .sidebar-nav .sidebar-link')"));
        assertFalse(header.contains("id=\"sidebarToggleBtn\""));
        assertTrue(header.contains("@{/interno/perfil}"));
        assertTrue(css.contains("data-dentia-reading-mode=\"large\""));
        assertTrue(css.contains("overflow-x: auto"));
        assertTrue(header.contains("dentia-reading-scale"));
        assertFalse(header.contains("id=\"dentiaReadingSlider\""));
        assertFalse(header.contains("reading-size-control"));
        assertTrue(header.contains("class=\"reading-control\""));
        assertFalse(header.contains("event.altKey || event.ctrlKey"));
        assertFalse(header.contains("mobile-dock-link\" th:href=\"@{/interno/perfil}"));
        assertTrue(header.contains("mobileDock?.appendChild(item)"));
    }

    @Test
    void perfilMuestraFotoSinPortadaYPermiteCambiarla() throws IOException {
        String perfil = resource("templates/interno/usuarios/perfil-propio.html");
        assertFalse(perfil.contains("profile-cover"));
        assertTrue(perfil.contains("profile-avatar-wrapper"));
        assertTrue(perfil.contains("for=\"fotoFileInput\""));
        assertTrue(perfil.contains("id=\"fotoFileInput\""));
    }

    @Test
    void paginaPublicaNoOfreceAccesoSimulado() throws IOException {
        String html = resource("templates/paciente/landing.html");
        assertFalse(html.contains("login-google-demo"));
        assertFalse(html.contains("Google Demo"));
        assertFalse(html.contains("Dev Mode"));
        assertTrue(html.contains("th:href=\"@{/oauth2/authorization/google}\""));
        assertTrue(html.contains("googleOAuthDisponible"));
        assertTrue(html.contains("La reserva en línea está temporalmente deshabilitada"));
        assertTrue(html.contains("El acceso con Google todavía no está configurado"));
    }

    @Test
    void loginRecuerdaUsuarioPeroNoGuardaPasswordEnAlmacenamientoWeb() throws IOException {
        String javascript = resource("static/js/dentia/login.js");
        String landing = resource("static/js/dentia/landing.js");

        assertTrue(javascript.contains("localStorage.setItem(rememberedUsernameKey, username.value.trim())"));
        assertTrue(javascript.contains("localStorage.removeItem(\"dentia_staff_pass\")"));
        assertFalse(javascript.contains("localStorage.setItem(\"dentia_staff_pass\""));
        assertFalse(landing.contains("localStorage.setItem('dentia_staff_pass'"));
        assertFalse(landing.contains("localStorage.getItem('dentia_staff_pass'"));
        assertTrue(landing.contains("localStorage.removeItem('dentia_staff_pass')"));
    }

    private String resource(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            if (input == null) throw new IOException("Recurso no encontrado: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

