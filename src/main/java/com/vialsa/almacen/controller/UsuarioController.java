package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Rol;
import com.vialsa.almacen.model.Usuario;
import com.vialsa.almacen.service.RolService;
import com.vialsa.almacen.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RolService rolService;

    // 📋 Listado de usuarios
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        return "usuarios/list";
    }

    // ➕ Formulario para crear nuevo usuario
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", rolService.listarRoles());
        return "usuarios/form";
    }

    // 💾 Guardar nuevo usuario
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario usuario) {
        usuarioService.guardar(usuario);
        return "redirect:/usuarios";
    }

    // ✏️ Formulario para editar un usuario existente
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable int id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id);
        List<Rol> roles = rolService.listarRoles();
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", roles);
        return "usuarios/edit";
    }

    // 💾 Actualizar usuario existente
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Usuario usuario) {
        usuarioService.actualizar(usuario);
        return "redirect:/usuarios";
    }

    // 🗑️ Eliminar usuario
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) {
        usuarioService.eliminar(id);
        return "redirect:/usuarios";
    }
}
