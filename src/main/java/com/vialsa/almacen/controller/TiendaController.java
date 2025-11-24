package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Usuario;
import com.vialsa.almacen.service.UsuarioService;
import com.vialsa.almacen.service.ProductoService;
import com.vialsa.almacen.controller.RegistroClienteDTO;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class TiendaController {

    private final ProductoService productoService;

    public TiendaController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/tienda")
    public String tienda(Model model) {

        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("titulo", "Tienda VIALSA");
        model.addAttribute("cliente", new RegistroClienteDTO());

        return "tienda/tienda";
    }
}
