package com.dentia.controller.interno;

import com.dentia.security.CustomAuthorizationService;
import com.dentia.service.interno.ClinicaInternaService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClinicaSecurityTest {
    @Test void usuarioSinPermisosNoConsultaExpedienteNiPacientes() {
        var service=mock(ClinicaInternaService.class);var permisos=mock(CustomAuthorizationService.class);
        var controller=new ClinicaInternaController(service,permisos);
        var auth=new UsernamePasswordAuthenticationToken("empleado","",List.of());
        assertThrows(ResponseStatusException.class,()->controller.cargarExpediente(1,auth));
        assertThrows(ResponseStatusException.class,()->controller.buscarPacientes("",auth));
        assertThrows(ResponseStatusException.class,()->controller.detallePlan(1,auth));
        verifyNoInteractions(service);
    }
    @Test void permisoDeOdontogramaNoAutorizaModificarHistoriaOPresupuestos() {
        var service=mock(ClinicaInternaService.class);var permisos=mock(CustomAuthorizationService.class);
        var controller=new ClinicaInternaController(service,permisos);
        var auth=new UsernamePasswordAuthenticationToken("odontograma","",List.of());
        when(permisos.tieneAcceso(auth,"odontograma")).thenReturn(true);
        assertThrows(ResponseStatusException.class,()->controller.guardarHistoria(null,auth));
        assertThrows(ResponseStatusException.class,()->controller.generarPresupuesto(1,auth));
        verifyNoInteractions(service);
    }
    @Test void permiso2dNoAutorizaConfiguracion3d() {
        var service=mock(ClinicaInternaService.class);var permisos=mock(CustomAuthorizationService.class);
        var controller=new ClinicaInternaController(service,permisos);
        var auth=new UsernamePasswordAuthenticationToken("dentista","",List.of());
        when(permisos.tieneAcceso(auth,"odontograma")).thenReturn(true);
        when(permisos.tieneAcceso(auth,"odontograma_2d")).thenReturn(true);
        assertThrows(ResponseStatusException.class,()->controller.configurarOdontograma(null,auth));
        verifyNoInteractions(service);
    }
}
