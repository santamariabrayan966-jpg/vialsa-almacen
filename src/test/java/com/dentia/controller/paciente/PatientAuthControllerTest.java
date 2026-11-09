package com.dentia.controller.paciente;

import com.dentia.security.GoogleOAuthAvailability;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PatientAuthControllerTest {
    private PatientAuthController controller;
    private MockMvc mvc;

    @BeforeEach void preparar() {
        controller = new PatientAuthController(new GoogleOAuthAvailability(
                "cliente-real.apps.googleusercontent.com", "secreto-real"));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void enlaceDemoSoloRedirigeAOAuthSinCrearSesionNiAutenticacion() throws Exception {
        SecurityContextHolder.clearContext();
        var result = mvc.perform(get("/paciente/login-google-demo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/google")).andReturn();
        assertNull(result.getRequest().getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void enlaceAntiguoNoSustituyeUnaIdentidadExistente() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("personal", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        mvc.perform(get("/paciente/login-google-demo").session(session).principal(auth))
                .andExpect(redirectedUrl("/oauth2/authorization/google"));
        assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
        assertSame(context, session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test void enlaceDemoNoAdmitePost() throws Exception {
        mvc.perform(post("/paciente/login-google-demo")).andExpect(status().isMethodNotAllowed());
    }

    @Test void enlaceAntiguoNoSaleAOAuthCuandoFaltanCredenciales() throws Exception {
        MockMvc mvcSinOAuth = MockMvcBuilders.standaloneSetup(new PatientAuthController(
                new GoogleOAuthAvailability("not-configured", ""))).build();
        mvcSinOAuth.perform(get("/paciente/login-google-demo"))
                .andExpect(redirectedUrl("/?oauthNoDisponible"));
    }

    @Test void loginAnonimoSolicitaGoogleYPacienteAutenticadoVaAlPortal() throws Exception {
        mvc.perform(get("/paciente/login")).andExpect(redirectedUrl("/?loginGoogle"));
        var auth = new UsernamePasswordAuthenticationToken("paciente", null, List.of(new SimpleGrantedAuthority("ROLE_PACIENTE")));
        mvc.perform(get("/paciente/login").principal(auth)).andExpect(redirectedUrl("/paciente"));
    }

    @Test void registroNoCreaCuentaLocal() throws Exception {
        mvc.perform(get("/paciente/registro")).andExpect(redirectedUrl("/?soloGoogle"));
        mvc.perform(post("/paciente/registro")).andExpect(redirectedUrl("/?soloGoogle"));
    }
}
