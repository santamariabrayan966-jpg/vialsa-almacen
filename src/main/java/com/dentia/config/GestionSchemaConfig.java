package com.dentia.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Inicialización explícita, idempotente y exclusivamente aditiva de Gestión. */
@Configuration
public class GestionSchemaConfig {
    @Bean
    @ConditionalOnProperty(name = "dentia.gestion.initialize-schema", havingValue = "true")
    ApplicationRunner inicializarGestion(DataSource dataSource) {
        return args -> new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/dentia/V2026_09_03_01__gestion_interna.sql")).execute(dataSource);
    }
}
