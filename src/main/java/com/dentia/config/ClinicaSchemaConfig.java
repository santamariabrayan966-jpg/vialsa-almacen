package com.dentia.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Inicialización segura, idempotente y aditiva del esquema del Área Clínica de DentIA.
 */
@Configuration
@ConditionalOnProperty(name = "dentia.clinica.initialize-schema", havingValue = "true", matchIfMissing = true)
public class ClinicaSchemaConfig {

    private static final Logger log = LoggerFactory.getLogger(ClinicaSchemaConfig.class);

    @Bean
    ApplicationRunner inicializarAreaClinica(DataSource dataSource) {
        return args -> {
            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/dentia/V2026_09_03_02__area_clinica.sql"),
                        new ClassPathResource("db/migration/dentia/V2026_09_14_01__odontograma_versionado.sql"),
                        new ClassPathResource("db/migration/dentia/V2026_09_16_01__atenciones_idempotentes.sql"),
                        new ClassPathResource("db/migration/dentia/V2026_09_20_01__paciente_direccion_celular_opcional.sql"),
                        new ClassPathResource("db/migration/dentia/V2026_09_20_02__usuario_direccion.sql"),
                        new ClassPathResource("db/migration/dentia/V2026_09_21_01__catalogo_vasquez_referencia.sql"),
                        new ClassPathResource("db/migration/dentia/V2026_09_21_02__servicios_clasificados.sql"),
                        new ClassPathResource("db/migration/dentia/V2026_10_24_01__permisos_vistas_odontograma.sql")
                );
                populator.setContinueOnError(false);
                populator.setSqlScriptEncoding("UTF-8");
                populator.execute(dataSource);

                log.info("Esquema del Área Clínica de DentIA inicializado correctamente.");
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo inicializar el esquema clínico de DentIA.", e);
            }
        };
    }
}
