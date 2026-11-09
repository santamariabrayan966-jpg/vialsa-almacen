package com.dentia.security;

import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.interno.Usuario;
import com.dentia.service.interno.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsuarioPasswordSecurityTest {
    @Test
    void editarPerfilNoVuelveACifrarElHashExistente() {
        var dao = mock(UsuarioDao.class);
        var encoder = new BCryptPasswordEncoder(4);
        var servicio = new UsuarioService(dao, encoder);
        var actual = usuario(encoder.encode("Anterior123!"));
        when(dao.obtenerPorId(9)).thenReturn(actual);
        when(dao.obtenerPorNombre("doctor")).thenReturn(Optional.empty());
        when(dao.findByCorreo("doctor@example.com")).thenReturn(Optional.empty());

        servicio.actualizarPerfil(actual);

        var guardado = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(dao).actualizarPerfil(guardado.capture());
        assertNull(guardado.getValue().getContrasena());
        verify(dao, never()).actualizarContrasena(anyInt(), anyString());
    }

    @Test
    void cambiarClavePropiaExigeLaClaveActualYGuardaBcrypt() {
        var dao = mock(UsuarioDao.class);
        var encoder = new BCryptPasswordEncoder(4);
        var servicio = new UsuarioService(dao, encoder);
        when(dao.obtenerPorId(9)).thenReturn(usuario(encoder.encode("Anterior123!")));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarContrasenaPropia(9, "incorrecta", "Nueva123!"));
        verify(dao, never()).actualizarContrasena(anyInt(), anyString());

        servicio.cambiarContrasenaPropia(9, "Anterior123!", "Nueva123!");
        var hash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(dao).actualizarContrasena(eq(9), hash.capture());
        assertTrue(encoder.matches("Nueva123!", hash.getValue()));
    }

    private Usuario usuario(String hash) {
        var usuario = new Usuario();
        usuario.setIdUsuario(9);
        usuario.setNombreUsuario("doctor");
        usuario.setNombres("Ana");
        usuario.setApellidos("Ramos");
        usuario.setIdRol(1);
        usuario.setIdEstadoUsuario(1);
        usuario.setCorreo("doctor@example.com");
        usuario.setContrasena(hash);
        return usuario;
    }
}
