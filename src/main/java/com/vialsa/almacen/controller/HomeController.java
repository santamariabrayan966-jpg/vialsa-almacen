package com.vialsa.almacen.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class HomeController {
  @GetMapping("/") public String root(){ return "redirect:/dashboard"; }
  @GetMapping("/dashboard") public String dashboard(Model model){ model.addAttribute("titulo","VIALSA | Panel Principal"); return "dashboard"; }
  @GetMapping("/login") public String login(){ return "login"; }
}
