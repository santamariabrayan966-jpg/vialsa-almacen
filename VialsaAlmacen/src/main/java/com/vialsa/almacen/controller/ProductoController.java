package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Producto;
import com.vialsa.almacen.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("titulo", "Gestión de Productos | VIALSA");
        model.addAttribute("productos", service.listar());
        model.addAttribute("productoForm", new Producto());
        return "productos/list";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("productoForm") Producto form,
                        BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("titulo", "Gestión de Productos | VIALSA");
            model.addAttribute("productos", service.listar());
            return "productos/list";
        }
        service.guardar(form);
        return "redirect:/productos";
    }

    @GetMapping("/{id}")
    public String editar(@PathVariable("id") Integer id, Model model) {
        Producto p = service.obtener(id);
        if (p == null) {
            return "redirect:/productos";
        }
        model.addAttribute("titulo", "Editar Producto | VIALSA");
        model.addAttribute("productoForm", p);
        model.addAttribute("productos", service.listar());
        return "productos/list";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable("id") Integer id) {
        service.eliminar(id);
        return "redirect:/productos";
    }
}
