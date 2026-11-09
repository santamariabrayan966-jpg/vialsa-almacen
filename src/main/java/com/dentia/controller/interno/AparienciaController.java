package com.dentia.controller.interno;

import com.dentia.model.interno.LoginAparienciaConfig;
import com.dentia.model.interno.MarcaIdentidadConfig;
import com.dentia.model.interno.PanelColoresConfig;
import com.dentia.service.interno.LoginAparienciaConfigService;
import com.dentia.service.interno.MarcaIdentidadService;
import com.dentia.service.interno.PanelColoresConfigService;
import com.dentia.service.shared.FileStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/interno/apariencia")
public class AparienciaController {

    private static final String RUTA_PUBLICA_MARCA = "/uploads/marca/";

    private final MarcaIdentidadService marcaIdentidadService;
    private final PanelColoresConfigService panelColoresConfigService;
    private final LoginAparienciaConfigService loginAparienciaConfigService;

    public AparienciaController(MarcaIdentidadService marcaIdentidadService,
                                PanelColoresConfigService panelColoresConfigService,
                                LoginAparienciaConfigService loginAparienciaConfigService) {
        this.marcaIdentidadService = marcaIdentidadService;
        this.panelColoresConfigService = panelColoresConfigService;
        this.loginAparienciaConfigService = loginAparienciaConfigService;
    }

    @GetMapping("/marca")
    public String marcaIdentidad(Model model) {
        MarcaIdentidadConfig config = marcaIdentidadService.obtenerConfiguracion();

        model.addAttribute("config", config);
        model.addAttribute("moduloActivo", "apariencia");
        model.addAttribute("seccionActiva", "marca");

        return "interno/apariencia/marca";
    }

    @GetMapping("/panel")
    public String panelInterno(Model model) {
        model.addAttribute("panelColoresConfig", panelColoresConfigService.obtenerConfiguracion());
        model.addAttribute("loginAparienciaConfig", loginAparienciaConfigService.obtenerConfiguracion());
        model.addAttribute("moduloActivo", "apariencia");
        model.addAttribute("seccionActiva", "panel");

        return "interno/apariencia/panel";
    }

    @PostMapping("/panel/colores/guardar")
    public String guardarColoresPanel(@ModelAttribute("panelColoresConfig") PanelColoresConfig panelColoresConfig,
                                      RedirectAttributes redirectAttributes) {
        try {
            panelColoresConfig.setIdConfig(1);
            panelColoresConfigService.guardar(panelColoresConfig);
            redirectAttributes.addFlashAttribute("mensajeExito", "Configuración del panel interno guardada correctamente.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo guardar la configuración del panel interno.");
        }

        return "redirect:/interno/apariencia/panel";
    }

    @PostMapping("/panel/colores/restaurar")
    public String restaurarColoresPanel(RedirectAttributes redirectAttributes) {
        try {
            panelColoresConfigService.restaurarDefaults();
            redirectAttributes.addFlashAttribute("mensajeExito", "Colores del panel interno restaurados correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudieron restaurar los colores del panel interno.");
        }

        return "redirect:/interno/apariencia/panel";
    }

    @PostMapping("/panel/login/guardar")
    public String guardarLoginPanel(@ModelAttribute("loginAparienciaConfig") LoginAparienciaConfig loginAparienciaConfig,
                                    @RequestParam(value = "imagenFondoFile", required = false) MultipartFile imagenFondoFile,
                                    @RequestParam(value = "eliminarImagen", defaultValue = "false") boolean eliminarImagen,
                                    @RequestParam(value = "carruselImagen1File", required = false) MultipartFile carruselImagen1File,
                                    @RequestParam(value = "carruselImagen2File", required = false) MultipartFile carruselImagen2File,
                                    @RequestParam(value = "carruselImagen3File", required = false) MultipartFile carruselImagen3File,
                                    @RequestParam(value = "eliminarCarruselImagen1", defaultValue = "false") boolean eliminarCarruselImagen1,
                                    @RequestParam(value = "eliminarCarruselImagen2", defaultValue = "false") boolean eliminarCarruselImagen2,
                                    @RequestParam(value = "eliminarCarruselImagen3", defaultValue = "false") boolean eliminarCarruselImagen3,
                                    RedirectAttributes redirectAttributes) {
        try {
            loginAparienciaConfigService.guardar(
                    loginAparienciaConfig,
                    imagenFondoFile,
                    eliminarImagen,
                    carruselImagen1File,
                    carruselImagen2File,
                    carruselImagen3File,
                    eliminarCarruselImagen1,
                    eliminarCarruselImagen2,
                    eliminarCarruselImagen3);
            redirectAttributes.addFlashAttribute("mensajeExito", "Diseño del login guardado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo guardar el diseño del login.");
        }

        return "redirect:/interno/apariencia/panel#login";
    }

    @PostMapping("/panel/login/restaurar")
    public String restaurarLoginPanel(RedirectAttributes redirectAttributes) {
        try {
            loginAparienciaConfigService.restaurarDefaults();
            redirectAttributes.addFlashAttribute("mensajeExito", "Diseño del login restaurado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo restaurar el diseño del login.");
        }

        return "redirect:/interno/apariencia/panel#login";
    }

    @PostMapping("/marca/guardar")
    public String guardarMarca(@ModelAttribute("config") MarcaIdentidadConfig config,
                               @RequestParam(value = "archivoLogo1", required = false) MultipartFile archivoLogo1,
                               @RequestParam(value = "archivoLogo2", required = false) MultipartFile archivoLogo2,
                               @RequestParam(value = "archivoLogo3", required = false) MultipartFile archivoLogo3,
                               @RequestParam(value = "eliminarLogo1", defaultValue = "false") Boolean eliminarLogo1,
                               @RequestParam(value = "eliminarLogo2", defaultValue = "false") Boolean eliminarLogo2,
                               @RequestParam(value = "eliminarLogo3", defaultValue = "false") Boolean eliminarLogo3,
                               @RequestParam(value = "tabActiva", defaultValue = "logos") String tabActiva,
                               RedirectAttributes redirectAttributes) {

        try {
            MarcaIdentidadConfig actual = marcaIdentidadService.obtenerConfiguracion();

            if (config.getIdConfig() == null) {
                config.setIdConfig(actual.getIdConfig());
            }

            config.setLogoOpcion1(actual.getLogoOpcion1());
            config.setLogoOpcion2(actual.getLogoOpcion2());
            config.setLogoOpcion3(actual.getLogoOpcion3());

            if (Boolean.TRUE.equals(eliminarLogo1)) {
                config.setLogoOpcion1("");
            }

            if (Boolean.TRUE.equals(eliminarLogo2)) {
                config.setLogoOpcion2("");
            }

            if (Boolean.TRUE.equals(eliminarLogo3)) {
                config.setLogoOpcion3("");
            }

            if (archivoLogo1 != null && !archivoLogo1.isEmpty()) {
                config.setLogoOpcion1(guardarArchivoMarca(archivoLogo1, "logo-1"));
            }

            if (archivoLogo2 != null && !archivoLogo2.isEmpty()) {
                config.setLogoOpcion2(guardarArchivoMarca(archivoLogo2, "logo-2"));
            }

            if (archivoLogo3 != null && !archivoLogo3.isEmpty()) {
                config.setLogoOpcion3(guardarArchivoMarca(archivoLogo3, "logo-3"));
            }

            marcaIdentidadService.guardar(config);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Cambios de marca e identidad guardados correctamente.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "No se pudo guardar la marca. Revisa que las imágenes sean válidas.");
        }

        return "redirect:/interno/apariencia/marca?tab=" + validarTabMarca(tabActiva);
    }

    @PostMapping("/marca/publicar")
    public String publicarMarca(RedirectAttributes redirectAttributes) {
        marcaIdentidadService.publicar();
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Identidad de DentIA publicada correctamente.");
        return "redirect:/interno/apariencia/marca";
    }

    @PostMapping("/marca/restaurar")
    public String restaurarMarca(RedirectAttributes redirectAttributes) {
        marcaIdentidadService.restaurar();
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Marca e identidad restaurada a la versión base.");
        return "redirect:/interno/apariencia/marca";
    }

    private String guardarArchivoMarca(MultipartFile archivo, String prefijo) throws IOException {
        validarImagen(archivo);

        Path carpeta = Paths.get(System.getProperty("user.dir"), "uploads", "marca")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(carpeta);

        String extension = FileStorageService.extensionPermitida(archivo);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String nombreArchivo = prefijo + "-" + fecha + extension;

        Path rutaFinal = carpeta.resolve(nombreArchivo)
                .toAbsolutePath()
                .normalize();

        archivo.transferTo(rutaFinal.toFile());

        return RUTA_PUBLICA_MARCA + nombreArchivo;
    }

    private String validarTabMarca(String tabActiva) {
        if (tabActiva == null || tabActiva.isBlank()) {
            return "logos";
        }

        return switch (tabActiva) {
            case "logos", "datos", "comunicacion", "redes" -> tabActiva;
            default -> "logos";
        };
    }

    private void validarImagen(MultipartFile archivo) {
        FileStorageService.extensionPermitida(archivo);
    }
}
