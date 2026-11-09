package com.dentia.controller.paciente;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/paciente")
public class PatientPortalController {

    @GetMapping({"", "/", "/inicio", "/reservar-cita", "/mis-citas"})
    public String redirectHaciaReservasEnLanding() {
        // El cliente no tiene portal separado; gestiona sus reservas directamente en la página principal
        return "redirect:/#reservar-cita";
    }
}
