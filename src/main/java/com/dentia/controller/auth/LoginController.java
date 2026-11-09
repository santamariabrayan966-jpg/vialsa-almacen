package com.dentia.controller.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/interno/login")
    public String login(Authentication authentication,
                        @RequestParam(required = false) String preview,
                        Model model) {

        if ("embed".equals(preview)) {
            model.addAttribute("modoPreviewLogin", true);
        }

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            boolean esPaciente = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));

            return esPaciente
                    ? "redirect:/paciente"
                    : "redirect:/interno/dashboard";
        }

        return "auth/login-empleados";
    }
}
