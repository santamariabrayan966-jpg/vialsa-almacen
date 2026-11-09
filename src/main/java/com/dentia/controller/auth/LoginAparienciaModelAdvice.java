package com.dentia.controller.auth;

import com.dentia.service.interno.LoginAparienciaConfigService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LoginAparienciaModelAdvice {

    private final LoginAparienciaConfigService loginAparienciaConfigService;

    public LoginAparienciaModelAdvice(LoginAparienciaConfigService loginAparienciaConfigService) {
        this.loginAparienciaConfigService = loginAparienciaConfigService;
    }

    @ModelAttribute
    public void cargarLoginApariencia(Model model) {
        try {
            model.addAttribute("loginAparienciaConfig", loginAparienciaConfigService.obtenerConfiguracion());
        } catch (Exception e) {
            model.addAttribute("loginAparienciaConfig", loginAparienciaConfigService.crearDefault());
        }
    }
}
