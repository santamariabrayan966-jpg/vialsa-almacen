package com.dentia.controller.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.model.interno.MarcaIdentidadConfig;
import com.dentia.service.interno.MarcaIdentidadService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
public class PwaController {

    private static final Pattern COLOR_HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private final MarcaIdentidadService marcaService;
    private final ObjectMapper objectMapper;

    public PwaController(MarcaIdentidadService marcaService, ObjectMapper objectMapper) {
        this.marcaService = marcaService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/app.webmanifest", produces = "application/manifest+json")
    @ResponseBody
    public ResponseEntity<String> manifest() throws JsonProcessingException {
        MarcaIdentidadConfig config = configuracion();
        String nombre = texto(config.getNombreComercial(), "DentIA");
        String version = config.getFechaActualizacion() == null
                ? "1"
                : String.valueOf(config.getFechaActualizacion().atZone(java.time.ZoneId.systemDefault()).toEpochSecond());

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("id", "/interno/");
        manifest.put("name", nombre + " · Gestión odontológica");
        manifest.put("short_name", abreviar(nombre, 18));
        manifest.put("description", texto(config.getDescripcionCorta(), "Sistema de gestión para el consultorio odontológico."));
        manifest.put("lang", "es-PE");
        manifest.put("start_url", "/interno/login?source=pwa");
        manifest.put("scope", "/");
        manifest.put("display", "standalone");
        manifest.put("orientation", "any");
        manifest.put("background_color", "#f3f9f8");
        manifest.put("theme_color", color(config.getColorPrimario(), "#075d63"));
        manifest.put("icons", List.of(
                icono(192, version, "any"),
                icono(512, version, "any"),
                icono(512, version, "maskable")
        ));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType("application/manifest+json"))
                .body(objectMapper.writeValueAsString(manifest));
    }

    @GetMapping(value = "/pwa/icon-{size}.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> icono(@PathVariable int size) throws IOException {
        if (size != 192 && size != 512) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        MarcaIdentidadConfig config = configuracion();
        BufferedImage icono = dibujarIcono(size, config);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(icono, "png", output);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(output.toByteArray());
    }

    @GetMapping(value = "/service-worker.js", produces = "application/javascript")
    @ResponseBody
    public ResponseEntity<String> serviceWorker() {
        String script = """
                self.addEventListener('install', () => self.skipWaiting());
                self.addEventListener('activate', event => event.waitUntil(self.clients.claim()));
                // DentIA maneja información clínica: las páginas autenticadas siempre se solicitan a la red.
                """;
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType("application/javascript"))
                .body(script);
    }

    private BufferedImage dibujarIcono(int size, MarcaIdentidadConfig config) {
        BufferedImage destino = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = destino.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setColor(Color.decode(color(config.getColorPrimario(), "#075d63")));
        graphics.fillRoundRect(0, 0, size, size, size / 5, size / 5);

        BufferedImage logo = leerLogo(config.getLogoPrincipal());
        if (logo != null) {
            int margen = Math.max(12, size / 10);
            double escala = Math.min((double) (size - margen * 2) / logo.getWidth(),
                    (double) (size - margen * 2) / logo.getHeight());
            int ancho = Math.max(1, (int) Math.round(logo.getWidth() * escala));
            int alto = Math.max(1, (int) Math.round(logo.getHeight() * escala));
            graphics.drawImage(logo, (size - ancho) / 2, (size - alto) / 2, ancho, alto, null);
        } else {
            graphics.setColor(Color.WHITE);
            Path2D.Double diente = new Path2D.Double();
            diente.moveTo(size * .27, size * .25);
            diente.curveTo(size * .35, size * .16, size * .43, size * .23, size * .50, size * .23);
            diente.curveTo(size * .57, size * .23, size * .65, size * .16, size * .73, size * .25);
            diente.curveTo(size * .84, size * .38, size * .69, size * .75, size * .60, size * .80);
            diente.curveTo(size * .53, size * .84, size * .54, size * .58, size * .50, size * .58);
            diente.curveTo(size * .46, size * .58, size * .47, size * .84, size * .40, size * .80);
            diente.curveTo(size * .31, size * .75, size * .16, size * .38, size * .27, size * .25);
            diente.closePath();
            graphics.fill(diente);
            graphics.setColor(Color.decode(color(config.getColorPrimario(), "#075d63")));
            graphics.setStroke(new BasicStroke(Math.max(5f, size / 35f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.drawLine((int) (size * .50), (int) (size * .31), (int) (size * .50), (int) (size * .49));
            graphics.drawLine((int) (size * .41), (int) (size * .40), (int) (size * .59), (int) (size * .40));
        }
        graphics.dispose();
        return destino;
    }

    private BufferedImage leerLogo(String url) {
        if (url == null || url.isBlank()) return null;
        try (InputStream input = abrirLogo(url.trim())) {
            return input == null ? null : ImageIO.read(input);
        } catch (Exception ignored) {
            return null;
        }
    }

    private InputStream abrirLogo(String url) throws IOException {
        if (url.startsWith("/uploads/")) {
            Path raiz = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
            Path archivo = raiz.resolve(url.substring("/uploads/".length())).normalize();
            if (!archivo.startsWith(raiz) || !Files.isRegularFile(archivo)) return null;
            return Files.newInputStream(archivo);
        }
        String recurso = url.startsWith("/") ? "static" + url : "static/" + url;
        ClassPathResource classpath = new ClassPathResource(recurso);
        return classpath.exists() ? classpath.getInputStream() : null;
    }

    private MarcaIdentidadConfig configuracion() {
        try {
            MarcaIdentidadConfig config = marcaService.obtenerConfiguracion();
            return config == null ? new MarcaIdentidadConfig() : config;
        } catch (Exception ignored) {
            return new MarcaIdentidadConfig();
        }
    }

    private Map<String, Object> icono(int size, String version, String purpose) {
        return Map.of(
                "src", "/pwa/icon-" + size + ".png?v=" + version,
                "sizes", size + "x" + size,
                "type", "image/png",
                "purpose", purpose
        );
    }

    private String texto(String valor, String fallback) {
        return valor == null || valor.isBlank() ? fallback : valor.trim();
    }

    private String color(String valor, String fallback) {
        return valor != null && COLOR_HEX.matcher(valor.trim()).matches() ? valor.trim() : fallback;
    }

    private String abreviar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite).trim();
    }

}
