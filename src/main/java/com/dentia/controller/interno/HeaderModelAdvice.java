package com.dentia.controller.interno;

import com.dentia.service.interno.PanelColoresConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class HeaderModelAdvice {

    private final PanelColoresConfigService panelColoresConfigService;

    public HeaderModelAdvice(PanelColoresConfigService panelColoresConfigService) {
        this.panelColoresConfigService = panelColoresConfigService;
    }

    @ModelAttribute
    public void cargarDatosHeader(Model model, HttpSession session) {

        Integer sedeActivaId = (Integer) session.getAttribute("sedeActivaId");
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");
        Integer usuarioActualId = (Integer) session.getAttribute("usuarioActualId");
        String usuarioActualNombre = (String) session.getAttribute("usuarioActualNombre");
        String usuarioActualRol = (String) session.getAttribute("usuarioActualRol");

        model.addAttribute("sedeActivaId", sedeActivaId);
        model.addAttribute("sedeActivaNombre", sedeActivaNombre);
        model.addAttribute("puedeCambiarSede", false);

        model.addAttribute("usuarioActualId", usuarioActualId);
        model.addAttribute("usuarioActualNombre", usuarioActualNombre);
        model.addAttribute("usuarioActualRol", usuarioActualRol);
        try {
            model.addAttribute("panelColoresConfig", panelColoresConfigService.obtenerConfiguracion());
        } catch (Exception e) {
            model.addAttribute("panelColoresConfig", panelColoresConfigService.crearDefault());
        }
    }
}
