package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.CuotaCompra;
import com.vialsa.almacen.model.DetalleCompra;
import com.vialsa.almacen.service.CompraService;
import com.vialsa.almacen.service.CuotaCompraService;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

// 🔥 IMPORTS JAVA CORRECTOS
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

// 🔥 IMPORTS iText CORRECTOS (NO USAR *)
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Image;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;

// Imports que te faltan para Excel con estilos POI
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;


// 🔥 IMPORTS PDF CORRECTOS
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import com.vialsa.almacen.model.Producto;


import java.awt.Color;


/**
 * Controlador de compras con validación fuerte (back-end)
 * y soporte para errores dentro del modal (opción C).
 */
@Controller
@RequestMapping("/compras")
public class CompraController {

    private final CompraService compraService;
    private final DetalleCompraService detalleCompraService;
    private final ProductoService productoService;
    private final ProveedorService proveedorService;
    private final UnidadService unidadService;
    private final CuotaCompraService cuotaCompraService;

    public CompraController(
            CompraService compraService,
            DetalleCompraService detalleCompraService,
            ProductoService productoService,
            ProveedorService proveedorService,
            UnidadService unidadService,
            CuotaCompraService cuotaCompraService
    ) {
        this.compraService = compraService;
        this.detalleCompraService = detalleCompraService;
        this.productoService = productoService;
        this.proveedorService = proveedorService;
        this.unidadService = unidadService;
        this.cuotaCompraService = cuotaCompraService;
    }

    // =========================================================
    // LISTAR
    // =========================================================
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("titulo", "Compras | VIALSA");
        model.addAttribute("compras", compraService.listar());
        return "compras/list";
    }

    // =========================================================
    // NUEVA COMPRA
    // =========================================================
    @GetMapping("/nueva")
    public String nueva(Model model) {

        Compra compra = new Compra();
        compra.setMoneda("PEN");
        compra.setPorcentajeIgv(new BigDecimal("18.00"));
        compra.setFormaPago("CONTADO");
        compra.setIncluyeIgv(Boolean.TRUE);

        compra.setNroOrdenCompra(compraService.generarNumeroOrdenCompra());

        model.addAttribute("titulo", "Registrar Compra | VIALSA");
        model.addAttribute("compraForm", compra);
        model.addAttribute("detallesCompra", null);
        cargarCombos(model);

        return "compras/form";
    }

    // =========================================================
// GUARDAR NUEVA COMPRA
// =========================================================
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("compraForm") Compra compra,
            @RequestParam("idProveedor") Integer idProveedor,
            @RequestParam("idProducto") List<Integer> idProductos,
            @RequestParam("idUnidad") List<Integer> idUnidades,
            @RequestParam("cantidad") List<String> cantidades,
            @RequestParam("precioUnitario") List<String> precios,
            @RequestParam("descuento") List<String> descuentos,
            @RequestParam(value = "fechaCuota", required = false) List<String> fechasCuotas,
            @RequestParam(value = "montoCuota", required = false) List<String> montosCuotas,
            @RequestParam("accion") String accion,
            Model model
    ) {

        // ===========================
        // Usuario autenticado
        // ===========================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String nombreUsuario = auth != null ? auth.getName() : "anonimo";

        Integer idUsuario = compraService.obtenerIdUsuarioPorNombre(nombreUsuario);
        compra.setIdProveedor(idProveedor);
        compra.setIdUsuario(idUsuario);
        compra.setFechaCompra(LocalDateTime.now());


        // ===========================
        // Estado según acción
        // ===========================
        if ("BORRADOR".equalsIgnoreCase(accion)) {
            compra.setEstado("BORRADOR");
        } else {
            if ("CONTADO".equalsIgnoreCase(compra.getFormaPago())) {
                compra.setEstado("PAGADA");
                compra.setDeuda(BigDecimal.ZERO);
            } else {
                compra.setEstado("REGISTRADA");
            }
        }

        boolean esDefinitiva =
                compra.getEstado().equalsIgnoreCase("REGISTRADA") ||
                        compra.getEstado().equalsIgnoreCase("PAGADA");

        if (compra.getPorcentajeIgv() == null) {
            compra.setPorcentajeIgv(new BigDecimal("18.00"));
        }


        // ===========================
        // VALIDACIÓN BACK-END
        // ===========================
        List<String> erroresGlobales = new ArrayList<>();
        Map<String, String> erroresCampos = new HashMap<>();

        boolean valido = validarCompra(
                compra,
                idProductos, idUnidades,
                cantidades, precios, descuentos,
                fechasCuotas, montosCuotas,
                erroresGlobales, erroresCampos
        );

        if (!valido) {
            model.addAttribute("titulo", "Registrar Compra | VIALSA");
            model.addAttribute("compraForm", compra);
            model.addAttribute("detallesCompra",
                    construirDetallesParaFormulario(idProductos, idUnidades, cantidades, precios, descuentos));
            model.addAttribute("erroresGlobales", erroresGlobales);
            model.addAttribute("erroresCampos", erroresCampos);
            cargarCombos(model);
            return "compras/form";
        }


        // ===========================
        // VALIDACIÓN: PRECIO COMPRA < PRECIO VENTA
        // ===========================
        for (int i = 0; i < idProductos.size(); i++) {

            Integer idProd = idProductos.get(i);
            BigDecimal precioCompra = new BigDecimal(precios.get(i));

            Producto producto = productoService.obtener(idProd);

            if (producto != null && producto.getPrecioUnitario() != null) {
                BigDecimal precioVenta = producto.getPrecioUnitario();

                if (precioCompra.compareTo(precioVenta) >= 0) {
                    erroresGlobales.add(
                            "El precio de compra del producto '" + producto.getNombreProducto() +
                                    "' (" + precioCompra + ") no puede ser mayor o igual al precio de venta actual (" + precioVenta + ")."
                    );

                    model.addAttribute("titulo", "Registrar Compra | VIALSA");
                    model.addAttribute("compraForm", compra);
                    model.addAttribute("detallesCompra",
                            construirDetallesParaFormulario(idProductos, idUnidades, cantidades, precios, descuentos));
                    cargarCombos(model);
                    return "compras/form";
                }
            }
        }


        // ===========================
        // Cálculo de totales
        // ===========================
        calcularTotales(compra, cantidades, precios, descuentos);


        // ===========================
        // GUARDAR COMPRA + DETALLES
        // ===========================
        try {

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

                // ===== SUMAR STOCK SOLO SI LA COMPRA ES DEFINITIVA =====
                if (esDefinitiva) {
                    productoService.aumentarStock(idProductos.get(i), d.getCantidad());
                }
            }


            // ===========================
            // CUOTAS (solo crédito)
            // ===========================
            if ("CREDITO".equalsIgnoreCase(compra.getFormaPago())
                    && fechasCuotas != null && montosCuotas != null) {

                for (int i = 0; i < fechasCuotas.size(); i++) {
                    cuotaCompraService.crearCuota(
                            idCompra,
                            i + 1,
                            LocalDate.parse(fechasCuotas.get(i)),
                            new BigDecimal(montosCuotas.get(i))
                    );
                }
            }

            return esDefinitiva
                    ? "redirect:/compras?success=true"
                    : "redirect:/compras?borrador=true";

        } catch (Exception e) {
            e.printStackTrace();
            erroresGlobales.add("Ocurrió un error inesperado al guardar la compra.");

            model.addAttribute("erroresGlobales", erroresGlobales);
            model.addAttribute("erroresCampos", erroresCampos);
            cargarCombos(model);
            model.addAttribute("detallesCompra",
                    construirDetallesParaFormulario(idProductos, idUnidades, cantidades, precios, descuentos));

            return "compras/form";
        }
    }

    // =========================================================
    // DETALLE DE COMPRA
    // =========================================================
    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable int id, Model model) {

        Compra compra = compraService.buscarPorId(id);
        if (compra == null) return "redirect:/compras?error=CompraNoEncontrada";

        List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);
        model.addAttribute("compra", compra);
        model.addAttribute("detalles", detalles);

        return "compras/detalle";
    }

    // =========================================================
    // EDITAR COMPRA (BORRADOR)
    // =========================================================
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable int id, Model model) {

        Compra compra = compraService.buscarPorId(id);
        if (compra == null) return "redirect:/compras?error=CompraNoEncontrada";

        if (!"BORRADOR".equalsIgnoreCase(compra.getEstado()))
            return "redirect:/compras?error=SoloBorradorEditable";

        model.addAttribute("titulo", "Editar Compra | VIALSA");
        model.addAttribute("compraForm", compra);
        model.addAttribute("detallesCompra", compraService.listarDetallesPorCompra(id));
        cargarCombos(model);

        return "compras/form";
    }

    // =========================================================
    // EDITAR Y GUARDAR
    // =========================================================
    @PostMapping("/editar/{id}")
    public String actualizar(
            @PathVariable int id,
            @ModelAttribute("compraForm") Compra compra,
            @RequestParam("idProveedor") Integer idProveedor,
            @RequestParam("idProducto") List<Integer> idProductos,
            @RequestParam("idUnidad") List<Integer> idUnidades,
            @RequestParam("cantidad") List<String> cantidades,
            @RequestParam("precioUnitario") List<String> precios,
            @RequestParam("descuento") List<String> descuentos,

            @RequestParam(value = "fechaCuota", required = false) List<String> fechasCuotas,
            @RequestParam(value = "montoCuota", required = false) List<String> montosCuotas,

            @RequestParam("accion") String accion,
            Model model
    ) {

        Compra actual = compraService.buscarPorId(id);
        if (actual == null || !"BORRADOR".equalsIgnoreCase(actual.getEstado()))
            return "redirect:/compras?errorEditar=true";

        compra.setIdCompra(id);
        compra.setIdUsuario(actual.getIdUsuario());
        compra.setFechaCompra(actual.getFechaCompra());
        compra.setFechaEmision(actual.getFechaEmision());
        compra.setIdProveedor(idProveedor);
        compra.setEstado("BORRADOR");

        if (compra.getPorcentajeIgv() == null) {
            compra.setPorcentajeIgv(new BigDecimal("18.00"));
        }

        List<String> erroresGlobales = new ArrayList<>();
        Map<String, String> erroresCampos = new HashMap<>();

        boolean valido = validarCompra(
                compra,
                idProductos, idUnidades,
                cantidades, precios, descuentos,
                fechasCuotas, montosCuotas,
                erroresGlobales, erroresCampos
        );

        if (!valido) {
            model.addAttribute("titulo", "Editar Compra | VIALSA");
            model.addAttribute("compraForm", compra);
            model.addAttribute("detallesCompra",
                    construirDetallesParaFormulario(idProductos, idUnidades, cantidades, precios, descuentos));
            model.addAttribute("erroresGlobales", erroresGlobales);
            model.addAttribute("erroresCampos", erroresCampos);
            cargarCombos(model);
            return "compras/form";
        }

        // Recalcular totales
        calcularTotales(compra, cantidades, precios, descuentos);

        try {
            // Actualizar cabecera
            compraService.actualizarCabecera(compra);

            // Reemplazar detalles
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

            // Reemplazar cuotas
            cuotaCompraService.eliminarPorCompra(id);

            if ("CREDITO".equalsIgnoreCase(compra.getFormaPago()) &&
                    fechasCuotas != null && montosCuotas != null) {

                for (int i = 0; i < fechasCuotas.size(); i++) {
                    LocalDate fecha = LocalDate.parse(fechasCuotas.get(i));
                    BigDecimal monto = new BigDecimal(montosCuotas.get(i));

                    cuotaCompraService.crearCuota(
                            id,
                            i + 1,
                            fecha,
                            monto
                    );
                }
            }

            // Confirmar compra si el usuario eligió REGISTRAR
            if ("REGISTRAR".equalsIgnoreCase(accion)) {

                List<DetalleCompra> nuevos = compraService.listarDetallesPorCompra(id);

                for (DetalleCompra d : nuevos) {
                    productoService.aumentarStock(d.getIdProducto(), d.getCantidad());
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

    // =========================================================
    // ANULAR / CONFIRMAR / ELIMINAR
    // =========================================================
    @GetMapping("/anular/{id}")
    public String anular(@PathVariable int id) {
        return compraService.anularCompra(id)
                ? "redirect:/compras?anulada=true"
                : "redirect:/compras?errorAnular=true";
    }

    @GetMapping("/confirmar/{id}")
    public String confirmar(@PathVariable int id) {
        try {
            Compra compra = compraService.buscarPorId(id);

            if (compra == null || !"BORRADOR".equalsIgnoreCase(compra.getEstado()))
                return "redirect:/compras?errorConfirmar=true";

            List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);
            for (DetalleCompra d : detalles)
                productoService.aumentarStock(d.getIdProducto(), d.getCantidad());

            compraService.confirmarCompra(id);

            return "redirect:/compras?confirmada=true";

        } catch (Exception e) {
            return "redirect:/compras?errorConfirmar=true";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) {
        return compraService.eliminarSiBorrador(id)
                ? "redirect:/compras?eliminada=true"
                : "redirect:/compras?errorEliminar=true";
    }

    // =========================================================
// EXPORTAR EXCEL PROFESIONAL
// =========================================================
    @GetMapping("/excel/{id}")
    public void exportarExcel(@PathVariable int id, HttpServletResponse response) throws IOException {

        Compra compra = compraService.buscarPorId(id);
        if (compra == null) {
            response.sendRedirect("/compras?error=CompraNoEncontrada");
            return;
        }

        List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);

        Workbook wb = new XSSFWorkbook();
        Sheet sh = wb.createSheet("Compra " + id);
        int rowIndex = 0;

        // ============================
        // ESTILOS
        // ============================
        // Fuente negrita
        org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
        boldFont.setBold(true);

        // Estilo header gris
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(boldFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Estilo general con bordes
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);

        // Estilo moneda
        CellStyle currencyStyle = wb.createCellStyle();
        currencyStyle.cloneStyleFrom(cellStyle);
        currencyStyle.setDataFormat(
                wb.createDataFormat().getFormat(
                        compra.getMoneda().equals("USD") ? "$ #,##0.00" : "S/ #,##0.00"
                )
        );

        // ============================
        // INFORMACIÓN PRINCIPAL
        // ============================
        Row info1 = sh.createRow(rowIndex++);
        info1.createCell(0).setCellValue("Proveedor:");
        info1.createCell(1).setCellValue(compra.getNombreProveedor());

        Row info2 = sh.createRow(rowIndex++);
        info2.createCell(0).setCellValue("Comprobante:");
        info2.createCell(1).setCellValue(compra.getTipoComprobante() + " " + compra.getSerie() + "-" + compra.getNumero());

        Row info3 = sh.createRow(rowIndex++);
        info3.createCell(0).setCellValue("Fecha:");
        info3.createCell(1).setCellValue(compra.getFechaEmision() != null
                ? compra.getFechaEmision().toString()
                : compra.getFechaCompra().toString());

        Row info4 = sh.createRow(rowIndex++);
        info4.createCell(0).setCellValue("Moneda:");
        info4.createCell(1).setCellValue(compra.getMoneda());

        if ("USD".equalsIgnoreCase(compra.getMoneda())) {
            Row info5 = sh.createRow(rowIndex++);
            info5.createCell(0).setCellValue("Tipo Cambio:");
            info5.createCell(1).setCellValue(compra.getTipoCambio().doubleValue());
        }

        rowIndex++; // Espacio

        // ============================
        // TABLA DETALLES
        // ============================
        Row header = sh.createRow(rowIndex++);
        String[] cols = {"Producto", "Unidad", "Cantidad", "Precio Unitario", "Descuento", "Subtotal"};

        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        for (DetalleCompra d : detalles) {
            Row row = sh.createRow(rowIndex++);

            BigDecimal subtotal = d.getCantidad().multiply(d.getPrecioUnitario()).subtract(d.getDescuento());

            row.createCell(0).setCellValue(d.getNombreProducto());
            row.createCell(1).setCellValue(d.getNombreUnidad());
            row.createCell(2).setCellValue(d.getCantidad().doubleValue());

            Cell precio = row.createCell(3);
            precio.setCellValue(d.getPrecioUnitario().doubleValue());
            precio.setCellStyle(currencyStyle);

            Cell desc = row.createCell(4);
            desc.setCellValue(d.getDescuento().doubleValue());
            desc.setCellStyle(currencyStyle);

            Cell sub = row.createCell(5);
            sub.setCellValue(subtotal.doubleValue());
            sub.setCellStyle(currencyStyle);
        }

        // ============================
        // TOTALES
        // ============================
        Row tot1 = sh.createRow(rowIndex++);
        tot1.createCell(4).setCellValue("Subtotal:");
        Cell t1 = tot1.createCell(5);
        t1.setCellValue(compra.getSubtotal().doubleValue());
        t1.setCellStyle(currencyStyle);

        Row tot2 = sh.createRow(rowIndex++);
        tot2.createCell(4).setCellValue("IGV:");
        Cell t2 = tot2.createCell(5);
        t2.setCellValue(compra.getMontoIgv().doubleValue());
        t2.setCellStyle(currencyStyle);

        Row tot3 = sh.createRow(rowIndex++);
        tot3.createCell(4).setCellValue("TOTAL:");
        Cell t3 = tot3.createCell(5);
        t3.setCellValue(compra.getTotalCompra().doubleValue());
        t3.setCellStyle(currencyStyle);

        // Ajuste automático
        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);

        // ============================
        // SEGUNDA HOJA SI ES USD -> EQUIVALENTE A SOLES
        // ============================
        if ("USD".equalsIgnoreCase(compra.getMoneda())) {

            Sheet sh2 = wb.createSheet("Equivalente PEN");
            int r = 0;

            Row title = sh2.createRow(r++);
            title.createCell(0).setCellValue("CONVERSIÓN A SOLES (PEN)");

            BigDecimal tc = compra.getTipoCambio();
            BigDecimal subPen = compra.getSubtotal().multiply(tc);
            BigDecimal igvPen = compra.getMontoIgv().multiply(tc);
            BigDecimal totPen = compra.getTotalCompra().multiply(tc);

            sh2.createRow(r++).createCell(0).setCellValue("Subtotal PEN: S/ " + subPen.setScale(2, RoundingMode.HALF_UP));
            sh2.createRow(r++).createCell(0).setCellValue("IGV PEN: S/ " + igvPen.setScale(2, RoundingMode.HALF_UP));
            sh2.createRow(r++).createCell(0).setCellValue("TOTAL PEN: S/ " + totPen.setScale(2, RoundingMode.HALF_UP));

            sh2.autoSizeColumn(0);
        }

        // ============================
        // DESCARGA
        // ============================
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=compra-" + id + ".xlsx");

        wb.write(response.getOutputStream());
        wb.close();
    }

    // =========================================================
    // MÉTODOS PRIVADOS — TOTALES / VALIDACIÓN / COMBOS
    // =========================================================
    private void calcularTotales(Compra compra,
                                 List<String> cantidades,
                                 List<String> precios,
                                 List<String> descuentos) {

        BigDecimal suma = BigDecimal.ZERO;

        for (int i = 0; i < cantidades.size(); i++) {

            BigDecimal cant = new BigDecimal(cantidades.get(i));
            BigDecimal prec = new BigDecimal(precios.get(i));
            BigDecimal desc = new BigDecimal(descuentos.get(i));

            BigDecimal parcial = cant.multiply(prec).subtract(desc);

            // Nunca permitir negativo por línea
            if (parcial.compareTo(BigDecimal.ZERO) < 0) {
                parcial = BigDecimal.ZERO;
            }

            suma = suma.add(parcial);
        }

        if (suma.compareTo(BigDecimal.ZERO) < 0) {
            suma = BigDecimal.ZERO;
        }

        BigDecimal porcIgv = compra.getPorcentajeIgv() != null
                ? compra.getPorcentajeIgv()
                : BigDecimal.ZERO;

        BigDecimal tasa = porcIgv.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        BigDecimal subtotal;
        BigDecimal montoIgv;
        BigDecimal total;

        if (Boolean.TRUE.equals(compra.isIncluyeIgv())) {
            total = suma;
            subtotal = total.divide(BigDecimal.ONE.add(tasa), 2, RoundingMode.HALF_UP);
            montoIgv = total.subtract(subtotal);
        } else {
            subtotal = suma.setScale(2, RoundingMode.HALF_UP);
            montoIgv = subtotal.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
            total = subtotal.add(montoIgv);
        }

        compra.setSubtotal(subtotal);
        compra.setMontoIgv(montoIgv);
        compra.setTotalCompra(total);
    }

    private void cargarCombos(Model model) {
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("proveedores", proveedorService.listarActivos());
        model.addAttribute("unidades", unidadService.listar());
    }

    private boolean validarCompra(
            Compra compra,
            List<Integer> idProductos,
            List<Integer> idUnidades,
            List<String> cantidades,
            List<String> precios,
            List<String> descuentos,
            List<String> fechasCuotas,
            List<String> montosCuotas,
            List<String> erroresGlobales,
            Map<String, String> erroresCampos
    ) {

        boolean ok = true;

        // -------- CABECERA --------
        if (compra.getIdProveedor() == null || compra.getIdProveedor() <= 0) {
            erroresCampos.put("idProveedor", "Debe seleccionar un proveedor.");
            erroresGlobales.add("Proveedor es obligatorio.");
            ok = false;
        }

        if (esVacio(compra.getTipoComprobante())) {
            erroresCampos.put("tipoComprobante", "Debe seleccionar un tipo de comprobante.");
            erroresGlobales.add("Tipo de comprobante es obligatorio.");
            ok = false;
        }

        if (esVacio(compra.getSerie())) {
            erroresCampos.put("serie", "La serie es obligatoria.");
            erroresGlobales.add("Serie es obligatoria.");
            ok = false;
        }

        if (esVacio(compra.getNumero())) {
            erroresCampos.put("numero", "El número es obligatorio.");
            erroresGlobales.add("Número es obligatorio.");
            ok = false;
        }

        if (esVacio(compra.getMoneda())) {
            erroresCampos.put("moneda", "Debe seleccionar una moneda.");
            erroresGlobales.add("Moneda es obligatoria.");
            ok = false;
        } else if ("USD".equalsIgnoreCase(compra.getMoneda())) {
            if (compra.getTipoCambio() == null ||
                    compra.getTipoCambio().compareTo(BigDecimal.ZERO) <= 0) {
                erroresCampos.put("tipoCambio", "Para moneda USD el tipo de cambio es obligatorio y mayor a 0.");
                erroresGlobales.add("Tipo de cambio inválido para USD.");
                ok = false;
            }
        }

        if (esVacio(compra.getFormaPago())) {
            erroresCampos.put("formaPago", "Debe seleccionar una forma de pago.");
            erroresGlobales.add("Forma de pago es obligatoria.");
            ok = false;
        }

        if (compra.getPorcentajeIgv() == null ||
                compra.getPorcentajeIgv().compareTo(BigDecimal.ZERO) < 0) {
            erroresCampos.put("porcentajeIgv", "Porcentaje de IGV inválido.");
            erroresGlobales.add("Porcentaje de IGV no puede ser negativo.");
            ok = false;
        }

        // -------- DETALLE --------
        if (idProductos == null || idProductos.isEmpty()) {
            erroresGlobales.add("Debe agregar al menos un producto a la compra.");
            erroresCampos.put("idProducto", "Agregue al menos un producto.");
            ok = false;
        } else {
            BigDecimal suma = BigDecimal.ZERO;

            for (int i = 0; i < idProductos.size(); i++) {

                Integer idProd = idProductos.get(i);
                Integer idUnd = idUnidades.get(i);
                String scant = cantidades.get(i);
                String spre  = precios.get(i);
                String sdesc = descuentos.get(i);

                if (idProd == null || idProd <= 0) {
                    erroresCampos.put("idProducto", "Hay filas con producto sin seleccionar.");
                    ok = false;
                }

                if (idUnd == null || idUnd <= 0) {
                    erroresCampos.put("idUnidad", "Hay filas con unidad sin seleccionar.");
                    ok = false;
                }

                BigDecimal cant = parseBigDecimal(scant, "cantidad", erroresCampos, erroresGlobales);
                BigDecimal prec = parseBigDecimal(spre,  "precioUnitario", erroresCampos, erroresGlobales);
                BigDecimal desc = parseBigDecimal(sdesc, "descuento", erroresCampos, erroresGlobales);

                if (cant == null || cant.compareTo(BigDecimal.ZERO) <= 0) {
                    erroresCampos.put("cantidad", "La cantidad debe ser mayor a 0.");
                    ok = false;
                }

                if (prec == null || prec.compareTo(BigDecimal.ZERO) < 0) {
                    erroresCampos.put("precioUnitario", "El precio no puede ser negativo.");
                    ok = false;
                }

                if (desc == null || desc.compareTo(BigDecimal.ZERO) < 0) {
                    erroresCampos.put("descuento", "El descuento no puede ser negativo.");
                    ok = false;
                }

                // ⚠️ Regla de negocio: precio de compra no puede ser mayor al precio de venta (en soles)
                if (idProd != null && idProd > 0 && prec != null && prec.compareTo(BigDecimal.ZERO) >= 0) {
                    Producto prod = productoService.obtener(idProd);

                    if (prod != null && prod.getPrecioUnitario() != null) {

                        BigDecimal precioVentaPen = prod.getPrecioUnitario(); // precio de venta en soles
                        BigDecimal precioCompraPen = prec; // inicia igual

                        // Si la compra es USD → convertir precio de compra a soles
                        if ("USD".equalsIgnoreCase(compra.getMoneda())) {
                            BigDecimal tc = compra.getTipoCambio() != null ? compra.getTipoCambio() : BigDecimal.ZERO;

                            if (tc.compareTo(BigDecimal.ZERO) > 0) {
                                precioCompraPen = prec.multiply(tc); // USD × TC = PEN
                            }
                        }

                        // Comparación correcta en soles
                        if (precioCompraPen.compareTo(precioVentaPen) >= 0) {
                            erroresGlobales.add(
                                    "El precio de compra del producto '" + prod.getNombreProducto() +
                                            "' (" + precioCompraPen.setScale(2) + " PEN) no puede ser mayor o igual " +
                                            "al precio de venta actual (" + precioVentaPen.setScale(2) + " PEN)."
                            );
                            erroresCampos.put("precioUnitario",
                                    "Precio de compra mayor que el precio de venta del producto.");
                            ok = false;
                        }
                    }
                }


                if (cant != null && prec != null && desc != null) {
                    BigDecimal parcial = cant.multiply(prec).subtract(desc);
                    if (parcial.compareTo(BigDecimal.ZERO) < 0) {
                        erroresCampos.put("descuento", "El descuento no puede superar el importe de la línea.");
                        erroresGlobales.add("Hay líneas con subtotal negativo (descuento muy alto).");
                        ok = false;
                    } else {
                        suma = suma.add(parcial);
                    }
                }
            }

            if (suma.compareTo(BigDecimal.ZERO) <= 0) {
                erroresCampos.put("totales", "El total de la compra debe ser mayor a 0.");
                erroresGlobales.add("La compra no puede tener total 0 o negativo.");
                ok = false;
            }
        }

        // -------- CRÉDITO / CUOTAS --------
        if ("CREDITO".equalsIgnoreCase(compra.getFormaPago())) {
            if (fechasCuotas == null || montosCuotas == null ||
                    fechasCuotas.isEmpty() || montosCuotas.isEmpty()) {
                erroresGlobales.add("Forma de pago CRÉDITO requiere cuotas generadas.");
                erroresCampos.put("numeroCuotasCredito", "Debe generar las cuotas.");
                ok = false;
            } else if (fechasCuotas.size() != montosCuotas.size()) {
                erroresGlobales.add("Las cuotas generadas son inconsistentes (fechas y montos no coinciden).");
                erroresCampos.put("numeroCuotasCredito", "Error en la generación de cuotas.");
                ok = false;
            } else {
                for (String m : montosCuotas) {
                    BigDecimal monto = parseBigDecimal(m, "montoCuota", erroresCampos, erroresGlobales);
                    if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                        erroresGlobales.add("Todas las cuotas deben tener monto mayor a 0.");
                        erroresCampos.put("montoCuota", "Cuota con monto inválido.");
                        ok = false;
                        break;
                    }
                }
            }
        }

        return ok;
    }

    private boolean esVacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private BigDecimal parseBigDecimal(
            String valor,
            String campo,
            Map<String, String> erroresCampos,
            List<String> erroresGlobales
    ) {
        try {
            if (valor == null || valor.trim().isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(valor);
        } catch (Exception e) {
            erroresCampos.put(campo, "Valor numérico inválido.");
            erroresGlobales.add("Hay valores numéricos inválidos en el campo " + campo + ".");
            return null;
        }
    }

    private List<DetalleCompra> construirDetallesParaFormulario(
            List<Integer> idProductos,
            List<Integer> idUnidades,
            List<String> cantidades,
            List<String> precios,
            List<String> descuentos
    ) {
        List<DetalleCompra> lista = new ArrayList<>();

        if (idProductos == null) {
            return lista;
        }

        for (int i = 0; i < idProductos.size(); i++) {
            DetalleCompra d = new DetalleCompra();
            d.setIdProducto(idProductos.get(i));
            d.setIdUnidad(idUnidades != null && idUnidades.size() > i ? idUnidades.get(i) : null);

            try {
                d.setCantidad(new BigDecimal(cantidades.get(i)));
            } catch (Exception e) {
                d.setCantidad(BigDecimal.ZERO);
            }

            try {
                d.setPrecioUnitario(new BigDecimal(precios.get(i)));
            } catch (Exception e) {
                d.setPrecioUnitario(BigDecimal.ZERO);
            }

            try {
                d.setDescuento(new BigDecimal(descuentos.get(i)));
            } catch (Exception e) {
                d.setDescuento(BigDecimal.ZERO);
            }

            lista.add(d);
        }

        return lista;
    }

    // =========================================================
    // API AJAX (Cuotas)
    // =========================================================
    @GetMapping("/cuotas/{id}")
    @ResponseBody
    public List<CuotaCompra> obtenerCuotas(@PathVariable int id) {
        return cuotaCompraService.listarPorCompra(id);
    }

    @PostMapping("/cuotas/pagar/{idCuota}")
    @ResponseBody
    public String pagarCuota(@PathVariable int idCuota) {
        cuotaCompraService.pagarCuota(idCuota);
        return "OK";
    }

    @GetMapping("/pdf/{id}")
    public void exportarPdf(@PathVariable int id, HttpServletResponse response) throws IOException {

        Compra compra = compraService.buscarPorId(id);
        if (compra == null) {
            response.sendRedirect("/compras?error=CompraNoEncontrada");
            return;
        }

        List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=compra-" + id + ".pdf");

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

    /* ============================================================
       LOGO
       ============================================================ */
        try {
            Image logo = Image.getInstance("src/main/resources/static/img/logo.png");
            logo.scaleToFit(120, 60);
            logo.setAlignment(Image.ALIGN_LEFT);
            document.add(logo);
        } catch (Exception ex) {
            // Si no hay logo, no romper
        }

    /* ============================================================
       TÍTULO EMPRESARIAL
       ============================================================ */
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph titulo = new Paragraph("COMPROBANTE DE COMPRA", titleFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingBefore(10);
        titulo.setSpacingAfter(15);
        document.add(titulo);

        document.add(new LineSeparator());

    /* ============================================================
       CABECERA DE INFORMACIÓN
       ============================================================ */
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setSpacingBefore(15);
        info.setWidths(new float[]{30, 70});

        agregarCeldaEncabezado(info, "Proveedor:");
        agregarCeldaDato(info, compra.getNombreProveedor());

        agregarCeldaEncabezado(info, "Comprobante:");
        agregarCeldaDato(info, compra.getTipoComprobante() + " " + compra.getSerie() + "-" + compra.getNumero());

        agregarCeldaEncabezado(info, "Fecha:");
        agregarCeldaDato(info, compra.getFechaEmision() != null
                ? compra.getFechaEmision().toString()
                : compra.getFechaCompra().toString()
        );

        agregarCeldaEncabezado(info, "Moneda:");
        agregarCeldaDato(info, compra.getMoneda());

        if ("USD".equalsIgnoreCase(compra.getMoneda())) {
            agregarCeldaEncabezado(info, "Tipo Cambio:");
            agregarCeldaDato(info, compra.getTipoCambio() + "");
        }

        document.add(info);

    /* ============================================================
       TABLA DE DETALLES
       ============================================================ */
        document.add(new Paragraph("\n"));
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35, 15, 10, 15, 10, 15});

        String[] headers = {"Producto", "Unidad", "Cant.", "Precio", "Desc.", "Subtotal"};
        for (String h : headers) table.addCell(celdaHeader(h));

        for (DetalleCompra d : detalles) {

            BigDecimal subtotal = d.getCantidad().multiply(d.getPrecioUnitario()).subtract(d.getDescuento());
            subtotal = subtotal.max(BigDecimal.ZERO);

            table.addCell(celdaNormal(d.getNombreProducto()));
            table.addCell(celdaNormal(d.getNombreUnidad()));
            table.addCell(celdaNormal(d.getCantidad().toPlainString()));

            table.addCell(celdaNormal(formatearMoneda(compra.getMoneda(), d.getPrecioUnitario())));
            table.addCell(celdaNormal(formatearMoneda(compra.getMoneda(), d.getDescuento())));
            table.addCell(celdaNormal(formatearMoneda(compra.getMoneda(), subtotal)));
        }

        document.add(table);

    /* ============================================================
       TOTALES PRINCIPALES
       ============================================================ */
        PdfPTable tot = new PdfPTable(2);
        tot.setWidthPercentage(40);
        tot.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tot.setSpacingBefore(15);
        tot.setWidths(new float[]{40, 60});

        tot.addCell(celdaLabel("Subtotal:"));
        tot.addCell(celdaValor(formatearMoneda(compra.getMoneda(), compra.getSubtotal())));

        tot.addCell(celdaLabel("IGV:"));
        tot.addCell(celdaValor(formatearMoneda(compra.getMoneda(), compra.getMontoIgv())));

        tot.addCell(celdaLabelBold("TOTAL:"));
        tot.addCell(celdaValorBold(formatearMoneda(compra.getMoneda(), compra.getTotalCompra())));

        document.add(tot);

    /* ============================================================
       EQUIVALENTE EN SOLES (si moneda = USD)
       ============================================================ */
        if ("USD".equalsIgnoreCase(compra.getMoneda())) {
            document.add(new Paragraph("\n"));

            PdfPTable pen = new PdfPTable(2);
            pen.setWidthPercentage(40);
            pen.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pen.setWidths(new float[]{40, 60});

            BigDecimal tc = compra.getTipoCambio();
            BigDecimal subPen = compra.getSubtotal().multiply(tc);
            BigDecimal igvPen = compra.getMontoIgv().multiply(tc);
            BigDecimal totPen = compra.getTotalCompra().multiply(tc);

            pen.addCell(celdaLabel("Subtotal PEN:"));
            pen.addCell(celdaValor("S/ " + subPen.setScale(2, RoundingMode.HALF_UP)));


            pen.addCell(celdaLabel("IGV PEN:"));
            pen.addCell(celdaValor("S/ " + igvPen.setScale(2, RoundingMode.HALF_UP)));

            pen.addCell(celdaLabelBold("TOTAL PEN:"));
            pen.addCell(celdaValor("S/ " + totPen.setScale(2, RoundingMode.HALF_UP)));

            document.add(pen);
        }

    /* ============================================================
       PIE DE PÁGINA
       ============================================================ */
        document.add(new Paragraph("\n"));
        Paragraph footer = new Paragraph(
                "Documento generado por VIALSA — " + LocalDate.now(),
                new Font(Font.HELVETICA, 9, Font.ITALIC)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }
    private PdfPCell celdaHeader(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 11, Font.BOLD)));
        c.setBackgroundColor(new Color(220, 220, 220));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(5);
        return c;
    }

    private PdfPCell celdaNormal(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 10)));
        c.setPadding(5);
        return c;
    }

    private PdfPCell celdaLabel(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 10)));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(5);
        return c;
    }

    private PdfPCell celdaLabelBold(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 11, Font.BOLD)));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBackgroundColor(new Color(210, 240, 210));
        c.setPadding(5);
        return c;
    }

    private PdfPCell celdaValor(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 10)));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(5);
        return c;
    }

    private PdfPCell celdaValorBold(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 11, Font.BOLD)));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBackgroundColor(new Color(210, 240, 210));
        c.setPadding(5);
        return c;
    }

    /* ENCABEZADO GRIS */
    private void agregarCeldaEncabezado(PdfPTable t, String txt) {
        PdfPCell c = new PdfPCell(new Phrase(txt, new Font(Font.HELVETICA, 10, Font.BOLD)));
        c.setBackgroundColor(new Color(240, 240, 240));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(5);
        t.addCell(c);
    }

    private void agregarCeldaDato(PdfPTable t, String txt) {
        PdfPCell c = new PdfPCell(new Phrase(txt, new Font(Font.HELVETICA, 10)));
        c.setBackgroundColor(new Color(240, 240, 240));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(5);
        t.addCell(c);
    }

    /* Formateo de moneda */
    private String formatearMoneda(String moneda, BigDecimal valor) {
        if ("USD".equalsIgnoreCase(moneda)) return "$ " + valor.setScale(2, RoundingMode.HALF_UP);
        return "S/ " + valor.setScale(2, RoundingMode.HALF_UP);
    }



    // =========================================================
// API — OBTENER SIGUIENTE SERIE Y NÚMERO (OPCIÓN C)
// =========================================================
    @GetMapping("/next-comprobante")
    @ResponseBody
    public java.util.Map<String, String> getNextComprobante(@RequestParam("tipo") String tipo) {

        // Forzamos explícitamente java.util.Map aquí
        java.util.Map<String, String> r = compraService.obtenerSiguienteSerieYNumero(tipo);

        // fallback si el servicio no devolvió nada
        if (r == null) {
            r = new java.util.HashMap<>();
            r.put("serie",  "");
            r.put("numero", "");
        }

        return r;
    }
    @GetMapping(value = "/tipo-cambio", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> obtenerTipoCambio(@RequestParam String fecha) {
        try {
            // Si la fecha es futura → usar fecha actual
            LocalDate f = LocalDate.parse(fecha);
            if (f.isAfter(LocalDate.now())) {
                f = LocalDate.now();
            }

            String url = "https://api.apis.net.pe/v1/tipo-cambio-sunat?fecha=" + f;

            RestTemplate rest = new RestTemplate();

            Map<String, Object> respuesta = rest.getForObject(url, Map.class);

            if (respuesta == null || !respuesta.containsKey("venta")) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "NO_DATA");
                error.put("mensaje", "SUNAT no tiene tipo de cambio para la fecha " + f);
                return ResponseEntity.ok(error); // <-- No retornar 502
            }

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "SERVER_ERROR");
            return ResponseEntity.ok(error); // <-- No 500
        }
    }


}
