package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Producto;
import com.vialsa.almacen.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;



@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    // ============================================================
    // LISTAR (principal)
    // ============================================================
    @GetMapping
    public String listar(@RequestParam(value = "buscar", required = false) String filtro,
                         Model model) {

        model.addAttribute("titulo", "Gestión de Productos | VIALSA");

        model.addAttribute("productos",
                (filtro != null && !filtro.isBlank())
                        ? service.buscar(filtro)
                        : service.listar()
        );

        // Si viene con datos (errores), respetamos ese form
        if (!model.containsAttribute("productoForm")) {
            model.addAttribute("productoForm", new Producto());
        }

        model.addAttribute("filtro", filtro);
        return "productos/list";
    }

    // ============================================================
    // GUARDAR PRODUCTO (CREAR / EDITAR)
    // ============================================================
    @PostMapping
    public String guardar(
            @Valid @ModelAttribute("productoForm") Producto form,
            BindingResult br,
            @RequestParam(value = "fotoArchivo", required = false) MultipartFile imagen,
            RedirectAttributes redirectAttrs
    ) {

        if (br.hasErrors()) {
            redirectAttrs.addFlashAttribute("productoForm", form);
            redirectAttrs.addFlashAttribute("modalError", true);
            redirectAttrs.addFlashAttribute("mensaje", "Hay errores en el formulario.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/productos";
        }

        try {
            // === Asegurar que descuentoMaximo no sea nulo ===
            if (form.getDescuentoMaximo() == null) {
                form.setDescuentoMaximo(BigDecimal.ZERO);
            }

            // === Validar que el descuento máximo no supere el precio unitario ===
            if (form.getDescuentoMaximo().compareTo(form.getPrecioUnitario()) > 0) {
                redirectAttrs.addFlashAttribute("productoForm", form);
                redirectAttrs.addFlashAttribute("modalError", true);
                redirectAttrs.addFlashAttribute("mensaje", "El descuento máximo no puede ser mayor al precio unitario.");
                redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
                return "redirect:/productos";
            }

            // ← Si no sube foto nueva, mantener la anterior
            if ((imagen == null || imagen.isEmpty()) && form.getIdProducto() != null) {
                Producto existente = service.obtener(form.getIdProducto());
                if (existente != null) {
                    form.setFoto(existente.getFoto());
                }
            }

            // ← Si sube nueva foto, guardarla
            if (imagen != null && !imagen.isEmpty()) {
                String nombre = System.currentTimeMillis() + "_" + imagen.getOriginalFilename();
                Path ruta = Paths.get("src/main/resources/static/img/productos/" + nombre);
                Files.copy(imagen.getInputStream(), ruta, StandardCopyOption.REPLACE_EXISTING);
                form.setFoto(nombre);
            }

            // === Guardar todo incluyendo el descuento ===
            service.guardar(form);

            redirectAttrs.addFlashAttribute("mensaje", "Producto guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "success");
            return "redirect:/productos";

        } catch (Exception ex) {
            ex.printStackTrace();
            redirectAttrs.addFlashAttribute("productoForm", form);
            redirectAttrs.addFlashAttribute("modalError", true);
            redirectAttrs.addFlashAttribute("mensaje", "Error inesperado al guardar el producto.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/productos";
        }
    }



    // ============================================================
    // API: obtener por ID (para abrir modal de edición)
    // ============================================================
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiObtener(@PathVariable Integer id) {

        Producto p = service.obtener(id);
        if (p == null) return ResponseEntity.notFound().build();

        BigDecimal ultimo = service.obtenerUltimoPrecioCompra(id);

        Map<String, Object> resp = new HashMap<>();
        resp.put("producto", p);
        resp.put("ultimoPrecioCompra", ultimo);

        return ResponseEntity.ok(resp);
    }



    // ============================================================
    // ELIMINAR PRODUCTO
    // ============================================================
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Integer id,
                           RedirectAttributes redirectAttrs) {

        Producto p = service.obtener(id);
        if (p == null) {
            redirectAttrs.addFlashAttribute("mensaje", "El producto no existe.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "warning");
            return "redirect:/productos";
        }

        try {
            service.eliminar(id);
            redirectAttrs.addFlashAttribute("mensaje", "Producto eliminado correctamente.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", "No se pudo eliminar el producto.");
            redirectAttrs.addFlashAttribute("tipoMensaje", "danger");
        }

        return "redirect:/productos";
    }

    // ============================================================
    // API: generar código interno
    // ============================================================
    @GetMapping("/api/codigo")
    @ResponseBody
    public ResponseEntity<String> apiCodigo(@RequestParam int tipo) {
        if (tipo <= 0) return ResponseEntity.badRequest().body("Tipo inválido");
        return ResponseEntity.ok(service.generarCodigoInterno(tipo));
    }

    // ============================================================
    // API: cambiar estado (activar / desactivar)
    // ============================================================
    @PostMapping("/{id}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(
            @PathVariable("id") Integer id,
            @RequestParam("estado") Integer estado) {

        Map<String, Object> body = new HashMap<>();

        try {
            boolean ok = service.cambiarEstado(id, estado);
            if (!ok) return ResponseEntity.badRequest().build();

            body.put("id", id);
            body.put("estado", estado);
            body.put("estadoTexto", estado == 1 ? "Activo" : "Inactivo");

            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
    // ============================================================
// AUTOCOMPLETADO PARA SELECT2 (SIN REST CONTROLLER)
// URL: /productos/buscar?term=xxx
// ============================================================
    @GetMapping("/buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarProductosAjax(@RequestParam("term") String term) {

        var lista = service.buscar(term == null ? "" : term.trim());
        List<Map<String, Object>> respuesta = new ArrayList<>();

        for (var p : lista) {
            Map<String, Object> item = new HashMap<>();

            item.put("id", p.getIdProducto());
            item.put("text", p.getNombreProducto());
            item.put("precio", p.getPrecioUnitario());
            item.put("stock", p.getStockActual());
            item.put("img", p.getFoto());
            item.put("codigo", p.getCodigoInterno());

            // 🔥 El frontend espera "idUnidad", NO "unidad"
            item.put("idUnidad", p.getIdUnidad());

            // Nombre visible
            String unidadNombre = switch (p.getIdUnidad()) {
                case 2 -> "Unidad";
                case 4 -> "Plancha";
                default -> "Otro";
            };
            item.put("unidadNombre", unidadNombre);

            // Descuento máximo
            item.put("descuentoMaximo", p.getDescuentoMaximo());

            item.put("img", p.getFoto());

            respuesta.add(item);
        }

        return respuesta;
    }


}
