package com.dentia.security;

import com.dentia.dao.interfaces.UsuarioDao;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginFailureHandlerTest {

    @Test
    void pacienteAccedeConGoogleDesdeLaPaginaPrincipal() {
        var controller = new com.dentia.controller.paciente.PatientAuthController(
                new GoogleOAuthAvailability("not-configured", ""));
        assertEquals("redirect:/?loginGoogle", controller.login(null));
        assertEquals("redirect:/?soloGoogle", controller.registro());
    }

    @Test
    void internoNoExponeIntentosRestantes() throws Exception {
        UsuarioDao usuarioDao = mock(UsuarioDao.class);
        when(usuarioDao.obtenerIntentosFallidos("recepcion")).thenReturn(2);
        InternalSecurityConfig config = new InternalSecurityConfig(
                mock(CustomAuthorizationService.class), mock(CustomSuccessHandler.class), usuarioDao);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "recepcion");
        MockHttpServletResponse response = new MockHttpServletResponse();

        config.authenticationFailureHandler().onAuthenticationFailure(
                request, response, new BadCredentialsException("credenciales"));

        assertEquals("/interno/login?error", response.getRedirectedUrl());
        verify(usuarioDao).incrementarIntentosFallidos("recepcion");
    }

    @Test
    void internoInactivoNoIncrementaIntentos() throws Exception {
        UsuarioDao usuarioDao = mock(UsuarioDao.class);
        InternalSecurityConfig config = new InternalSecurityConfig(
                mock(CustomAuthorizationService.class), mock(CustomSuccessHandler.class), usuarioDao);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "recepcion");
        MockHttpServletResponse response = new MockHttpServletResponse();

        config.authenticationFailureHandler().onAuthenticationFailure(
                request, response, new DisabledException("USUARIO_INACTIVO"));

        assertEquals("/interno/login?disabled", response.getRedirectedUrl());
        verify(usuarioDao, never()).incrementarIntentosFallidos("recepcion");
    }
    @Test void usuarioInexistenteVuelveAlFormularioConError() throws Exception {
        UsuarioDao dao = mock(UsuarioDao.class);
        var config = new InternalSecurityConfig(mock(CustomAuthorizationService.class), mock(CustomSuccessHandler.class), dao);
        var request = new MockHttpServletRequest(); request.setParameter("username","no_existe");
        var response = new MockHttpServletResponse();
        config.authenticationFailureHandler().onAuthenticationFailure(request,response,new BadCredentialsException("inválido"));
        assertEquals("/interno/login?error",response.getRedirectedUrl());
    }
}
