package com.dentia.service.interno;

import com.dentia.dao.interfaces.ILoginAparienciaConfigDao;
import com.dentia.model.interno.LoginAparienciaConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class LoginAparienciaConfigService {

    public static final String TIPO_COLOR = "COLOR";
    public static final String TIPO_DEGRADADO = "DEGRADADO";
    public static final String TIPO_IMAGEN = "IMAGEN";
    public static final String POSICION_IZQUIERDA = "IZQUIERDA";
    public static final String POSICION_CENTRO = "CENTRO";
    public static final String POSICION_DERECHA = "DERECHA";

    private static final int ID_CONFIG = 1;
    private static final String COLOR_FONDO_1 = "#082f49";
    private static final String COLOR_FONDO_2 = "#4c0519";
    private static final String TITULO = "DentIA";
    private static final String SUBTITULO = "Gestion inteligente para tu consultorio dental";
    private static final String TEXTO_INFORMATIVO = "Acceso exclusivo para el equipo del consultorio";
    private static final String COLOR_CARD = "#24384c";
    private static final String COLOR_TEXTO = "#ffffff";
    private static final String COLOR_BOTON_1 = "#0d6efd";
    private static final String COLOR_BOTON_2 = "#c2185b";
    private static final String COLOR_TEXTO_BOTON = "#ffffff";
    private static final BigDecimal CARRUSEL_ZOOM_DEFAULT = new BigDecimal("1.000");
    private static final BigDecimal CARRUSEL_ZOOM_MIN = new BigDecimal("0.500");
    private static final BigDecimal CARRUSEL_ZOOM_MAX = new BigDecimal("3.000");
    private static final BigDecimal CARRUSEL_OFFSET_DEFAULT = new BigDecimal("0.000");
    private static final BigDecimal CARRUSEL_OFFSET_MIN = new BigDecimal("-800.000");
    private static final BigDecimal CARRUSEL_OFFSET_MAX = new BigDecimal("800.000");
    private static final String RUTA_PUBLICA_LOGIN = "/uploads/login/";
    private static final String RUTA_PUBLICA_CARRUSEL = "/uploads/login/carrusel/";
    private static final Set<String> TIPOS_FONDO = Set.of(TIPO_COLOR, TIPO_DEGRADADO, TIPO_IMAGEN);
    private static final Set<String> POSICIONES = Set.of(POSICION_IZQUIERDA, POSICION_CENTRO, POSICION_DERECHA);
    private static final Set<String> CONTENT_TYPES_IMAGEN = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Pattern HEX_COLOR = Pattern.compile("^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$");

    private final ILoginAparienciaConfigDao loginAparienciaConfigDao;

    public LoginAparienciaConfigService(ILoginAparienciaConfigDao loginAparienciaConfigDao) {
        this.loginAparienciaConfigDao = loginAparienciaConfigDao;
    }

    public LoginAparienciaConfig obtenerConfiguracion() {
        LoginAparienciaConfig config = loginAparienciaConfigDao.obtenerConfiguracion();
        if (config == null) {
            config = crearDefault();
        }

        normalizar(config, null);
        return config;
    }

    public void guardar(LoginAparienciaConfig config,
                        MultipartFile imagenFondoFile,
                        boolean eliminarImagen,
                        MultipartFile carruselImagen1File,
                        MultipartFile carruselImagen2File,
                        MultipartFile carruselImagen3File,
                        boolean eliminarCarruselImagen1,
                        boolean eliminarCarruselImagen2,
                        boolean eliminarCarruselImagen3) throws IOException {
        LoginAparienciaConfig actual = obtenerConfiguracion();
        if (config == null) {
            config = crearDefault();
        }

        normalizar(config, actual.getImagenFondo());
        conservarCarruselActual(config, actual);

        if (eliminarImagen) {
            config.setImagenFondo(null);
        }

        if (imagenFondoFile != null && !imagenFondoFile.isEmpty()) {
            config.setImagenFondo(guardarImagen(imagenFondoFile));
        }

        if (TIPO_IMAGEN.equals(config.getTipoFondo()) && (config.getImagenFondo() == null || config.getImagenFondo().isBlank())) {
            config.setTipoFondo(TIPO_DEGRADADO);
        }

        if (eliminarCarruselImagen1) {
            config.setCarruselImagen1(null);
        }
        if (eliminarCarruselImagen2) {
            config.setCarruselImagen2(null);
        }
        if (eliminarCarruselImagen3) {
            config.setCarruselImagen3(null);
        }

        if (carruselImagen1File != null && !carruselImagen1File.isEmpty()) {
            config.setCarruselImagen1(guardarImagenCarrusel(carruselImagen1File, 1));
        }
        if (carruselImagen2File != null && !carruselImagen2File.isEmpty()) {
            config.setCarruselImagen2(guardarImagenCarrusel(carruselImagen2File, 2));
        }
        if (carruselImagen3File != null && !carruselImagen3File.isEmpty()) {
            config.setCarruselImagen3(guardarImagenCarrusel(carruselImagen3File, 3));
        }

        loginAparienciaConfigDao.guardar(config);
    }

    public void restaurarDefaults() {
        loginAparienciaConfigDao.guardar(crearDefault());
    }

    public LoginAparienciaConfig crearDefault() {
        LoginAparienciaConfig config = new LoginAparienciaConfig();
        config.setIdConfig(ID_CONFIG);
        config.setTipoFondo(TIPO_DEGRADADO);
        config.setColorFondo1(COLOR_FONDO_1);
        config.setColorFondo2(COLOR_FONDO_2);
        config.setImagenFondo(null);
        config.setPosicionCard(POSICION_CENTRO);
        config.setTitulo(TITULO);
        config.setSubtitulo(SUBTITULO);
        config.setTextoInformativo(TEXTO_INFORMATIVO);
        config.setColorCard(COLOR_CARD);
        config.setColorTexto(COLOR_TEXTO);
        config.setColorBoton1(COLOR_BOTON_1);
        config.setColorBoton2(COLOR_BOTON_2);
        config.setColorTextoBoton(COLOR_TEXTO_BOTON);
        config.setCarruselActivo(false);
        config.setCarruselImagen1(null);
        config.setCarruselImagen1Zoom(CARRUSEL_ZOOM_DEFAULT);
        config.setCarruselImagen1OffsetX(CARRUSEL_OFFSET_DEFAULT);
        config.setCarruselImagen1OffsetY(CARRUSEL_OFFSET_DEFAULT);
        config.setCarruselImagen2(null);
        config.setCarruselImagen2Zoom(CARRUSEL_ZOOM_DEFAULT);
        config.setCarruselImagen2OffsetX(CARRUSEL_OFFSET_DEFAULT);
        config.setCarruselImagen2OffsetY(CARRUSEL_OFFSET_DEFAULT);
        config.setCarruselImagen3(null);
        config.setCarruselImagen3Zoom(CARRUSEL_ZOOM_DEFAULT);
        config.setCarruselImagen3OffsetX(CARRUSEL_OFFSET_DEFAULT);
        config.setCarruselImagen3OffsetY(CARRUSEL_OFFSET_DEFAULT);
        return config;
    }

    private void normalizar(LoginAparienciaConfig c, String imagenActual) {
        c.setIdConfig(ID_CONFIG);
        c.setTipoFondo(valorPermitido(c.getTipoFondo(), TIPOS_FONDO, TIPO_DEGRADADO));
        c.setColorFondo1(color(c.getColorFondo1(), COLOR_FONDO_1));
        c.setColorFondo2(color(c.getColorFondo2(), COLOR_FONDO_2));
        c.setImagenFondo(textoOpcional(c.getImagenFondo(), 255, imagenActual));
        c.setPosicionCard(valorPermitido(c.getPosicionCard(), POSICIONES, POSICION_CENTRO));
        c.setTitulo(texto(c.getTitulo(), TITULO, 120));
        c.setSubtitulo(texto(c.getSubtitulo(), SUBTITULO, 180));
        c.setTextoInformativo(texto(c.getTextoInformativo(), TEXTO_INFORMATIVO, 255));
        c.setColorCard(color(c.getColorCard(), COLOR_CARD));
        c.setColorTexto(color(c.getColorTexto(), COLOR_TEXTO));
        c.setColorBoton1(color(c.getColorBoton1(), COLOR_BOTON_1));
        c.setColorBoton2(color(c.getColorBoton2(), COLOR_BOTON_2));
        c.setColorTextoBoton(color(c.getColorTextoBoton(), COLOR_TEXTO_BOTON));
        c.setCarruselActivo(Boolean.TRUE.equals(c.getCarruselActivo()));
        c.setCarruselImagen1(textoOpcional(c.getCarruselImagen1(), 255, null));
        c.setCarruselImagen1Zoom(decimalEnRango(c.getCarruselImagen1Zoom(), CARRUSEL_ZOOM_MIN, CARRUSEL_ZOOM_MAX, CARRUSEL_ZOOM_DEFAULT));
        c.setCarruselImagen1OffsetX(decimalEnRango(c.getCarruselImagen1OffsetX(), CARRUSEL_OFFSET_MIN, CARRUSEL_OFFSET_MAX, CARRUSEL_OFFSET_DEFAULT));
        c.setCarruselImagen1OffsetY(decimalEnRango(c.getCarruselImagen1OffsetY(), CARRUSEL_OFFSET_MIN, CARRUSEL_OFFSET_MAX, CARRUSEL_OFFSET_DEFAULT));
        c.setCarruselImagen2(textoOpcional(c.getCarruselImagen2(), 255, null));
        c.setCarruselImagen2Zoom(decimalEnRango(c.getCarruselImagen2Zoom(), CARRUSEL_ZOOM_MIN, CARRUSEL_ZOOM_MAX, CARRUSEL_ZOOM_DEFAULT));
        c.setCarruselImagen2OffsetX(decimalEnRango(c.getCarruselImagen2OffsetX(), CARRUSEL_OFFSET_MIN, CARRUSEL_OFFSET_MAX, CARRUSEL_OFFSET_DEFAULT));
        c.setCarruselImagen2OffsetY(decimalEnRango(c.getCarruselImagen2OffsetY(), CARRUSEL_OFFSET_MIN, CARRUSEL_OFFSET_MAX, CARRUSEL_OFFSET_DEFAULT));
        c.setCarruselImagen3(textoOpcional(c.getCarruselImagen3(), 255, null));
        c.setCarruselImagen3Zoom(decimalEnRango(c.getCarruselImagen3Zoom(), CARRUSEL_ZOOM_MIN, CARRUSEL_ZOOM_MAX, CARRUSEL_ZOOM_DEFAULT));
        c.setCarruselImagen3OffsetX(decimalEnRango(c.getCarruselImagen3OffsetX(), CARRUSEL_OFFSET_MIN, CARRUSEL_OFFSET_MAX, CARRUSEL_OFFSET_DEFAULT));
        c.setCarruselImagen3OffsetY(decimalEnRango(c.getCarruselImagen3OffsetY(), CARRUSEL_OFFSET_MIN, CARRUSEL_OFFSET_MAX, CARRUSEL_OFFSET_DEFAULT));
    }

    private void conservarCarruselActual(LoginAparienciaConfig config, LoginAparienciaConfig actual) {
        if (config.getCarruselImagen1() == null || config.getCarruselImagen1().isBlank()) {
            config.setCarruselImagen1(actual.getCarruselImagen1());
        }
        if (config.getCarruselImagen2() == null || config.getCarruselImagen2().isBlank()) {
            config.setCarruselImagen2(actual.getCarruselImagen2());
        }
        if (config.getCarruselImagen3() == null || config.getCarruselImagen3().isBlank()) {
            config.setCarruselImagen3(actual.getCarruselImagen3());
        }
    }

    private String guardarImagen(MultipartFile archivo) throws IOException {
        validarImagen(archivo);

        Path carpeta = Paths.get(System.getProperty("user.dir"), "uploads", "login")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(carpeta);

        String extension = com.dentia.service.shared.FileStorageService.extensionPermitida(archivo);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String nombreArchivo = "login-bg-" + fecha + extension;

        Path rutaFinal = carpeta.resolve(nombreArchivo)
                .toAbsolutePath()
                .normalize();

        if (!rutaFinal.startsWith(carpeta)) {
            throw new IllegalArgumentException("Ruta de imagen invalida.");
        }

        archivo.transferTo(rutaFinal.toFile());
        return RUTA_PUBLICA_LOGIN + nombreArchivo;
    }

    private String guardarImagenCarrusel(MultipartFile archivo, int numero) throws IOException {
        validarImagen(archivo);

        Path carpeta = Paths.get(System.getProperty("user.dir"), "uploads", "login", "carrusel")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(carpeta);

        String extension = com.dentia.service.shared.FileStorageService.extensionPermitida(archivo);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String nombreArchivo = "login-carousel-" + numero + "-" + fecha + extension;

        Path rutaFinal = carpeta.resolve(nombreArchivo)
                .toAbsolutePath()
                .normalize();

        if (!rutaFinal.startsWith(carpeta)) {
            throw new IllegalArgumentException("Ruta de imagen invalida.");
        }

        archivo.transferTo(rutaFinal.toFile());
        return RUTA_PUBLICA_CARRUSEL + nombreArchivo;
    }

    private void validarImagen(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        if (contentType == null || !CONTENT_TYPES_IMAGEN.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("La imagen de fondo debe ser JPG, PNG o WEBP.");
        }
    }

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return ".png";
        }

        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case ".jpg", ".jpeg", ".png", ".webp" -> extension;
            default -> ".png";
        };
    }

    private String color(String valor, String defecto) {
        if (valor == null || valor.isBlank()) {
            return defecto;
        }

        String limpio = valor.trim();
        return HEX_COLOR.matcher(limpio).matches() ? limpio : defecto;
    }

    private String texto(String valor, String defecto, int maximo) {
        if (valor == null || valor.isBlank()) {
            return defecto;
        }

        String limpio = valor.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    private String textoOpcional(String valor, int maximo, String fallback) {
        if (valor == null || valor.isBlank()) {
            return fallback;
        }

        String limpio = valor.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    private String valorPermitido(String valor, Set<String> permitidos, String defecto) {
        if (valor == null || valor.isBlank()) {
            return defecto;
        }

        String limpio = valor.trim().toUpperCase(Locale.ROOT);
        return permitidos.contains(limpio) ? limpio : defecto;
    }

    private BigDecimal decimalEnRango(BigDecimal valor, BigDecimal minimo, BigDecimal maximo, BigDecimal defecto) {
        if (valor == null) {
            return defecto;
        }
        if (valor.compareTo(minimo) < 0) {
            return minimo;
        }
        if (valor.compareTo(maximo) > 0) {
            return maximo;
        }
        return valor;
    }
}
