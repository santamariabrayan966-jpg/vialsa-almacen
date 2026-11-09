package com.dentia.controller.interno;

import com.dentia.model.interno.Usuario;
import com.dentia.service.interno.RolService;
import com.dentia.service.interno.UsuarioService;
import com.dentia.service.shared.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsuarioControllerErrorTest {

    @Test
    void nuncaMuestraSqlEnElMensajeDeError() {
        UsuarioService usuarios = mock(UsuarioService.class);
        Usuario original = new Usuario();
        original.setIdUsuario(7);
        original.setContrasena("hash-anterior");
        when(usuarios.buscarPorId(7)).thenReturn(original);
        doThrow(new DataIntegrityViolationException("PreparedStatementCallback; SQL UPDATE usuarios ... Duplicate entry"))
                .when(usuarios).actualizarUsuarioInterno(any(Usuario.class), anyString(), eq("hash-anterior"));

        Usuario cambio = new Usuario();
        cambio.setIdUsuario(7);
        RedirectAttributesModelMap mensajes = new RedirectAttributesModelMap();
        String destino = new UsuarioController(usuarios, mock(RolService.class), mock(FileStorageService.class))
                .guardar(cambio, null, false, "Nueva123!", mensajes);

        assertEquals("redirect:/interno/usuarios", destino);
        String error = String.valueOf(mensajes.getFlashAttributes().get("error"));
        assertTrue(error.contains("ya está registrado"));
        assertFalse(error.contains("SQL"));
        assertFalse(error.contains("PreparedStatement"));
    }
}
