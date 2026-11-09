package com.dentia.service.interno;

import com.dentia.dao.interfaces.RolDao;
import com.dentia.model.interno.Rol;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RolServiceTest {
    private final RolDao dao = mock(RolDao.class);
    private final RolService service = new RolService(dao);

    @Test void creaRolNuevoActivoYNormalizaNombre() {
        when(dao.listarRoles()).thenReturn(List.of()); when(dao.crearRol(any())).thenReturn(12);
        Rol rol = rol("  RECEPCIONISTA  ", null);
        assertEquals(12, service.crearRol(rol));
        assertEquals("RECEPCIONISTA", rol.getNombreRol()); assertTrue(rol.getActivo());
    }

    @Test void creaRolConDescripcion() {
        when(dao.listarRoles()).thenReturn(List.of()); when(dao.crearRol(any())).thenReturn(13);
        Rol rol = rol("SUPERVISOR", "  Gestion de recepcion  "); service.crearRol(rol);
        assertEquals("Gestion de recepcion", rol.getDescripcion()); verify(dao).crearRol(rol);
    }

    @Test void creaRolSinDescripcionComoNull() {
        when(dao.listarRoles()).thenReturn(List.of()); when(dao.crearRol(any())).thenReturn(14);
        Rol rol = rol("ASISTENTE", "   "); service.crearRol(rol);
        assertNull(rol.getDescripcion());
    }

    @Test void impideNombreDuplicadoSinImportarMayusculas() {
        when(dao.listarRoles()).thenReturn(List.of(rolExistente(2, "Supervisor")));
        RolValidationException ex = assertThrows(RolValidationException.class,
                () -> service.crearRol(rol(" supervisor ", null)));
        assertEquals(RolValidationException.Codigo.NOMBRE_DUPLICADO, ex.getCodigo());
        verify(dao, never()).crearRol(any());
    }

    @Test void editaRolYExcluyeSuPropioNombreDeDuplicados() {
        Rol existente = rolExistente(4, "RECEPCION");
        when(dao.obtenerPorId(4)).thenReturn(existente); when(dao.listarRoles()).thenReturn(List.of(existente));
        Rol editado = rol(" RECEPCION ", "Nueva descripcion"); editado.setIdRol(4);
        service.actualizarRol(editado);
        verify(dao).actualizarRol(editado); assertEquals("RECEPCION", editado.getNombreRol());
    }

    @Test void activaYDesactivaRolNoProtegido() {
        when(dao.obtenerPorId(5)).thenReturn(rolExistente(5, "CAJA"));
        service.cambiarEstadoActivo(5, false); service.cambiarEstadoActivo(5, true);
        verify(dao).cambiarEstadoActivo(5, false); verify(dao).cambiarEstadoActivo(5, true);
    }

    @Test void protegeAdminPorNombreYNoPorIdFijo() {
        when(dao.obtenerPorId(99)).thenReturn(rolExistente(99, "ADMIN"));
        RolValidationException ex = assertThrows(RolValidationException.class,
                () -> service.cambiarEstadoActivo(99, false));
        assertEquals(RolValidationException.Codigo.ADMIN_PROTEGIDO, ex.getCodigo());
        verify(dao, never()).cambiarEstadoActivo(anyInt(), anyBoolean());
    }

    @Test void crearRolNuevoNoConsultaNiExigeUsuariosAsociados() {
        when(dao.listarRoles()).thenReturn(List.of()); when(dao.crearRol(any())).thenReturn(15);
        assertEquals(15, service.crearRol(rol("AUDITOR", null)));
        verify(dao, never()).contarUsuariosAsociados(anyInt());
    }

    private Rol rol(String nombre, String descripcion) { Rol r = new Rol(); r.setNombreRol(nombre); r.setDescripcion(descripcion); return r; }
    private Rol rolExistente(int id, String nombre) { Rol r = rol(nombre, null); r.setIdRol(id); r.setActivo(true); return r; }
}
