package com.dentia.controller.interno;

import com.dentia.model.interno.MarcaIdentidadConfig;
import com.dentia.service.interno.MarcaIdentidadService;
import com.dentia.service.shared.FileStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/interno/configuracion")
public class ConsultorioController {
    private final MarcaIdentidadService service;
    private final FileStorageService archivos;

    public ConsultorioController(MarcaIdentidadService service, FileStorageService archivos) {
        this.service = service;
        this.archivos = archivos;
    }

    @GetMapping
    public String configuracion(Model model) {
        model.addAttribute("config", service.obtenerConfiguracion());
        return "interno/configuracion/consultorio";
    }

    @PostMapping
    public String guardar(@ModelAttribute MarcaIdentidadConfig cambios,
                          @RequestParam(required = false) MultipartFile logo,
                          @RequestParam(defaultValue = "false") boolean eliminarLogo,
                          RedirectAttributes redirect) {
        try {
            MarcaIdentidadConfig config = service.obtenerConfiguracion();
            config.setNombreComercial(cambios.getNombreComercial());
            config.setSlogan(cambios.getSlogan() != null ? cambios.getSlogan().trim() : "");
            config.setRuc(cambios.getRuc());
            config.setDireccionPrincipal(cambios.getDireccionPrincipal());
            config.setWhatsappPrincipal(cambios.getWhatsappPrincipal());
            String logoNuevo = archivos.guardarLogoConsultorio(logo);
            if (eliminarLogo) {
                config.setLogoOpcion1("");
                config.setLogoPrincipal("");
                config.setLogoActivo(1);
            } else if (logoNuevo != null) {
                config.setLogoOpcion1(logoNuevo);
                config.setLogoPrincipal(logoNuevo);
                config.setLogoActivo(1);
            }
            service.guardar(config);
            redirect.addFlashAttribute("ok", "Configuracion del consultorio guardada.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/interno/configuracion";
    }
}
