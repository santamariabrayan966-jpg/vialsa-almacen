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

    // ➕ Nuevo rol
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("rol", new Rol());
        model.addAttribute("titulo", "Nuevo Rol");
        return "roles/form";
    }

    // 🔹 API JSON para editar desde modal
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
     */
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Rol rol) {

        int id = rol.getIdRol(); // 0 = nuevo

        if (id == 0) {
            rolService.crearRol(rol);
        } else {
            rolService.actualizarRol(rol);
        }
        return "redirect:/roles";
    }

    // ✏️ Editar (vista completa)
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

    // 💾 Actualizar (manual)
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Rol rol) {
        rolService.actualizarRol(rol);
        return "redirect:/roles";
    }

    // 🗑️ Eliminar rol
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) {
        rolService.eliminarRol(id);
        return "redirect:/roles?eliminado";
    }

    // ⏹️ Desactivar rol
    @GetMapping("/desactivar/{id}")
    public String desactivarRol(@PathVariable int id) {
        rolService.cambiarEstadoActivo(id, false);
        return "redirect:/roles?estadoCambiado";
    }

    // ▶️ Activar rol
    @GetMapping("/activar/{id}")
    public String activarRol(@PathVariable int id) {
        rolService.cambiarEstadoActivo(id, true);
        return "redirect:/roles?estadoCambiado";
    }

    // 🔑 Editar permisos del rol
    @GetMapping("/permisos/{id}")
    public String editarPermisos(@PathVariable int id, Model model) {
        Rol rol = rolService.obtenerPorId(id);
        if (rol == null) {
            return "redirect:/roles";
        }

        List<Permiso> permisos = permisoService.obtenerPorRol(id);

        if (permisoService.obtenerPorRol(id).isEmpty()) {
            permisos = List.of(
                    crearPermisoBase(id, "productos"),
                    crearPermisoBase(id, "compras"),
                    crearPermisoBase(id, "ventas"),
                    crearPermisoBase(id, "inventario"),
                    crearPermisoBase(id, "proveedores"),
                    crearPermisoBase(id, "usuarios"),
                    crearPermisoBase(id, "roles"),
                    crearPermisoBase(id, "dashboard"),
                    crearPermisoBase(id, "clientes")
            );

        }

        model.addAttribute("rol", rol);
        model.addAttribute("permisos", permisos);
        model.addAttribute("titulo", "Permisos del Rol");
        return "roles/permisos";
    }

    // ======================
// 📌 Obtener permisos vía AJAX
// ======================
    @GetMapping("/permisos/ajax/{id}")
    @ResponseBody
    public List<Permiso> obtenerPermisosAjax(@PathVariable int id) {

        List<Permiso> permisos = permisoService.obtenerPorRol(id);

        // Lista base con proveedores incluido
        List<String> modulosBase = List.of(
                "productos",
                "compras",
                "ventas",
                "inventario",
                "proveedores",   // ← AQUI
                "usuarios",
                "roles",
                "dashboard",
                "clientes"
        );

        // Agregar permisos faltantes
        for (String modulo : modulosBase) {

            boolean existe = permisos.stream()
                    .anyMatch(p -> p.getModulo().equalsIgnoreCase(modulo));

            if (!existe) {
                permisos.add(crearPermisoBase(id, modulo));
            }
        }

        return permisos;
    }


    // ==============================
// 📌 Guardar permisos vía AJAX
// ==============================
    @PostMapping("/permisos/guardar/{id}")
    @ResponseBody
    public ResponseEntity<?> guardarPermisosAjax(
            @PathVariable int id,
            @RequestBody List<String> accesos) {

        // Módulos base
        List<String> modulos = List.of(
                "productos",
                "compras",
                "ventas",
                "inventario",
                "proveedores",
                "usuarios",
                "roles",
                "dashboard",
                "clientes"
        );

        // Construir lista de permisos
        List<Permiso> permisos = modulos.stream().map(m -> {
            Permiso p = new Permiso();
            p.setIdRol(id);
            p.setModulo(m);
            p.setPuedeAcceder(accesos.contains(m));
            return p;
        }).toList();

        permisoService.guardarPermisos(id, permisos);

        return ResponseEntity.ok("Permisos guardados");
    }



    private Permiso crearPermisoBase(int idRol, String modulo) {
        Permiso p = new Permiso();
        p.setIdRol(idRol);
        p.setModulo(modulo);
        p.setPuedeAcceder(false);
        return p;
    }

}
