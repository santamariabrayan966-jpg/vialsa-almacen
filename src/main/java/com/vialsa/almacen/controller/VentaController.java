package com.vialsa.almacen.controller;

import com.vialsa.almacen.dao.interfaces.IDetalleVentaDao;
import com.vialsa.almacen.model.DetalleVenta;
import com.vialsa.almacen.model.Venta;
import com.vialsa.almacen.service.DetalleVentaService;
import com.vialsa.almacen.service.ProductoService;
import com.vialsa.almacen.service.UnidadService;
import com.vialsa.almacen.service.VentaService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

// PDF
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final DetalleVentaService detalleService;
    private final ProductoService productoService;
    private final UnidadService unidadService;
    private final IDetalleVentaDao detalleDao;

    public VentaController(
            VentaService ventaService,
            DetalleVentaService detalleService,
            ProductoService productoService,
            UnidadService unidadService,
            IDetalleVentaDao detalleDao
    ) {
        this.ventaService = ventaService;
        this.detalleService = detalleService;
        this.productoService = productoService;
        this.unidadService = unidadService;
        this.detalleDao = detalleDao;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("titulo", "Ventas | VIALSA");
        model.addAttribute("ventas", ventaService.listar());
        return "ventas/list";
    }

    @GetMapping("/nueva")
    public String nuevaVenta(Model model) {
        Venta venta = new Venta();
        venta.setIdVentas(null);
        venta.setEstadoVenta("BORRADOR");

        model.addAttribute("ventaForm", venta);
        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("unidades", unidadService.listar());

        return "ventas/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable int id, Model model) {

        Venta venta = ventaService.buscarPorId(id);
        if (venta == null || !"BORRADOR".equals(venta.getEstadoVenta()))
            return "redirect:/ventas?error";

        model.addAttribute("ventaForm", venta);
        model.addAttribute("detalles", detalleService.listarPorVenta(id));
        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("unidades", unidadService.listar());

        return "ventas/form";
    }

    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable int id, Model model) {

        Venta venta = ventaService.buscarPorId(id);
        if (venta == null) return "redirect:/ventas?error";

        model.addAttribute("venta", venta);
        model.addAttribute("detalles", detalleService.listarPorVenta(id));

        return "ventas/detalle";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("ventaForm") Venta venta,
                          @RequestParam(value = "idProducto", required = false) List<Integer> idProducto,
                          @RequestParam(value = "idUnidad", required = false) List<Integer> idUnidad,
                          @RequestParam(value = "cantidad", required = false) List<BigDecimal> cantidad,
                          @RequestParam(value = "precioUnitario", required = false) List<BigDecimal> precio,
                          @RequestParam(value = "descuento", required = false) List<BigDecimal> descuento,
                          @RequestParam(value = "pagoInicial", required = false) BigDecimal pagoInicial,
                          @RequestParam(value = "deudaVenta", required = false) BigDecimal deuda,
                          @RequestParam("accion") String accion) {

        // Validar cliente
        if (venta.getNombreCliente() == null || venta.getNombreCliente().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar un nombre de cliente.");
        }

        // Validar productos
        if (idProducto == null || idProducto.isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un producto.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer idUser = ventaService.obtenerIdUsuarioPorNombre(auth.getName());
        venta.setIdUsuario(idUser);

        // ============================================================
        // 🚀 SI idCliente viene vacío → crear cliente automáticamente
        // ============================================================
        if (venta.getIdCliente() == null || venta.getIdCliente() == 0) {

            Integer nuevoClienteId = ventaService.crearClienteManual(
                    venta.getDocumentoCliente(),
                    venta.getNombreCliente(),
                    venta.getTelefonoCliente()
            );

            venta.setIdCliente(nuevoClienteId);
        }

        // Crear cabecera
        int idVenta = ventaService.crearBorrador(venta);

        int detallesGuardados = 0;

        for (int i = 0; i < idProducto.size(); i++) {

            Integer unidad = (idUnidad != null && i < idUnidad.size())
                    ? idUnidad.get(i)
                    : null;

            if (unidad == null || unidad <= 0) {
                System.out.println("⚠ Fila ignorada por unidad inválida");
                continue;
            }

            DetalleVenta d = new DetalleVenta();
            d.setIdVenta(idVenta);
            d.setIdProducto(idProducto.get(i));
            d.setIdUnidad(unidad);
            d.setCantidad(cantidad.get(i));
            d.setPrecioUnitario(precio.get(i));
            d.setDescuento(descuento.get(i));

            ventaService.agregarDetalle(idVenta, d);
            detallesGuardados++;
        }

        if (detallesGuardados == 0) {
            return "redirect:/ventas?sinDetalles=true";
        }

        if ("CREDITO".equalsIgnoreCase(venta.getFormaPago())) {
            ventaService.registrarCredito(
                    idVenta,
                    pagoInicial == null ? BigDecimal.ZERO : pagoInicial,
                    deuda == null ? BigDecimal.ZERO : deuda
            );
        }

        if ("REGISTRAR".equals(accion)) {
            ventaService.confirmarVenta(idVenta);
        }

        return "redirect:/ventas?guardada=true";
    }

    @GetMapping("/confirmar/{id}")
    public String confirmarVenta(@PathVariable int id) {
        ventaService.confirmarVenta(id);
        return "redirect:/ventas?confirmada=true";
    }

    @GetMapping("/anular/{id}")
    public String anularVenta(@PathVariable int id) {
        ventaService.anularVenta(id);
        return "redirect:/ventas?anulada=true";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarBorrador(@PathVariable int id) {
        ventaService.eliminarBorrador(id);
        return "redirect:/ventas?eliminada=true";
    }

    @GetMapping("/pdf/{id}")
    public void exportarPdf(@PathVariable int id, HttpServletResponse response) throws IOException {

        Venta venta = ventaService.buscarPorId(id);
        if (venta == null) {
            response.sendRedirect("/ventas?error");
            return;
        }

        List<DetalleVenta> detalles = detalleService.listarPorVenta(id);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=venta-" + id + ".pdf");

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        document.add(new Paragraph("COMPROBANTE DE VENTA", new Font(Font.HELVETICA, 16, Font.BOLD)));
        document.add(new LineSeparator());

        document.add(new Paragraph("Cliente: " + venta.getNombreCliente()));
        document.add(new Paragraph("Fecha: " + venta.getFechaVenta()));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        table.addCell("Producto");
        table.addCell("Cantidad");
        table.addCell("Precio");
        table.addCell("Subtotal");

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVenta d : detalles) {
            BigDecimal subtotal = d.getCantidad().multiply(d.getPrecioUnitario());

            table.addCell(d.getNombreProducto());
            table.addCell(d.getCantidad().toString());
            table.addCell("S/ " + d.getPrecioUnitario());
            table.addCell("S/ " + subtotal);

            total = total.add(subtotal);
        }

        document.add(table);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("TOTAL: S/ " + total));

        document.close();
    }

    @GetMapping("/siguiente-numero")
    @ResponseBody
    public String obtenerSiguienteNumero(@RequestParam String tipo) {
        return ventaService.obtenerSiguienteComprobante(tipo);
    }

}
