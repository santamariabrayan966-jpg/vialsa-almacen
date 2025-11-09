package com.vialsa.almacen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Ruta física donde guardamos: uploads/usuarios
        Path uploadDir = Paths.get("uploads/usuarios");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Cuando se pida /uploads/usuarios/** se mapea a esa carpeta
        registry.addResourceHandler("/uploads/usuarios/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
