package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Permiso;
import com.vialsa.almacen.model.Rol;
import com.vialsa.almacen.service.PermisoService;
import com.vialsa.almacen.service.RolService;
import com.vialsa.almacen.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/roles")
public class RolController {

    private final RolService rolService;
    private final PermisoService permisoService;
    private final UsuarioService usuarioService;

    public RolController(RolService rolService,
                         PermisoService permisoService,
                         UsuarioService usuarioService) {
        this.rolService = rolService;
        this.permisoService = permisoService;
        this.usuarioService = usuarioService;
    }

    // 📋 Listar roles (activos + inactivos)
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("roles", rolService.listarRoles());
        model.addAttribute("titulo", "Gestión de Roles");
        return "roles/list";
    }

    // ➕ Nuevo rol (vista completa opcional)
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("rol", new Rol());
        model.addAttribute("titulo", "Nuevo Rol");
        return "roles/form";
    }

    // 🔹 API JSON para el modal
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Rol> obtenerRolJson(@PathVariable int id) {
        Rol rol = rolService.obtenerPorId(id);
        if (rol == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rol);
    }

    /**
     * 💾 Guardar rol (nuevo o existente)
     * - idRol == 0 → crear
     * - idRol > 0  → actualizar
     */
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Rol rol) {

        int id = rol.getIdRol(); // int primitivo, 0 = nuevo

        if (id == 0) {
            rolService.crearRol(rol);
        } else {
            rolService.actualizarRol(rol);
        }
        return "redirect:/roles";
    }

    // ✏️ Editar rol (vista completa, si la sigues usando)
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable int id, Model model) {
        Rol rol = rolService.obtenerPorId(id);
        if (rol == null) {
            return "redirect:/roles";
        }
        model.addAttribute("rol", rol);
        model.addAttribute("titulo", "Editar Rol");
        return "roles/edit";
    }

    // 💾 Actualizar (endpoint clásico, opcional)
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Rol rol) {
        rolService.actualizarRol(rol);
        return "redirect:/roles";
    }

    // 🗑️ Eliminar = BORRAR rol + desactivar usuarios del rol
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) {
        // Primero desactivamos usuarios que tienen este rol
        usuarioService.cambiarEstadoActivoPorRol(id, false);
        // Luego eliminamos el rol físicamente
        rolService.eliminarRol(id);
        return "redirect:/roles?eliminado";
    }

    // ⏹️ Desactivar rol (soft delete) + desactivar usuarios del rol
    @GetMapping("/desactivar/{id}")
    public String desactivarRol(@PathVariable int id) {
        rolService.cambiarEstadoActivo(id, false);
        usuarioService.cambiarEstadoActivoPorRol(id, false);
        return "redirect:/roles?estadoCambiado";
    }

    // ▶️ Activar rol
    @GetMapping("/activar/{id}")
    public String activarRol(@PathVariable int id) {
        rolService.cambiarEstadoActivo(id, true);

        // Si quieres reactivar también usuarios del rol, descomenta:
        // usuarioService.cambiarEstadoActivoPorRol(id, true);

        return "redirect:/roles?estadoCambiado";
    }

    // 🔑 Editar permisos
    @GetMapping("/permisos/{id}")
    public String editarPermisos(@PathVariable int id, Model model) {
        Rol rol = rolService.obtenerPorId(id);
        if (rol == null) {
            return "redirect:/roles";
        }

        List<Permiso> permisos = permisoService.obtenerPorRol(id);

        if (permisos.isEmpty()) {
            permisos = List.of(
                    crearPermisoBase(id, "productos"),
                    crearPermisoBase(id, "compras"),
                    crearPermisoBase(id, "ventas"),
                    crearPermisoBase(id, "inventario"),
                    crearPermisoBase(id, "usuarios"),
                    crearPermisoBase(id, "roles"),
                    crearPermisoBase(id, "dashboard")
            );
        }

        model.addAttribute("rol", rol);
        model.addAttribute("permisos", permisos);
        model.addAttribute("titulo", "Permisos del Rol");
        return "roles/permisos";
    }

    // 💾 Guardar permisos
    @PostMapping("/permisos/guardar/{id}")
    public String guardarPermisos(@PathVariable int id,
                                  @RequestParam List<String> modulos,
                                  @RequestParam(required = false) List<String> accesos) {

        List<Permiso> permisos = modulos.stream().map(m -> {
            Permiso p = new Permiso();
            p.setIdRol(id);
            p.setModulo(m);
            p.setPuedeAcceder(accesos != null && accesos.contains(m));
            return p;
        }).toList();

        permisoService.guardarPermisos(id, permisos);
        return "redirect:/roles";
    }

    private Permiso crearPermisoBase(int idRol, String modulo) {
        Permiso p = new Permiso();
        p.setIdRol(idRol);
        p.setModulo(modulo);
        p.setPuedeAcceder(false);
        return p;
    }
}
