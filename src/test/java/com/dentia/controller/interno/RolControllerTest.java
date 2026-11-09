package com.dentia.controller.interno;

import com.dentia.model.interno.Rol;
import com.dentia.service.interno.PermisoService;
import com.dentia.service.interno.RolService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RolControllerTest {
    @Test void idRolVacioDelModalSeTrataComoNuevoSinAutounboxing() {
        RolService roles = mock(RolService.class);
        RolController controller = new RolController(roles, mock(PermisoService.class));
        Rol nuevo = new Rol(); nuevo.setIdRol(null); nuevo.setNombreRol("RECEPCIONISTA");

        String resultado = controller.guardar(nuevo);

        assertEquals("redirect:/interno/roles?guardado", resultado);
        verify(roles).crearRol(nuevo);
        verify(roles, never()).actualizarRol(any());
    }
}
