package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;
import com.vialsa.almacen.service.CompraService;
import com.vialsa.almacen.service.DetalleCompraService;
import com.vialsa.almacen.service.ProductoService;
import com.vialsa.almacen.service.ProveedorService;
import com.vialsa.almacen.service.UnidadService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/compras")
public class CompraController {

    private final CompraService compraService;
    private final DetalleCompraService detalleCompraService;
    private final ProductoService productoService;
    private final ProveedorService proveedorService;
    private final UnidadService unidadService;

    public CompraController(CompraService compraService,
                            DetalleCompraService detalleCompraService,
                            ProductoService productoService,
                            ProveedorService proveedorService,
                            UnidadService unidadService) {
        this.compraService = compraService;
        this.detalleCompraService = detalleCompraService;
        this.productoService = productoService;
        this.proveedorService = proveedorService;
        this.unidadService = unidadService;
    }

    // 📋 Listar compras
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("titulo", "Compras | VIALSA");
        model.addAttribute("compras", compraService.listar());
        return "compras/list";
    }

    // 🆕 Formulario nueva compra
    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("titulo", "Nueva Compra | VIALSA");
        model.addAttribute("compraForm", new Compra());
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("proveedores", proveedorService.listar());
        model.addAttribute("unidades", unidadService.listar());
        return "compras/form";
    }

    // 💾 Guardar compra
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Compra compra,
                          @RequestParam("idProveedor") int idProveedor,
                          @RequestParam("idProducto") List<Integer> idProductos,
                          @RequestParam("idUnidad") List<Integer> idUnidades,
                          @RequestParam("cantidad") List<String> cantidades,
                          @RequestParam("precioUnitario") List<String> precios,
                          @RequestParam("descuento") List<String> descuentos) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String nombreUsuario = auth != null ? auth.getName() : "anonimo";
            Integer idUsuario = compraService.obtenerIdUsuarioPorNombre(nombreUsuario);

            if (idUsuario == null) {
                throw new IllegalStateException("No se encontró el usuario autenticado en la base de datos.");
            }

            compra.setIdProveedor(idProveedor);
            compra.setIdUsuario(idUsuario);
            int idCompra = compraService.registrarYObtenerId(compra);

            for (int i = 0; i < idProductos.size(); i++) {
                DetalleCompra d = new DetalleCompra();
                d.setIdCompra(idCompra);
                d.setIdProducto(idProductos.get(i));
                d.setIdUnidad(idUnidades.get(i));
                d.setCantidad(new BigDecimal(cantidades.get(i)));
                d.setPrecioUnitario(new BigDecimal(precios.get(i)));
                d.setDescuento(new BigDecimal(descuentos.get(i)));

                detalleCompraService.registrar(d);
                productoService.aumentarStock(idProductos.get(i), new BigDecimal(cantidades.get(i)));
            }

            return "redirect:/compras?success=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/compras?error=true";
        }
    }

    // 🔍 Detalle compra
    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable int id, Model model) {
        Compra compra = compraService.buscarPorId(id);
        if (compra == null) {
            return "redirect:/compras?error=CompraNoEncontrada";
        }
        List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);
        model.addAttribute("titulo", "Detalle de Compra | VIALSA");
        model.addAttribute("compra", compra);
        model.addAttribute("detalles", detalles);
        return "compras/detalle";
    }
}
