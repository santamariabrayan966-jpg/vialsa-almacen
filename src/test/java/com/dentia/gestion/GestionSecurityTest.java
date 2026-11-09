package com.dentia.gestion;

import com.dentia.controller.interno.GestionInternaController;
import com.dentia.security.CustomAuthorizationService;
import com.dentia.service.interno.GestionInternaService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionSecurityTest {
    @Test void deniegaCadaModuloYSusDatosYArchivosSinPermiso(){
        var service=mock(GestionInternaService.class);var permisos=mock(CustomAuthorizationService.class);
        var controller=new GestionInternaController(service,permisos);
        var auth=new UsernamePasswordAuthenticationToken("recepcion","",List.of(new SimpleGrantedAuthority("ROLE_RECEPCION")));
        for(String modulo:GestionInternaService.MODULOS){
            assertThrows(ResponseStatusException.class,()->controller.pagina(modulo,auth,new ExtendedModelMap()));
            assertThrows(ResponseStatusException.class,()->controller.datos(modulo,auth,null,null,null,null,null,0));
            assertThrows(ResponseStatusException.class,()->controller.opciones(modulo,auth,1L,null));
        }
        assertThrows(ResponseStatusException.class,()->controller.descargarDocumento(1,auth));
        assertThrows(ResponseStatusException.class,()->controller.crearPago(null,auth));
        assertThrows(ResponseStatusException.class,()->controller.estadoDocumento(1,null,auth));
        verifyNoInteractions(service);
    }
    @Test void permisoDeDocumentosNoDaAccesoAPagos(){
        var service=mock(GestionInternaService.class);var permisos=mock(CustomAuthorizationService.class);
        var controller=new GestionInternaController(service,permisos);
        var auth=new UsernamePasswordAuthenticationToken("archivo","",List.of(new SimpleGrantedAuthority("ROLE_ARCHIVO")));
        when(permisos.tieneAcceso(auth,"documentos")).thenReturn(true);
        assertEquals("interno/gestion/index",controller.pagina("documentos",auth,new ExtendedModelMap()));
        assertThrows(ResponseStatusException.class,()->controller.crearPago(null,auth));
        verifyNoInteractions(service);
    }
}
