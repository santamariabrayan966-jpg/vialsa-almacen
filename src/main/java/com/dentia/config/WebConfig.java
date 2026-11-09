package com.dentia.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path uploadsRoot = Paths.get(System.getProperty("user.dir"), "uploads")
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(uploadsRoot.resolve("clientes"));
            Files.createDirectories(uploadsRoot.resolve("productos"));
            Files.createDirectories(uploadsRoot.resolve("usuarios"));
            Files.createDirectories(uploadsRoot.resolve("profesionales"));
            Files.createDirectories(uploadsRoot.resolve("sedes"));
            Files.createDirectories(uploadsRoot.resolve("ajustes"));
            Files.createDirectories(uploadsRoot.resolve("traslados"));
            Files.createDirectories(uploadsRoot.resolve("marca"));

        } catch (Exception e) {
            throw new RuntimeException("No se pudieron crear las carpetas de uploads.", e);
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsRoot.toUri().toString());
    }
}
