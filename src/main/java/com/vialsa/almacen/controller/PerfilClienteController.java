package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Usuario;
import com.vialsa.almacen.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class PerfilClienteController {

    private final UsuarioService usuarioService;

    public PerfilClienteController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // =====================================
    // DESACTIVAR VALIDACIONES PARA USUARIO
    // =====================================
    @InitBinder("usuario")
    public void disableValidation(WebDataBinder binder) {
        binder.setValidator(null);   // ⬅ DESACTIVA VALIDACIÓN DE @NotBlank/@NotNull
    }

    // =====================================
    // MOSTRAR PERFIL DEL CLIENTE
    // =====================================
    @GetMapping("/tienda/perfil")
    public String perfilCliente(Model model) {

        Usuario usuario = usuarioService.obtenerUsuarioActual();

        if (usuario == null) {
            return "redirect:/tienda?login";
        }

        model.addAttribute("usuario", usuario);
        return "tienda/perfil-cliente";
    }

    // =====================================
    // ACTUALIZAR PERFIL
    // =====================================
    @PostMapping("/tienda/perfil/actualizar")
    public String actualizarPerfil(
            @ModelAttribute Usuario usuario,
            @RequestParam(required = false) String nuevaContrasena,
            @RequestParam(required = false) String confirmarContrasena
    ) {

        Usuario usuarioActual = usuarioService.obtenerUsuarioActual();

        if (usuarioActual == null) {
            return "redirect:/tienda?login";
        }

        // 🔒 FORZAR QUE SIEMPRE SE EDITA EL USUARIO LOGUEADO
        usuario.setIdUsuario(usuarioActual.getIdUsuario());

        // 🔒 COMPLETAR CAMPOS OBLIGATORIOS QUE EL FORMULARIO NO ENVÍA
        usuario.setNombreUsuario(usuarioActual.getNombreUsuario());
        usuario.setNroDocumento(usuarioActual.getNroDocumento());
        usuario.setIdRol(usuarioActual.getIdRol());
        usuario.setActivo(usuarioActual.getActivo());
        usuario.setFoto(usuarioActual.getFoto());

        // =============================
        // Validación de contraseña
        // =============================
        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {

            if (!nuevaContrasena.equals(confirmarContrasena)) {
                return "redirect:/tienda/perfil?errorPass";
            }

            if (!nuevaContrasena.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$")) {
                return "redirect:/tienda/perfil?errorPass";
            }

            usuario.setContrasena(nuevaContrasena);

        } else {
            usuario.setContrasena(null); // dejar null -> servicio mantiene la actual
        }

        usuarioService.actualizarPerfil(usuario);

        return "redirect:/tienda/perfil?exito";
    }

}
