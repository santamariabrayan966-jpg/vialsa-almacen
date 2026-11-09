package com.dentia.service.shared;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;

    public String guardarFotoUsuario(MultipartFile file) {
        return guardar(file, "usuarios", "USU_");
    }

    public String guardarFotoProfesional(MultipartFile file) {
        return guardar(file, "profesionales", "DOC_");
    }

    public String guardarLogoConsultorio(MultipartFile file) {
        String nombre = guardar(file, "marca", "DENTIA_LOGO_");
        return nombre == null ? null : "/uploads/marca/" + nombre;
    }

    private String guardar(MultipartFile file, String subdirectorio, String prefijo) {
        if (file == null || file.isEmpty()) return null;
        try {
            byte[] bytes = leerImagen(file);
            String extension = extensionVerificada(bytes);
            Path directorio = Paths.get(System.getProperty("user.dir"), "uploads", subdirectorio)
                    .toAbsolutePath().normalize();
            Files.createDirectories(directorio);
            String nombre = prefijo + UUID.randomUUID() + extension;
            Files.write(directorio.resolve(nombre), bytes);
            return nombre;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la imagen.", ex);
        }
    }

    public static String extensionPermitida(MultipartFile file) {
        try {
            return extensionVerificada(leerImagen(file));
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer la imagen.", ex);
        }
    }

    private static byte[] leerImagen(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecciona una imagen.");
        byte[] bytes = file.getInputStream().readNBytes(MAX_BYTES + 1);
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 5 MB.");
        }
        return bytes;
    }

    private static String extensionVerificada(byte[] bytes) throws IOException {
        String extension;
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P'
                && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 13
                && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) {
            extension = ".png";
        } else if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            extension = ".jpg";
        } else if (bytes.length >= 20 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P'
                && bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8'
                && (bytes[15] == ' ' || bytes[15] == 'L' || bytes[15] == 'X')) {
            return ".webp";
        } else {
            throw new IllegalArgumentException("Solo se permiten imagenes PNG, JPEG o WebP validas.");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Imagen no reconocida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                int ancho = reader.getWidth(0);
                int alto = reader.getHeight(0);
                if (ancho < 1 || alto < 1 || ancho > MAX_DIMENSION || alto > MAX_DIMENSION) {
                    throw new IllegalArgumentException("Dimensiones de imagen no permitidas.");
                }
                if (!reader.getFormatName().equalsIgnoreCase(extension.equals(".png") ? "png" : "jpeg")) {
                    throw new IllegalArgumentException("El contenido de la imagen no coincide con su formato.");
                }
            } finally {
                reader.dispose();
            }
        }
        return extension;
    }
}
