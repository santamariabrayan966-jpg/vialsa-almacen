package com.vialsa.almacen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        // Ahora la página principal es la tienda pública
        return "redirect:/tienda";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("titulo", "VIALSA | Panel Principal");
        return "dashboard"; // dashboard.html
    }

    // ❗ IMPORTANTE:
    // NO definir @GetMapping("/login") aquí
    // Spring Security se encarga de manejar /login automáticamente
}
