package com.dentia.config;

import com.dentia.dao.interfaces.UsuarioDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class StartupConfig {

    private static final Logger log = LoggerFactory.getLogger(StartupConfig.class);

    @Bean
    CommandLineRunner initAdmin(
            UsuarioDao usuarioDao,
            PasswordEncoder passwordEncoder,
            @Value("${dentia.bootstrap-admin.enabled:false}") boolean enabled,
            @Value("${dentia.bootstrap-admin.username:}") String username,
            @Value("${dentia.bootstrap-admin.password:}") String password
    ) {
        return args -> {
            if (!enabled) {
                log.info("Inicializacion del primer administrador de DentIA desactivada.");
                return;
            }

            String usuarioSeguro = username == null ? "" : username.trim();
            if (usuarioSeguro.isBlank() || password == null || password.length() < 12) {
                throw new IllegalStateException(
                        "Para inicializar el administrador define DENTIA_BOOTSTRAP_ADMIN_USERNAME "
                                + "y una DENTIA_BOOTSTRAP_ADMIN_PASSWORD de al menos 12 caracteres."
                );
            }

            boolean creado = usuarioDao.ensureAdminUserExists(
                    usuarioSeguro,
                    passwordEncoder.encode(password)
            );
            if (creado) {
                log.info("Primer administrador de DentIA creado de forma segura.");
            } else {
                log.info("El administrador configurado ya existe; no se modifico su contrasena.");
            }
        };
    }
}
