package com.dentia.service.interno;

import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.interno.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class UsuarioCorreoRecuperacionTest {

    @Test
    void correoVacioSeGuardaComoNullYNoProvocaDuplicado() {
        UsuarioDao dao = mock(UsuarioDao.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario existente = new Usuario();
        existente.setIdUsuario(7);
        existente.setIdRol(1);
        existente.setIdEstadoUsuario(1);
        existente.setContrasena("hash-anterior");
        when(dao.obtenerPorId(7)).thenReturn(existente);
        when(dao.obtenerPorNombre("adminDentia")).thenReturn(Optional.of(existente));

        Usuario cambio = usuario(7, "adminDentia", "   ");
        new UsuarioService(dao, encoder).actualizarUsuarioInterno(cambio, "", "hash-anterior");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(dao).actualizar(captor.capture());
        assertNull(captor.getValue().getCorreo());
        assertNull(captor.getValue().getCorreoRecuperacion());
        assertEquals("hash-anterior", captor.getValue().getContrasena());
    }

    @Test
    void unSoloCorreoSirveParaCuentaYRecuperacion() {
        UsuarioDao dao = mock(UsuarioDao.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("Segura123!")).thenReturn("hash-nuevo");

        Usuario nuevo = usuario(null, "usuarioNuevo", "  Persona@Example.com  ");
        nuevo.setContrasena("Segura123!");
        new UsuarioService(dao, encoder).guardarUsuarioInterno(nuevo);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(dao).crear(captor.capture());
        assertEquals("persona@example.com", captor.getValue().getCorreo());
        assertEquals("persona@example.com", captor.getValue().getCorreoRecuperacion());
    }

    private Usuario usuario(Integer id, String nombreUsuario, String correo) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setNombres("Nombre");
        usuario.setApellidos("Apellido");
        usuario.setIdRol(1);
        usuario.setCorreo(correo);
        return usuario;
    }
}
