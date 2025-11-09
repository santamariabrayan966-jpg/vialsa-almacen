package com.vialsa.almacen.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    // Carpeta donde se guardarán las fotos de usuarios
    // Puedes ponerlo también en application.properties como:
    // upload.usuarios.dir=uploads/usuarios
    @Value("${upload.usuarios.dir:uploads/usuarios}")
    private String uploadDir;

    public String guardarFotoUsuario(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // Creamos la carpeta si no existe
            Path directorio = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directorio);

            // Nombre único: UUID + extensión original
            String extension = "";
            String nombreOriginal = file.getOriginalFilename();
            if (nombreOriginal != null && nombreOriginal.contains(".")) {
                extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            }

            String nombreArchivo = UUID.randomUUID().toString() + extension;

            Path destino = directorio.resolve(nombreArchivo);

            // Guardar el archivo
            Files.copy(file.getInputStream(), destino);

            // Devolvemos solo el nombre, que irá en la columna foto
            return nombreArchivo;

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la foto del usuario", e);
        }
    }
}
