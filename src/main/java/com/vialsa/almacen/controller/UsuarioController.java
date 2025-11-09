package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Rol;
import com.vialsa.almacen.model.Usuario;
import com.vialsa.almacen.service.FileStorageService;
import com.vialsa.almacen.service.RolService;
import com.vialsa.almacen.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final RolService rolService;
    private final FileStorageService fileStorageService;

    public UsuarioController(UsuarioService usuarioService,
                             RolService rolService,
                             FileStorageService fileStorageService) {
        this.usuarioService = usuarioService;
        this.rolService = rolService;
        this.fileStorageService = fileStorageService;
    }

    // 📋 Listado de usuarios (tabla + modales)
    @GetMapping
    public String listar(Model model) {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        List<Rol> roles = rolService.listarRoles();   // para el <select> del modal

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", roles);
        model.addAttribute("titulo", "Usuarios del Sistema");

        return "usuarios/list";
    }

    // ➕ Nuevo usuario
    // Ya NO usamos una vista completa; simplemente volvemos a la lista
    // y el modal "Nuevo usuario" se abre con JS.
    @GetMapping("/nuevo")
    public String nuevo() {
        return "redirect:/usuarios";
    }

    // 💾 Guardar nuevo usuario (desde el modal "Nuevo")
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario usuario,
                          @RequestParam(value = "fotoFile", required = false) MultipartFile fotoFile) {

        // Foto nueva (si se envía)
        if (fotoFile != null && !fotoFile.isEmpty()) {
            String nombreArchivo = fileStorageService.guardarFotoUsuario(fotoFile);
            usuario.setFoto(nombreArchivo);
        }

        // Si activo viene nulo, por defecto lo dejamos activo
        if (usuario.getActivo() == null) {
            usuario.setActivo(true);
        }

        usuarioService.guardar(usuario);
        return "redirect:/usuarios?guardado";
    }

    // ❌ Vista completa /editar/{id} ya no se usa (todo va por modal)
    // Si quieres, puedes borrar también usuarios/edit.html
    /*
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario == null) {
            return "redirect:/usuarios?noEncontrado";
        }
        List<Rol> roles = rolService.listarRoles();
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", roles);
        return "usuarios/edit";
    }
    */

    /**
     * 🔹 API JSON para el modal
     * GET /usuarios/{id}  → devuelve Usuario en JSON (sin contraseña)
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Usuario> obtenerUsuarioJson(@PathVariable Integer id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        // Nunca exponer la contraseña al front
        usuario.setContrasena(null);

        return ResponseEntity.ok(usuario);
    }

    // 💾 Actualizar (desde el modal "Editar")
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Usuario usuario,
                             @RequestParam(value = "fotoFile", required = false) MultipartFile fotoFile) {

        // Recuperar el usuario original para preservar campos sensibles
        Usuario original = usuarioService.buscarPorId(usuario.getIdUsuario());
        if (original == null) {
            return "redirect:/usuarios?noEncontrado";
        }

        // ❌ No se cambia contraseña aquí
        usuario.setContrasena(original.getContrasena());

        // Foto: por defecto mantenemos la actual
        String nombreFoto = original.getFoto();

        // Si suben una nueva, la guardamos
        if (fotoFile != null && !fotoFile.isEmpty()) {
            String nuevaFoto = fileStorageService.guardarFotoUsuario(fotoFile);
            if (nuevaFoto != null) {
                nombreFoto = nuevaFoto;
            }
        }
        usuario.setFoto(nombreFoto);

        // Si no viene activo en el form, preservamos
        if (usuario.getActivo() == null) {
            usuario.setActivo(original.getActivo());
        }

        usuarioService.actualizar(usuario);
        return "redirect:/usuarios?actualizado";
    }

    // 🗑️ Eliminar usuario
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        usuarioService.eliminar(id);
        return "redirect:/usuarios?eliminado";
    }

    // 👤 Perfil (cada usuario ve el suyo y ahí SÍ puede cambiar su contraseña)
    @GetMapping("/perfil")
    public String perfil(Model model) {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        model.addAttribute("usuario", usuario);
        return "usuarios/perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@ModelAttribute Usuario usuario) {
        usuarioService.actualizarPerfil(usuario); // aquí sí se controla el cambio de contraseña
        return "redirect:/usuarios/perfil?exito";
    }

    // ⏹️ DESACTIVAR usuario
    @GetMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Integer id) {
        usuarioService.cambiarEstadoActivo(id, false);
        return "redirect:/usuarios?estadoCambiado";
    }

    // ▶️ ACTIVAR usuario
    @GetMapping("/activar/{id}")
    public String activar(@PathVariable Integer id) {
        usuarioService.cambiarEstadoActivo(id, true);
        return "redirect:/usuarios?estadoCambiado";
    }
}
