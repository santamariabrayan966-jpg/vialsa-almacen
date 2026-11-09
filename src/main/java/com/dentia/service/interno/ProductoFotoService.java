package com.dentia.service.interno;

import com.dentia.dao.interfaces.IProductoFotoDao;
import com.dentia.model.interno.ProductoFoto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductoFotoService {

    private static final int MAX_FOTOS = 3;
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");

    private final IProductoFotoDao fotoDao;

    public ProductoFotoService(IProductoFotoDao fotoDao) {
        this.fotoDao = fotoDao;
    }

    public List<ProductoFoto> listarActivas(Integer idProducto) {
        validarProducto(idProducto);
        return fotoDao.listarActivasPorProducto(idProducto);
    }

    public ProductoFoto subirFoto(Integer idProducto, MultipartFile archivo) {
        validarProducto(idProducto);
        validarArchivo(archivo);

        int totalActual = fotoDao.contarActivasPorProducto(idProducto);
        if (totalActual >= MAX_FOTOS) {
            throw new IllegalArgumentException("Máximo 3 imágenes por producto.");
        }

        String extension = com.dentia.service.shared.FileStorageService.extensionPermitida(archivo).substring(1);

        String nombreArchivo = "PROD_" + idProducto + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + "." + extension;
        Path carpeta = Paths.get(System.getProperty("user.dir"), "uploads", "productos")
                .toAbsolutePath()
                .normalize();
        Path destino = carpeta.resolve(nombreArchivo).toAbsolutePath().normalize();

        if (!destino.startsWith(carpeta)) {
            throw new IllegalArgumentException("Ruta de imagen no valida.");
        }

        try {
            Files.createDirectories(carpeta);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar la imagen del producto.", ex);
        }

        ProductoFoto foto = new ProductoFoto();
        foto.setIdProducto(idProducto);
        foto.setNombreArchivo(nombreArchivo);
        foto.setUrlArchivo("/uploads/productos/" + nombreArchivo);
        foto.setPortada(totalActual == 0);
        foto.setOrden(totalActual + 1);
        foto.setEstado(1);

        fotoDao.guardar(foto);

        if (Boolean.TRUE.equals(foto.getPortada())) {
            fotoDao.obtenerPrimeraActivaPorProducto(idProducto)
                    .ifPresent(f -> fotoDao.marcarPortada(f.getIdProductoFoto()));
        }

        return fotoDao.listarActivasPorProducto(idProducto)
                .stream()
                .filter(f -> nombreArchivo.equals(f.getNombreArchivo()))
                .findFirst()
                .orElse(foto);
    }

    @Transactional
    public List<ProductoFoto> marcarPortada(Integer idProductoFoto) {
        validarFoto(idProductoFoto);
        ProductoFoto foto = fotoDao.buscarPorId(idProductoFoto)
                .orElseThrow(() -> new IllegalArgumentException("La imagen seleccionada no existe."));

        int filas = fotoDao.marcarPortada(idProductoFoto);

        if (filas == 0) {
            throw new IllegalArgumentException("No se pudo marcar la imagen como portada.");
        }

        return fotoDao.listarActivasPorProducto(foto.getIdProducto());
    }

    public void eliminarFoto(Integer idProductoFoto) {
        validarFoto(idProductoFoto);
        ProductoFoto foto = fotoDao.buscarPorId(idProductoFoto)
                .orElseThrow(() -> new IllegalArgumentException("La imagen seleccionada no existe."));

        boolean eraPortada = Boolean.TRUE.equals(foto.getPortada());
        int filas = fotoDao.desactivar(idProductoFoto);

        if (filas == 0) {
            throw new IllegalArgumentException("No se pudo eliminar la imagen.");
        }

        if (eraPortada) {
            fotoDao.obtenerPrimeraActivaPorProducto(foto.getIdProducto())
                    .ifPresent(siguiente -> fotoDao.marcarPortada(siguiente.getIdProductoFoto()));
        }
    }

    private void validarProducto(Integer idProducto) {
        if (idProducto == null || idProducto <= 0) {
            throw new IllegalArgumentException("Producto invalido.");
        }
    }

    private void validarFoto(Integer idProductoFoto) {
        if (idProductoFoto == null || idProductoFoto <= 0) {
            throw new IllegalArgumentException("Imagen invalida.");
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen.");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen.");
        }
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "jpg";
        }

        return nombreOriginal.substring(nombreOriginal.lastIndexOf(".") + 1).trim().toLowerCase();
    }

    private void validarExtension(String extension) {
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Solo se permiten imagenes JPG, PNG o WEBP.");
        }
    }
}

