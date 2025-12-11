package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Proveedor;
import com.vialsa.almacen.service.ProveedorService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    // GET /proveedores  → lista principal
    @GetMapping
    public String listar(Model model,
                         @ModelAttribute(name = "success", binding = false) String success,
                         @ModelAttribute(name = "error", binding = false) String error) {

        List<Proveedor> lista = proveedorService.listar();

        model.addAttribute("titulo", "Proveedores | VIALSA");
        model.addAttribute("proveedores", lista);

        if (success != null && !success.isBlank()) {
            model.addAttribute("success", success);
        }
        if (error != null && !error.isBlank()) {
            model.addAttribute("error", error);
        }

        return "proveedores/list";
    }

    // ENDPOINT JSON para el modal de EDITAR
    // GET /proveedores/api/{id}
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Proveedor> obtenerProveedorApi(@PathVariable Integer id) {
        Proveedor proveedor = proveedorService.buscarPorId(id);
        if (proveedor == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(proveedor);
    }

    // Vista separada (por si usas la pantalla fuera del modal)
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("titulo", "Nuevo Proveedor | VIALSA");
        model.addAttribute("proveedor", new Proveedor());
        return "proveedores/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        Proveedor proveedor = proveedorService.buscarPorId(id);
        if (proveedor == null) {
            redirectAttributes.addFlashAttribute("error", "El proveedor no existe.");
            return "redirect:/proveedores";
        }

        model.addAttribute("titulo", "Editar Proveedor | VIALSA");
        model.addAttribute("proveedor", proveedor);
        return "proveedores/form";
    }

    // POST /proveedores/guardar  (nuevo o editar desde modal o formulario)
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("proveedor") Proveedor proveedor,
                          RedirectAttributes redirectAttributes) {

        ProveedorService.ResultadoGuardarProveedor resultado =
                proveedorService.guardar(proveedor);

        switch (resultado) {
            case NUEVO -> redirectAttributes.addFlashAttribute(
                    "success", "Proveedor guardado correctamente.");

            case ACTUALIZADO -> redirectAttributes.addFlashAttribute(
                    "success", "Proveedor actualizado correctamente.");

            case REACTIVADO -> redirectAttributes.addFlashAttribute(
                    "success", "Proveedor reactivado correctamente (estaba inactivo).");

            case DUPLICADO -> redirectAttributes.addFlashAttribute(
                    "error", "Ya existe un proveedor activo con ese número de documento.");

            default -> redirectAttributes.addFlashAttribute(
                    "error", "Ocurrió un error al guardar el proveedor.");
        }

        return "redirect:/proveedores";
    }

    // GET /proveedores/eliminar/{id}
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id,
                           RedirectAttributes redirectAttributes) {

        boolean ok = proveedorService.eliminar(id);

        if (ok) {
            redirectAttributes.addFlashAttribute("success",
                    "Proveedor eliminado (inactivado) correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo inactivar/eliminar el proveedor.");
        }

        return "redirect:/proveedores";
    }
    // Cambiar estado ACTIVO / INACTIVO (para el switch del panel lateral)
    @PostMapping("/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam("activo") boolean activo) {

        Proveedor p = proveedorService.buscarPorId(id);
        if (p == null) {
            return ResponseEntity.badRequest().body("Proveedor no encontrado");
        }

        p.setActivo(activo);

        boolean ok = proveedorService.actualizarEstado(p);

        if (!ok) {
            return ResponseEntity.badRequest().body("No se pudo actualizar el estado");
        }

        return ResponseEntity.ok("OK");
    }


}
