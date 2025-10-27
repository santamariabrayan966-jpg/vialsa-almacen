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

import java.time.LocalDateTime;

@Controller
@RequestMapping("/inventario")
public class MovimientoController {

    private final MovimientoService movimientoService;
    private final ProductoService productoService;
    private final UnidadService unidadService; // ✅ Nuevo servicio

    // 🧩 Inyección de dependencias
    public MovimientoController(MovimientoService movimientoService,
                                ProductoService productoService,
                                UnidadService unidadService) {
        this.movimientoService = movimientoService;
        this.productoService = productoService;
        this.unidadService = unidadService;
    }

    // 📋 Listar todos los movimientos del inventario
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("titulo", "Inventario | VIALSA");
        model.addAttribute("movimientos", movimientoService.listar());
        return "inventario/list";
    }

    // 🆕 Formulario para registrar un nuevo movimiento
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("titulo", "Nuevo Movimiento | VIALSA");
        model.addAttribute("movimientoForm", new Movimiento());
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("unidades", unidadService.listar()); // ✅ Unidades dinámicas
        return "inventario/form";
    }

    // 💾 Registrar un movimiento (Entrada / Salida)
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Movimiento movimiento) {
        try {
            // 🧠 1️⃣ Obtener el usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null || auth.getName().equals("anonymousUser")) {
                throw new IllegalStateException("No hay un usuario autenticado actualmente.");
            }

            String nombreUsuario = auth.getName();
            Integer idUsuario = movimientoService.obtenerIdUsuarioPorNombre(nombreUsuario);

            if (idUsuario == null) {
                throw new IllegalStateException("No se encontró el usuario '" + nombreUsuario + "' en la base de datos.");
            }

            // 🕒 2️⃣ Asignar metadatos
            movimiento.setIdUsuario(idUsuario);
            movimiento.setFecha(LocalDateTime.now());

            // ✅ 3️⃣ Registrar movimiento en la BD
            boolean registrado = movimientoService.registrar(movimiento);
            if (!registrado) {
                throw new IllegalStateException("Error al registrar el movimiento en la base de datos.");
            }

            return "redirect:/inventario";

        } catch (Exception e) {
            // ⚠️ Registro del error y redirección con alerta
            System.err.println("❌ Error al guardar movimiento: " + e.getMessage());
            return "redirect:/inventario?error=true";
        }
    }
}
