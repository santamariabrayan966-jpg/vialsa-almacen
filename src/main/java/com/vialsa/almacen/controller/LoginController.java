package com.vialsa.almacen.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Authentication authentication) {

        // ✅ Si YA está autenticado, no mostramos el login otra vez
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            return "redirect:/dashboard";
        }

        // ✅ Si NO está autenticado, mostramos la vista de login
        // (templates/login.html)
        return "login";
    }
}
