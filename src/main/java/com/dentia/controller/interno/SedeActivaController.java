package com.dentia.controller.interno;

import com.dentia.model.interno.Sede;
import com.dentia.service.interno.SedeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SedeActivaController {

    private final SedeService sedeService;

    public SedeActivaController(SedeService sedeService) {
        this.sedeService = sedeService;
    }

    @PostMapping("/interno/sesion/cambiar-sede")
    public String cambiarSedeActiva(
            @RequestParam("idSede") Integer idSede,
            HttpSession session,
            HttpServletRequest request
    ) {

        Boolean puedeCambiarSede = (Boolean) session.getAttribute("puedeCambiarSede");

        if (!Boolean.TRUE.equals(puedeCambiarSede)) {
            return "redirect:/acceso-denegado";
        }

        Sede sede = sedeService.buscarPorId(idSede);

        if (sede == null || !sede.isActivo()) {
            return "redirect:/interno/dashboard?sedeInvalida";
        }

        session.setAttribute("sedeActivaId", sede.getIdSede());
        session.setAttribute("sedeActivaNombre", sede.getNombre());

        String referer = request.getHeader("Referer");

        if (referer != null && referer.contains("/interno/")) {
            return "redirect:" + referer;
        }

        return "redirect:/interno/dashboard?sedeCambiada";
    }
}