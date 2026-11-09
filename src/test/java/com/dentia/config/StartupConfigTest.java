package com.dentia.config;

import com.dentia.dao.interfaces.UsuarioDao;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StartupConfigTest {

    private final UsuarioDao usuarioDao = mock(UsuarioDao.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final StartupConfig startupConfig = new StartupConfig();

    @Test
    void noCreaAdministradorCuandoBootstrapEstaDesactivado() throws Exception {
        CommandLineRunner runner = startupConfig.initAdmin(
                usuarioDao, passwordEncoder, false, "admin-seguro", "clave-muy-segura"
        );

        runner.run();

        verifyNoInteractions(usuarioDao, passwordEncoder);
    }

    @Test
    void creaAdministradorConHashCuandoBootstrapEstaActivado() throws Exception {
        when(passwordEncoder.encode("clave-muy-segura")).thenReturn("$2a$10$hashSeguro");
        when(usuarioDao.ensureAdminUserExists("admin-seguro", "$2a$10$hashSeguro"))
                .thenReturn(true);

        CommandLineRunner runner = startupConfig.initAdmin(
                usuarioDao, passwordEncoder, true, " admin-seguro ", "clave-muy-segura"
        );

        runner.run();

        verify(passwordEncoder).encode("clave-muy-segura");
        verify(usuarioDao).ensureAdminUserExists("admin-seguro", "$2a$10$hashSeguro");
    }

    @Test
    void rechazaBootstrapSinCredencialesSeguras() {
        CommandLineRunner runner = startupConfig.initAdmin(
                usuarioDao, passwordEncoder, true, "", "corta"
        );

        assertThrows(IllegalStateException.class, runner::run);
        verify(usuarioDao, never()).ensureAdminUserExists("", "corta");
        verifyNoInteractions(passwordEncoder);
    }
}
