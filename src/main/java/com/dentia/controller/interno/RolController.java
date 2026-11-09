package com.dentia.controller.interno;

import com.dentia.model.interno.Permiso;
import com.dentia.model.interno.Rol;
import com.dentia.service.interno.PermisoService;
import com.dentia.service.interno.RolService;
import com.dentia.service.interno.RolValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/interno/roles")
public class RolController {
    private static final Logger log = LoggerFactory.getLogger(RolController.class);
    private static final List<String> MODULOS_BASE = List.of(
            "dashboard", "pacientes", "citas", "historia_clinica", "odontograma", "odontograma_2d", "odontograma_3d",
            "diagnosticos", "tratamientos", "plan_tratamiento", "presupuestos", "atenciones",
            "seguimiento", "documentos", "pagos", "reportes", "profesionales", "servicios", "usuarios", "roles", "configuracion");

    private final RolService rolService;
    private final PermisoService permisoService;

    public RolController(RolService rolService, PermisoService permisoService) {
        this.rolService = rolService;
        this.permisoService = permisoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("roles", rolService.listarRoles());
        model.addAttribute("titulo", "Gestion de Roles");
        return "interno/roles/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("rol", new Rol());
        model.addAttribute("titulo", "Nuevo Rol");
        return "interno/roles/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable int id, Model model) {
        Rol rol = rolService.obtenerPorId(id);
        if (rol == null) return "redirect:/interno/roles?noEncontrado";
        model.addAttribute("rol", rol);
        model.addAttribute("titulo", "Editar Rol");
        return "interno/roles/edit";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Rol> obtenerRolJson(@PathVariable int id) {
        Rol rol = rolService.obtenerPorId(id);
        return rol == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(rol);
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Rol rol) {
        try {
            if (rol != null && rol.getNombreRol() != null) {
                rol.setNombreRol(rol.getNombreRol().trim().toUpperCase());
            }
            Integer id = rol != null ? rol.getIdRol() : null;
            if (id == null || id <= 0) rolService.crearRol(rol);
            else rolService.actualizarRol(rol);
            return "redirect:/interno/roles?guardado";
        } catch (RolValidationException e) {
            log.warn("No se guardo el rol: codigo={}, detalle={}", e.getCodigo(), e.getMessage());
            return redirectValidacion(e);
        } catch (DataAccessException e) {
            log.error("Error SQL al guardar el rol nombre={}", rol == null ? null : rol.getNombreRol(), e);
            return "redirect:/interno/roles?errorTecnico";
        } catch (RuntimeException e) {
            log.error("Error inesperado al guardar un rol", e);
            return "redirect:/interno/roles?errorRol";
        }
    }

    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Rol rol) { return guardar(rol); }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) {
        try {
            rolService.eliminarRol(id);
            return "redirect:/interno/roles?eliminado";
        } catch (RolValidationException e) {
            log.warn("No se elimino administrativamente el rol {}: {}", id, e.getCodigo());
            return redirectValidacion(e);
        }
    }

    @PostMapping("/desactivar/{id}")
    public String desactivarRol(@PathVariable int id) { return cambiarEstado(id, false); }

    @PostMapping("/activar/{id}")
    public String activarRol(@PathVariable int id) { return cambiarEstado(id, true); }

    private String cambiarEstado(int id, boolean activo) {
        try {
            rolService.cambiarEstadoActivo(id, activo);
            return "redirect:/interno/roles?estadoCambiado";
        } catch (RolValidationException e) {
            log.warn("No se cambio el estado del rol {}: {}", id, e.getCodigo());
            return redirectValidacion(e);
        }
    }

    @GetMapping("/permisos/ajax/{id}")
    @ResponseBody
    public List<Permiso> obtenerPermisosAjax(@PathVariable int id) {
        List<Permiso> permisos = new ArrayList<>(permisoService.obtenerPorRol(id));
        completarPermisosFaltantes(id, permisos);
        return permisos;
    }

    @GetMapping("/permisos/{id}")
    public String editarPermisos(@PathVariable int id, Model model) {
        Rol rol = rolService.obtenerPorId(id);
        if (rol == null) return "redirect:/interno/roles?noEncontrado";
        List<Permiso> permisos = obtenerPermisosAjax(id);
        model.addAttribute("rol", rol);
        model.addAttribute("permisos", permisos);
        model.addAttribute("titulo", "Permisos del Rol");
        return "interno/roles/permisos";
    }

    @PostMapping("/permisos/guardar/{id}")
    @ResponseBody
    public ResponseEntity<?> guardarPermisosAjax(@PathVariable int id, @RequestBody List<String> accesos) {
        if (rolService.esAdmin(id)) return ResponseEntity.badRequest().body("El rol ADMIN esta protegido");
        Set<String> normalizados = accesos == null ? Set.of() : accesos.stream()
                .filter(a -> a != null && !a.isBlank()).map(a -> a.trim().toLowerCase())
                .collect(Collectors.toSet());
        List<Permiso> permisos = MODULOS_BASE.stream().map(modulo -> {
            Permiso permiso = new Permiso();
            permiso.setIdRol(id);
            permiso.setModulo(modulo);
            permiso.setPuedeAcceder(normalizados.contains(modulo));
            return permiso;
        }).toList();
        permisoService.guardarPermisos(id, permisos);
        return ResponseEntity.ok("Permisos guardados");
    }

    private void completarPermisosFaltantes(int idRol, List<Permiso> permisos) {
        for (String modulo : MODULOS_BASE) {
            if (permisos.stream().noneMatch(p -> modulo.equalsIgnoreCase(p.getModulo()))) {
                Permiso permiso = new Permiso();
                permiso.setIdRol(idRol);
                permiso.setModulo(modulo);
                permiso.setPuedeAcceder(false);
                permisos.add(permiso);
            }
        }
    }

    private String redirectValidacion(RolValidationException e) {
        return switch (e.getCodigo()) {
            case NOMBRE_REQUERIDO -> "redirect:/interno/roles?nombreRequerido";
            case NOMBRE_DUPLICADO -> "redirect:/interno/roles?duplicado";
            case ADMIN_PROTEGIDO -> "redirect:/interno/roles?adminProtegido";
            case USUARIOS_ASOCIADOS -> "redirect:/interno/roles?usuariosAsociados";
            case NO_ENCONTRADO -> "redirect:/interno/roles?noEncontrado";
        };
    }
}

