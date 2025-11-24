package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;
import com.vialsa.almacen.service.CompraService;
import com.vialsa.almacen.service.DetalleCompraService;
import com.vialsa.almacen.service.ProductoService;
import com.vialsa.almacen.service.ProveedorService;
import com.vialsa.almacen.service.UnidadService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // ===================== LISTAR =====================

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("titulo", "Compras | VIALSA");
        model.addAttribute("compras", compraService.listar());
        return "compras/list";
    }

    // ===================== NUEVA COMPRA =====================

    @GetMapping("/nueva")
    public String nueva(Model model) {
        Compra compra = new Compra();
        // valores por defecto
        compra.setMoneda("PEN");
        compra.setPorcentajeIgv(new BigDecimal("18.00"));
        compra.setFormaPago("CONTADO");

        model.addAttribute("titulo", "Registrar Compra | VIALSA");
        model.addAttribute("compraForm", compra);
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("proveedores", proveedorService.listar());
        model.addAttribute("unidades", unidadService.listar());
        // detalles vacíos
        model.addAttribute("detallesCompra", null);

        return "compras/form";
    }

    // ===================== GUARDAR NUEVA =====================

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("compraForm") Compra compra,
                          @RequestParam("idProveedor") int idProveedor,
                          @RequestParam("idProducto") List<Integer> idProductos,
                          @RequestParam("idUnidad") List<Integer> idUnidades,
                          @RequestParam("cantidad") List<String> cantidades,
                          @RequestParam("precioUnitario") List<String> precios,
                          @RequestParam("descuento") List<String> descuentos,
                          @RequestParam("accion") String accion) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String nombreUsuario = auth != null ? auth.getName() : "anonimo";
            Integer idUsuario = compraService.obtenerIdUsuarioPorNombre(nombreUsuario);

            if (idUsuario == null) {
                throw new IllegalStateException("No se encontró el usuario autenticado en la base de datos.");
            }

            compra.setIdProveedor(idProveedor);
            compra.setIdUsuario(idUsuario);

            // Estado según botón
            if ("BORRADOR".equalsIgnoreCase(accion)) {
                compra.setEstado("BORRADOR");
            } else {
                compra.setEstado("REGISTRADA");
            }

            // porcentaje IGV por defecto
            if (compra.getPorcentajeIgv() == null) {
                compra.setPorcentajeIgv(new BigDecimal("18.00"));
            }

            // >>>>> CALCULAR TOTALES ANTES DE GUARDAR
            calcularTotales(compra, cantidades, precios, descuentos);

            // Registrar cabecera
            int idCompra = compraService.registrarYObtenerId(compra);
            boolean esRegistrada = "REGISTRADA".equalsIgnoreCase(compra.getEstado());

            // Registrar detalle
            for (int i = 0; i < idProductos.size(); i++) {
                DetalleCompra d = new DetalleCompra();
                d.setIdCompra(idCompra);
                d.setIdProducto(idProductos.get(i));
                d.setIdUnidad(idUnidades.get(i));
                d.setCantidad(new BigDecimal(cantidades.get(i)));
                d.setPrecioUnitario(new BigDecimal(precios.get(i)));
                d.setDescuento(new BigDecimal(descuentos.get(i)));

                detalleCompraService.registrar(d);

                // Afectar stock solo si REGISTRADA
                if (esRegistrada) {
                    productoService.aumentarStock(idProductos.get(i), new BigDecimal(cantidades.get(i)));
                }
            }

            if (esRegistrada) {
                return "redirect:/compras?success=true";
            } else {
                return "redirect:/compras?borrador=true";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/compras?error=true";
        }
    }

    // ===================== DETALLE =====================

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

    // ===================== EDITAR COMPLETA (BORRADOR) =====================

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable int id, Model model) {

        Compra compra = compraService.buscarPorId(id);
        if (compra == null) {
            return "redirect:/compras?error=CompraNoEncontrada";
        }

        if (!"BORRADOR".equalsIgnoreCase(compra.getEstado())) {
            return "redirect:/compras?error=SoloBorradorEditable";
        }

        List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);

        model.addAttribute("titulo", "Editar compra | VIALSA");
        model.addAttribute("compraForm", compra);
        model.addAttribute("detallesCompra", detalles);
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("proveedores", proveedorService.listar());
        model.addAttribute("unidades", unidadService.listar());

        return "compras/form";
    }

    @PostMapping("/editar/{id}")
    public String actualizarCompleta(@PathVariable int id,
                                     @ModelAttribute("compraForm") Compra compra,
                                     @RequestParam("idProveedor") int idProveedor,
                                     @RequestParam("idProducto") List<Integer> idProductos,
                                     @RequestParam("idUnidad") List<Integer> idUnidades,
                                     @RequestParam("cantidad") List<String> cantidades,
                                     @RequestParam("precioUnitario") List<String> precios,
                                     @RequestParam("descuento") List<String> descuentos,
                                     @RequestParam("accion") String accion) {

        try {
            Compra compraDb = compraService.buscarPorId(id);
            if (compraDb == null || !"BORRADOR".equalsIgnoreCase(compraDb.getEstado())) {
                return "redirect:/compras?errorEditar=true";
            }

            // preservar datos que no deben cambiar
            compra.setIdCompra(id);
            compra.setIdUsuario(compraDb.getIdUsuario());
            compra.setFechaCompra(compraDb.getFechaCompra());
            compra.setFechaEmision(compraDb.getFechaEmision());
            compra.setFechaVencimiento(compraDb.getFechaVencimiento());

            compra.setIdProveedor(idProveedor);
            compra.setEstado("BORRADOR");

            if (compra.getPorcentajeIgv() == null) {
                compra.setPorcentajeIgv(new BigDecimal("18.00"));
            }

            // >>>>> CALCULAR TOTALES ANTES DE ACTUALIZAR
            calcularTotales(compra, cantidades, precios, descuentos);

            // actualizar cabecera
            compraService.actualizarCabecera(compra);

            // reemplazar todo el detalle
            detalleCompraService.eliminarPorCompra(id);

            for (int i = 0; i < idProductos.size(); i++) {
                DetalleCompra d = new DetalleCompra();
                d.setIdCompra(id);
                d.setIdProducto(idProductos.get(i));
                d.setIdUnidad(idUnidades.get(i));
                d.setCantidad(new BigDecimal(cantidades.get(i)));
                d.setPrecioUnitario(new BigDecimal(precios.get(i)));
                d.setDescuento(new BigDecimal(descuentos.get(i)));

                detalleCompraService.registrar(d);
            }

            // Si el usuario decide REGISTRAR, confirmamos y afectamos stock
            if ("REGISTRAR".equalsIgnoreCase(accion)) {
                List<DetalleCompra> nuevosDetalles = compraService.listarDetallesPorCompra(id);
                for (DetalleCompra d : nuevosDetalles) {
                    if (d.getCantidad() != null) {
                        productoService.aumentarStock(d.getIdProducto(), d.getCantidad());
                    }
                }
                compraService.confirmarCompra(id);
                return "redirect:/compras?confirmada=true";
            }

            return "redirect:/compras?borradorEditado=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/compras?errorEditar=true";
        }
    }

    // ===================== ANULAR / CONFIRMAR / ELIMINAR =====================

    @GetMapping("/anular/{id}")
    public String anular(@PathVariable int id) {
        try {
            boolean ok = compraService.anularCompra(id);
            if (ok) {
                return "redirect:/compras?anulada=true";
            } else {
                return "redirect:/compras?errorAnular=true";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/compras?errorAnular=true";
        }
    }

    @GetMapping("/confirmar/{id}")
    public String confirmar(@PathVariable int id) {
        try {
            Compra compra = compraService.buscarPorId(id);
            if (compra == null || !"BORRADOR".equalsIgnoreCase(compra.getEstado())) {
                return "redirect:/compras?errorConfirmar=true";
            }

            List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);
            for (DetalleCompra d : detalles) {
                if (d.getCantidad() != null) {
                    productoService.aumentarStock(d.getIdProducto(), d.getCantidad());
                }
            }

            boolean ok = compraService.confirmarCompra(id);
            if (ok) {
                return "redirect:/compras?confirmada=true";
            } else {
                return "redirect:/compras?errorConfirmar=true";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/compras?errorConfirmar=true";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) {
        try {
            boolean ok = compraService.eliminarSiBorrador(id);
            if (ok) {
                return "redirect:/compras?eliminada=true";
            } else {
                return "redirect:/compras?errorEliminar=true";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/compras?errorEliminar=true";
        }
    }

    // ===================== EXCEL =====================

    @GetMapping("/excel/{id}")
    public void exportarExcel(@PathVariable int id,
                              HttpServletResponse response) throws IOException {

        Compra compra = compraService.buscarPorId(id);
        if (compra == null) {
            response.sendRedirect("/compras?error=CompraNoEncontrada");
            return;
        }
        List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Compra " + id);

        int rowIdx = 0;

        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue("Producto");
        header.createCell(1).setCellValue("Unidad");
        header.createCell(2).setCellValue("Cantidad");
        header.createCell(3).setCellValue("Precio Unitario");
        header.createCell(4).setCellValue("Descuento");
        header.createCell(5).setCellValue("Subtotal");

        for (DetalleCompra d : detalles) {
            Row row = sheet.createRow(rowIdx++);

            BigDecimal cantidad = d.getCantidad() != null ? d.getCantidad() : BigDecimal.ZERO;
            BigDecimal precio   = d.getPrecioUnitario() != null ? d.getPrecioUnitario() : BigDecimal.ZERO;
            BigDecimal desc     = d.getDescuento() != null ? d.getDescuento() : BigDecimal.ZERO;
            BigDecimal subtotal = cantidad.multiply(precio).subtract(desc);

            row.createCell(0).setCellValue(d.getNombreProducto());
            row.createCell(1).setCellValue(d.getNombreUnidad());
            row.createCell(2).setCellValue(cantidad.doubleValue());
            row.createCell(3).setCellValue(precio.doubleValue());
            row.createCell(4).setCellValue(desc.doubleValue());
            row.createCell(5).setCellValue(subtotal.doubleValue());
        }

        for (int i = 0; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }

        String fileName = "compra-" + id + ".xlsx";
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ===================== MÉTODO PRIVADO: CALCULAR TOTALES =====================

    /**
     * Calcula subtotal, IGV y totalCompra a partir de los detalles.
     */
    private void calcularTotales(Compra compra,
                                 List<String> cantidades,
                                 List<String> precios,
                                 List<String> descuentos) {

        BigDecimal sumaLineas = BigDecimal.ZERO;

        for (int i = 0; i < cantidades.size(); i++) {
            BigDecimal cant = new BigDecimal(cantidades.get(i));
            BigDecimal prec = new BigDecimal(precios.get(i));
            BigDecimal desc = new BigDecimal(descuentos.get(i));

            BigDecimal linea = cant.multiply(prec).subtract(desc);
            sumaLineas = sumaLineas.add(linea);
        }

        BigDecimal porcIgv = compra.getPorcentajeIgv() != null
                ? compra.getPorcentajeIgv()
                : new BigDecimal("18.00");
        compra.setPorcentajeIgv(porcIgv);

        BigDecimal tasa = porcIgv
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        BigDecimal subtotal;
        BigDecimal montoIgv;
        BigDecimal total;

        if (compra.isIncluyeIgv()) {
            // Los precios vienen con IGV incluido
            total = sumaLineas;
            subtotal = total
                    .divide(BigDecimal.ONE.add(tasa), 2, RoundingMode.HALF_UP);
            montoIgv = total.subtract(subtotal);
        } else {
            // Precios sin IGV
            subtotal = sumaLineas.setScale(2, RoundingMode.HALF_UP);
            montoIgv = subtotal.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
            total = subtotal.add(montoIgv);
        }

        compra.setSubtotal(subtotal);
        compra.setMontoIgv(montoIgv);
        compra.setTotalCompra(total);
    }

}
