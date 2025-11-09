package com.vialsa.almacen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        // Redirige a /dashboard cuando entran a la raíz
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("titulo", "VIALSA | Panel Principal");
        return "dashboard"; // dashboard.html
    }

    // 👇 IMPORTANTE: ya NO debe existir el @GetMapping("/login") aquí
}
