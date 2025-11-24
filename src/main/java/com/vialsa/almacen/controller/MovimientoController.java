package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Movimiento;
import com.vialsa.almacen.service.MovimientoService;
import com.vialsa.almacen.service.ProductoService;
import com.vialsa.almacen.service.UnidadService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventario")
public class MovimientoController {

    private final MovimientoService movimientoService;
    private final ProductoService productoService;
    private final UnidadService unidadService;

    public MovimientoController(MovimientoService movimientoService,
                                ProductoService productoService,
                                UnidadService unidadService) {
        this.movimientoService = movimientoService;
        this.productoService = productoService;
        this.unidadService = unidadService;
    }

    // 📋 LISTAR MOVIMIENTOS
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("titulo", "Inventario | VIALSA");
        model.addAttribute("movimientos", movimientoService.listar());
        return "inventario/list";
    }

    // 🆕 FORMULARIO NUEVO MOVIMIENTO (modal)
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Movimiento m = new Movimiento();
        m.setOrigen("MANUAL"); // 👈 por defecto manual

        model.addAttribute("titulo", "Nuevo Movimiento | VIALSA");
        model.addAttribute("movimientoForm", m);
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("unidades", unidadService.listar());
        return "inventario/form";
    }

    // 💾 GUARDAR MOVIMIENTO
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Movimiento movimiento) {
        try {

            // 1️⃣ Usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null || auth.getName().equals("anonymousUser")) {
                throw new IllegalStateException("No hay un usuario autenticado.");
            }

            Integer idUsuario = movimientoService.obtenerIdUsuarioPorNombre(auth.getName());
            if (idUsuario == null) {
                throw new IllegalStateException("No se encontró el usuario en la base de datos.");
            }

            movimiento.setIdUsuario(idUsuario);

            // 2️⃣ Registrar correctamente
            boolean ok = movimientoService.registrar(movimiento);
            if (!ok) {
                throw new IllegalStateException("Error al registrar el movimiento.");
            }

            return "redirect:/inventario?success=true";

        } catch (Exception e) {
            System.err.println("❌ Error Movimiento: " + e.getMessage());
            return "redirect:/inventario?error=true";
        }
    }
}
