package com.dentia.controller.interno;

import com.dentia.model.interno.Usuario;
import com.dentia.service.interno.RolService;
import com.dentia.service.interno.UsuarioService;
import com.dentia.service.shared.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/interno/usuarios")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioService usuarios;
    private final RolService roles;
    private final FileStorageService archivos;

    public UsuarioController(UsuarioService u, RolService r, FileStorageService a) {
        this.usuarios = u;
        this.roles = r;
        this.archivos = a;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Integer rol,
                         @RequestParam(required = false) Integer estado,
                         Model m) {
        m.addAttribute("usuarios", usuarios.buscarUsuarios(q, rol, estado));
        m.addAttribute("roles", roles.listarRolesInternos());
        m.addAttribute("usuario", new Usuario());
        m.addAttribute("q", q);
        m.addAttribute("rolFiltro", rol);
        m.addAttribute("estadoFiltro", estado);
        return "interno/usuarios/list";
    }

    @GetMapping(value = "/{id}/datos", produces = "application/json")
    @ResponseBody
    public Usuario obtenerDatos(@PathVariable Integer id) {
        Usuario u = usuarios.buscarPorId(id);
        if (u != null) {
            u.setContrasena(null);
        }
        return u;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model m) {
        return formulario(new Usuario(), "Nuevo usuario", m);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model m) {
        Usuario u = usuarios.buscarPorId(id);
        if (u == null) return "redirect:/interno/usuarios?noEncontrado";
        u.setContrasena(null);
        return formulario(u, "Editar usuario", m);
    }

    @GetMapping("/{id}/perfil")
    public String perfil(@PathVariable Integer id, Model m) {
        Usuario u = usuarios.buscarPorId(id);
        if (u == null) return "redirect:/interno/usuarios?noEncontrado";
        u.setContrasena(null);
        m.addAttribute("usuario", u);
        return "interno/usuarios/perfil";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario u,
                          @RequestParam(required = false) MultipartFile fotoFile,
                          @RequestParam(defaultValue = "false") boolean eliminarFoto,
                          @RequestParam(required = false) String nuevaContrasena,
                          RedirectAttributes ra) {
        try {
            Usuario original = u.getIdUsuario() == null ? null : usuarios.buscarPorId(u.getIdUsuario());
            String foto = eliminarFoto ? null : (original == null ? null : original.getFoto());
            if (fotoFile != null && !fotoFile.isEmpty()) {
                foto = archivos.guardarFotoUsuario(fotoFile);
            }
            if (u.getNombres() != null) u.setNombres(u.getNombres().trim().toUpperCase());
            if (u.getApellidos() != null) u.setApellidos(u.getApellidos().trim().toUpperCase());
            u.setFoto(foto);

            if (original == null) {
                u.setContrasena(nuevaContrasena);
                usuarios.guardarUsuarioInterno(u);
            } else {
                usuarios.actualizarUsuarioInterno(u, nuevaContrasena, original.getContrasena());
            }

            ra.addFlashAttribute("ok", "Usuario guardado correctamente.");
            return "redirect:/interno/usuarios";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/interno/usuarios";
        } catch (DataIntegrityViolationException ex) {
            log.warn("No se pudo guardar el usuario por un dato duplicado o una restriccion de la base de datos");
            ra.addFlashAttribute("error", "No se pudo guardar: el usuario, DNI o correo ya está registrado.");
            return "redirect:/interno/usuarios";
        } catch (RuntimeException ex) {
            log.error("No se pudo guardar el usuario", ex);
            ra.addFlashAttribute("error", "No se pudo guardar el usuario. Revisa los datos e inténtalo de nuevo.");
            return "redirect:/interno/usuarios";
        }
    }

    @PostMapping("/{id}/estado")
    public String estado(@PathVariable Integer id, @RequestParam Integer estado, RedirectAttributes ra) {
        try {
            usuarios.cambiarEstadoUsuario(id, estado);
            ra.addFlashAttribute("ok", "Estado actualizado.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("No se pudo actualizar el estado del usuario", ex);
            ra.addFlashAttribute("error", "No se pudo actualizar el estado. Inténtalo nuevamente.");
        }
        return "redirect:/interno/usuarios";
    }

    @PostMapping("/{id}/contrasena")
    public String password(@PathVariable Integer id, @RequestParam String nuevaContrasena, RedirectAttributes ra) {
        try {
            usuarios.restablecerContrasena(id, nuevaContrasena);
            ra.addFlashAttribute("ok", "Contraseña restablecida de forma segura.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("No se pudo restablecer la contrasena del usuario", ex);
            ra.addFlashAttribute("error", "No se pudo actualizar la contraseña. Inténtalo nuevamente.");
        }
        return "redirect:/interno/usuarios/" + id + "/perfil";
    }

    private String formulario(Usuario u, String titulo, Model m) {
        m.addAttribute("usuario", u);
        m.addAttribute("roles", roles.listarRolesInternos());
        m.addAttribute("titulo", titulo);
        return "interno/usuarios/form";
    }
}
