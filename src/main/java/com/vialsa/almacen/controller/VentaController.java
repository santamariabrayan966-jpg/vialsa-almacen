package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Venta;
import com.vialsa.almacen.model.DetalleVenta;
import com.vialsa.almacen.service.VentaService;
import com.vialsa.almacen.service.DetalleVentaService;
import com.vialsa.almacen.service.ProductoService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final DetalleVentaService detalleVentaService;
    private final ProductoService productoService;

    public VentaController(VentaService ventaService,
                           DetalleVentaService detalleVentaService,
                           ProductoService productoService) {
        this.ventaService = ventaService;
        this.detalleVentaService = detalleVentaService;
        this.productoService = productoService;
    }

    // 📋 Listar todas las ventas
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("titulo", "Ventas | VIALSA");
        model.addAttribute("ventas", ventaService.listar());
        return "ventas/list";
    }

    // 🆕 Formulario para registrar una nueva venta
    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("titulo", "Nueva Venta | VIALSA");
        model.addAttribute("ventaForm", new Venta());
        model.addAttribute("productos", productoService.listar());
        return "ventas/form";
    }

    // 💾 Guardar venta con sus detalles y descontar stock
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Venta venta,
            @RequestParam("idCliente") int idCliente,
            @RequestParam("idProducto") List<Integer> idProductos,
            @RequestParam("idUnidad") List<Integer> idUnidades,
            @RequestParam("cantidad") List<String> cantidades,
            @RequestParam("precioUnitario") List<String> precios,
            @RequestParam("descuento") List<String> descuentos) {

        try {
            // 🧠 1️⃣ Obtener usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String nombreUsuario = (auth != null) ? auth.getName() : "anonimo";
            Integer idUsuario = ventaService.obtenerIdUsuarioPorNombre(nombreUsuario);

            if (idUsuario == null) {
                throw new IllegalStateException("No se encontró el usuario autenticado en la base de datos.");
            }

            // 🧾 2️⃣ Completar datos de la venta
            venta.setIdCliente(idCliente);
            venta.setIdUsuario(idUsuario);

            // 💾 3️⃣ Registrar la venta principal y obtener el ID generado
            int idVentaGenerado = ventaService.registrarYObtenerId(venta);

            // 🧩 4️⃣ Registrar los detalles de la venta y actualizar stock
            for (int i = 0; i < idProductos.size(); i++) {
                BigDecimal cantidad = new BigDecimal(cantidades.get(i));
                BigDecimal precio = new BigDecimal(precios.get(i));
                BigDecimal descuento = new BigDecimal(descuentos.get(i));

                DetalleVenta detalle = new DetalleVenta();
                detalle.setIdVenta(idVentaGenerado);
                detalle.setIdProducto(idProductos.get(i));
                detalle.setIdUnidad(idUnidades.get(i));
                detalle.setCantidad(cantidad);
                detalle.setPrecioUnitario(precio);
                detalle.setDescuento(descuento);

                detalleVentaService.registrar(detalle);

                // ✅ Descontar stock del producto vendido
                productoService.descontarStock(idProductos.get(i), cantidad);
            }

            // ✅ 5️⃣ Redirigir con mensaje de éxito
            return "redirect:/ventas?success=true";

        } catch (Exception e) {
            System.err.println("❌ Error al registrar venta: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/ventas?error=true";
        }
    }

    // 🔍 Ver detalle de una venta específica
    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable("id") int id, Model model) {
        Venta venta = ventaService.buscarPorId(id);
        if (venta == null) {
            return "redirect:/ventas?error=VentaNoEncontrada";
        }

        List<DetalleVenta> detalles = detalleVentaService.listarPorVenta(id);
        model.addAttribute("titulo", "Detalle de Venta | VIALSA");
        model.addAttribute("venta", venta);
        model.addAttribute("detalles", detalles);

        return "ventas/detalle";
    }
}
