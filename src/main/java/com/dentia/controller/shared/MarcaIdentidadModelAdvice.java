package com.dentia.controller.shared;

import com.dentia.model.interno.MarcaIdentidadConfig;
import com.dentia.service.interno.MarcaIdentidadService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class MarcaIdentidadModelAdvice {

    private static final String LOGO_FALLBACK = "/img/dentia-mark.svg";

    private final MarcaIdentidadService marcaIdentidadService;

    public MarcaIdentidadModelAdvice(MarcaIdentidadService marcaIdentidadService) {
        this.marcaIdentidadService = marcaIdentidadService;
    }

    @ModelAttribute
    public void cargarMarcaIdentidad(Model model) {
        MarcaIdentidadConfig marcaConfig = null;
        String logoPrincipalUrl = LOGO_FALLBACK;

        try {
            marcaConfig = marcaIdentidadService.obtenerConfiguracion();

            if (marcaConfig != null) {
                String activo = marcaConfig.obtenerLogoActivo();
                if (esLogoValido(activo)) {
                    logoPrincipalUrl = activo.trim();
                } else if (esLogoValido(marcaConfig.getLogoPrincipal())) {
                    logoPrincipalUrl = marcaConfig.getLogoPrincipal().trim();
                }
            }
        } catch (Exception ignored) {
            // Mantiene disponible el logo base si la configuracion de marca aun no esta lista.
        }

        model.addAttribute("marcaConfig", marcaConfig);
        model.addAttribute("logoPrincipalUrl", logoPrincipalUrl);
    }

    private boolean esLogoValido(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return false;
        }
        String clean = ruta.trim().toLowerCase();
        return !clean.contains("vialsa") && !clean.startsWith("/img/logos/");
    }
}
