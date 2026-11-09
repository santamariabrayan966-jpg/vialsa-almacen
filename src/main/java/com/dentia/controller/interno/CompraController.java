package com.dentia.controller.interno;

import com.dentia.model.interno.Compra;
import com.dentia.model.interno.CuotaCompra;
import com.dentia.model.interno.DetalleCompra;
import com.dentia.service.interno.CompraService;
import com.dentia.service.interno.CuotaCompraService;
import com.dentia.service.interno.DetalleCompraService;
import com.dentia.service.interno.ProductoService;
import com.dentia.service.interno.ProveedorService;
import com.dentia.service.interno.UnidadService;
import com.dentia.service.interno.SedeService;
import com.dentia.service.interno.CompraRecepcionService;
import com.dentia.model.interno.CompraRecepcion;
import com.dentia.model.interno.CompraRecepcionDetalle;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
import java.util.LinkedHashMap;

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



import java.awt.Color;


/**
 * Controlador de compras con validación fuerte (back-end)
 * y soporte para errores dentro del modal (opción C).
 */
@Controller
@RequestMapping("/interno/compras")
public class CompraController {

    private final CompraService compraService;
    private final DetalleCompraService detalleCompraService;
    private final ProductoService productoService;
    private final ProveedorService proveedorService;
    private final UnidadService unidadService;
    private final CuotaCompraService cuotaCompraService;
    private final SedeService sedeService;
    private final CompraRecepcionService compraRecepcionService;

    public CompraController(
            CompraService compraService,
            DetalleCompraService detalleCompraService,
            ProductoService productoService,
            ProveedorService proveedorService,
            UnidadService unidadService,
            CuotaCompraService cuotaCompraService,
            SedeService sedeService,
            CompraRecepcionService compraRecepcionService
    ) {
        this.compraService = compraService;
        this.detalleCompraService = detalleCompraService;
        this.productoService = productoService;
        this.proveedorService = proveedorService;
        this.unidadService = unidadService;
        this.cuotaCompraService = cuotaCompraService;
        this.sedeService = sedeService;
        this.compraRecepcionService = compraRecepcionService;
    }

    // =========================================================
    // LISTAR
    // =========================================================
    @GetMapping
    public String listar(Model model, HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            model.addAttribute("titulo", "Compras | DentIA");
            model.addAttribute("compras", List.of());
            model.addAttribute("errorSedeActiva", "No se encontró una sede activa en tu sesión.");
            return "interno/compras/list";
        }

        model.addAttribute("titulo", "Compras | DentIA");
        model.addAttribute("compras", compraService.listarPorSede(sedeActivaId));
        model.addAttribute("sedeListadoNombre", session.getAttribute("sedeActivaNombre"));

        return "interno/compras/list";
    }

    // =========================================================
    // NUEVA COMPRA
    // =========================================================
    @GetMapping("/nueva")
    public String nueva(Model model, HttpSession session) {

        Compra compra = new Compra();
        compra.setMoneda("PEN");
        compra.setPorcentajeIgv(new BigDecimal("18.00"));
        compra.setFormaPago("CONTADO");
        compra.setIncluyeIgv(Boolean.TRUE);
        compra.setTipoRecepcion("PENDIENTE_GUIA");
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/dashboard?sinSedeActiva";
        }

        compra.setIdSede(sedeActivaId);

        compra.setNroOrdenCompra(compraService.generarNumeroOrdenCompra());

        model.addAttribute("titulo", "Registrar Compra | DentIA");
        model.addAttribute("compraForm", compra);
        model.addAttribute("detallesCompra", null);
        cargarCombos(model, sedeActivaId);

        return "interno/compras/form";
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
            Model model,
            HttpSession session
    ) {

        // ===========================
        // Usuario autenticado
        // ===========================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String nombreUsuario = auth != null ? auth.getName() : "anonimo";

        Integer idUsuario = compraService.obtenerIdUsuarioPorNombre(nombreUsuario);

        Integer sedeActivaId = obtenerSedeActivaId(session);
        if (sedeActivaId == null) {
            model.addAttribute("titulo", "Registrar Compra | DentIA");
            model.addAttribute("compraForm", compra);
            model.addAttribute("detallesCompra",
                    construirDetallesParaFormulario(idProductos, idUnidades, cantidades, precios, descuentos));
            model.addAttribute("erroresGlobales", List.of("No se encontró una sede activa para registrar la compra."));
            model.addAttribute("erroresCampos", Map.of("idSede", "Debe seleccionar una sede activa desde el perfil."));
            cargarCombos(model, null);
            return "interno/compras/form";
        }

        compra.setIdProveedor(idProveedor);
        compra.setIdUsuario(idUsuario);
        compra.setIdSede(sedeActivaId);
        compra.setFechaCompra(LocalDateTime.now());


        // ===========================
        // Estado según acción
        // ===========================
        if ("BORRADOR".equalsIgnoreCase(accion)) {
            compra.setEstado("BORRADOR");
        } else {

            if ("INMEDIATA".equalsIgnoreCase(compra.getTipoRecepcion())) {
                compra.setEstado("RECIBIDA_COMPLETA");
            } else {
                compra.setEstado("REGISTRADA");
            }

            if ("CONTADO".equalsIgnoreCase(compra.getFormaPago())) {
                compra.setDeuda(BigDecimal.ZERO);
            }
        }

        boolean esDefinitiva =
                !"BORRADOR".equalsIgnoreCase(compra.getEstado());

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
            model.addAttribute("titulo", "Registrar Compra | DentIA");
            model.addAttribute("compraForm", compra);
            model.addAttribute("detallesCompra",
                    construirDetallesParaFormulario(idProductos, idUnidades, cantidades, precios, descuentos));
            model.addAttribute("erroresGlobales", erroresGlobales);
            model.addAttribute("erroresCampos", erroresCampos);
            cargarCombos(model, null);
            return "interno/compras/form";
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

                Integer idProd = obtenerEnteroSeguro(idProductos, i);
                Integer idUnd = obtenerEnteroSeguro(idUnidades, i);

                if (idProd == null || idProd <= 0 || idUnd == null || idUnd <= 0) {
                    continue;
                }

                DetalleCompra d = new DetalleCompra();
                d.setIdCompra(idCompra);
                d.setIdProducto(idProd);
                d.setIdUnidad(idUnd);
                d.setCantidad(new BigDecimal(obtenerTextoSeguro(cantidades, i, "0")));
                d.setPrecioUnitario(new BigDecimal(obtenerTextoSeguro(precios, i, "0")));
                d.setDescuento(new BigDecimal(obtenerTextoSeguro(descuentos, i, "0")));

                detalleCompraService.registrar(d);
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

            // ===========================
// RECEPCIÓN INMEDIATA
// ===========================
// Si el usuario indicó que la mercadería ya llegó completa,
// se crea una recepción automática y recién ahí se aumenta stock.
            if (esDefinitiva && "INMEDIATA".equalsIgnoreCase(compra.getTipoRecepcion())) {
                compraRecepcionService.registrarRecepcionInmediata(
                        idCompra,
                        compra.getIdUsuario(),
                        "Recepción inmediata generada al registrar la compra."
                );
            }

            return esDefinitiva
                    ? "redirect:/interno/compras?success=true"
                    : "redirect:/interno/compras?borrador=true";

        } catch (Exception e) {
            e.printStackTrace();
            erroresGlobales.add("Ocurrió un error inesperado al guardar la compra.");

            model.addAttribute("erroresGlobales", erroresGlobales);
            model.addAttribute("erroresCampos", erroresCampos);
            cargarCombos(model, sedeActivaId);
            model.addAttribute("detallesCompra",
                    construirDetallesParaFormulario(idProductos, idUnidades, cantidades, precios, descuentos));

            return "interno/compras/form";
        }
    }

    // =========================================================
    // DETALLE DE COMPRA
    // =========================================================
    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable int id, Model model) {

        Compra compra = compraService.buscarPorId(id);

        if (compra == null) {
            return "redirect:/interno/compras?error=CompraNoEncontrada";
        }

        List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);
        List<CuotaCompra> cuotas = cuotaCompraService.listarPorCompra(id);

        // ===========================
        // Resumen de pagos / crédito
        // ===========================
        BigDecimal totalPagado = BigDecimal.ZERO;
        BigDecimal totalPendiente = BigDecimal.ZERO;

        if (cuotas != null) {
            for (CuotaCompra cuota : cuotas) {
                BigDecimal monto = cuota.getMontoCuota() != null
                        ? cuota.getMontoCuota()
                        : BigDecimal.ZERO;

                if ("PAGADA".equalsIgnoreCase(cuota.getEstado())) {
                    totalPagado = totalPagado.add(monto);
                } else {
                    totalPendiente = totalPendiente.add(monto);
                }
            }
        }

        // ===========================
        // Recepciones / guías
        // ===========================
        List<CompraRecepcion> recepciones = compraRecepcionService.listarPorCompra(id);

        Map<Integer, List<CompraRecepcionDetalle>> detallesRecepcion = new LinkedHashMap<>();

        if (recepciones != null) {
            for (CompraRecepcion recepcion : recepciones) {
                detallesRecepcion.put(
                        recepcion.getIdRecepcion(),
                        compraRecepcionService.listarDetallesPorRecepcion(recepcion.getIdRecepcion())
                );
            }
        }

        List<CompraRecepcionDetalle> pendientesRecepcion =
                compraRecepcionService.listarPendientesPorCompra(id);

        // ===========================
        // Neto financiero
        // ===========================
        BigDecimal saldoFavor = compra.getSaldoFavor() != null
                ? compra.getSaldoFavor()
                : BigDecimal.ZERO;

        BigDecimal totalNeto = compra.getTotalCompra() != null
                ? compra.getTotalCompra().subtract(saldoFavor)
                : BigDecimal.ZERO;

        if (totalNeto.compareTo(BigDecimal.ZERO) < 0) {
            totalNeto = BigDecimal.ZERO;
        }

        model.addAttribute("titulo", "Detalle de Compra | DentIA");

        // Compra y productos
        model.addAttribute("compra", compra);
        model.addAttribute("detalles", detalles);

        // Crédito y pagos
        model.addAttribute("cuotas", cuotas);
        model.addAttribute("totalPagado", totalPagado);
        model.addAttribute("totalPendiente", totalPendiente);

        // Recepciones / guías
        model.addAttribute("recepciones", recepciones);
        model.addAttribute("detallesRecepcion", detallesRecepcion);
        model.addAttribute("pendientesRecepcion", pendientesRecepcion);

        // Cierre con diferencia / devolución
        model.addAttribute("saldoFavor", saldoFavor);
        model.addAttribute("totalNeto", totalNeto);

        return "interno/compras/detalle";
    }

    // =========================================================
    // EDITAR COMPRA (BORRADOR)
    // =========================================================
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable int id, Model model) {

        Compra compra = compraService.buscarPorId(id);
        if (compra == null) return "redirect:/interno/compras?error=CompraNoEncontrada";

        if (!"BORRADOR".equalsIgnoreCase(compra.getEstado()))
            return "redirect:/interno/compras?error=SoloBorradorEditable";

        model.addAttribute("titulo", "Editar Compra | DentIA");
        model.addAttribute("compraForm", compra);
        model.addAttribute("detallesCompra", compraService.listarDetallesPorCompra(id));
        cargarCombos(model, compra.getIdSede());

        return "interno/compras/form";
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
            return "redirect:/interno/compras?errorEditar=true";

        compra.setIdCompra(id);
        compra.setIdUsuario(actual.getIdUsuario());
        compra.setIdSede(actual.getIdSede());
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
            model.addAttribute("titulo", "Editar Compra | DentIA");
            model.addAttribute("compraForm", compra);
            model.addAttribute("detallesCompra",
                    construirDetallesParaFormulario(idProductos, idUnidades, cantidades, precios, descuentos));
            model.addAttribute("erroresGlobales", erroresGlobales);
            model.addAttribute("erroresCampos", erroresCampos);
            cargarCombos(model, actual.getIdSede());
            return "interno/compras/form";
        }

        // Recalcular totales
        calcularTotales(compra, cantidades, precios, descuentos);

        try {
            // Actualizar cabecera
            compraService.actualizarCabecera(compra);

            // Reemplazar detalles
            detalleCompraService.eliminarPorCompra(id);

            for (int i = 0; i < idProductos.size(); i++) {

                Integer idProd = obtenerEnteroSeguro(idProductos, i);
                Integer idUnd = obtenerEnteroSeguro(idUnidades, i);

                if (idProd == null || idProd <= 0 || idUnd == null || idUnd <= 0) {
                    continue;
                }

                DetalleCompra d = new DetalleCompra();
                d.setIdCompra(id);
                d.setIdProducto(idProd);
                d.setIdUnidad(idUnd);
                d.setCantidad(new BigDecimal(obtenerTextoSeguro(cantidades, i, "0")));
                d.setPrecioUnitario(new BigDecimal(obtenerTextoSeguro(precios, i, "0")));
                d.setDescuento(new BigDecimal(obtenerTextoSeguro(descuentos, i, "0")));

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



                // Confirmamos la compra, pero ya no aumentamos stock directo aquí.
                compraService.confirmarCompra(id);

                // Si la proforma se registra como recepción inmediata,
                // se crea la recepción automática y recién ahí aumenta stock.
                if ("INMEDIATA".equalsIgnoreCase(compra.getTipoRecepcion())) {
                    compraRecepcionService.registrarRecepcionInmediata(
                            id,
                            compra.getIdUsuario(),
                            "Recepción inmediata generada al confirmar la proforma."
                    );
                }

                return "redirect:/interno/compras?confirmada=true";
            }

            return "redirect:/interno/compras?borradorEditado=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/interno/compras?errorEditar=true";
        }
    }

    // =========================================================
    // ANULAR / CONFIRMAR / ELIMINAR
    // =========================================================
    @PostMapping("/anular/{id}")
    public String anular(@PathVariable int id) {
        return compraService.anularCompra(id)
                ? "redirect:/interno/compras?anulada=true"
                : "redirect:/interno/compras?errorAnular=true";
    }

    @PostMapping("/confirmar/{id}")
    public String confirmar(@PathVariable int id) {
        try {
            Compra compra = compraService.buscarPorId(id);

            if (compra == null || !"BORRADOR".equalsIgnoreCase(compra.getEstado()))
                return "redirect:/interno/compras?errorConfirmar=true";

            List<DetalleCompra> detalles = compraService.listarDetallesPorCompra(id);



// Confirmar ya no aumenta stock directo.
// Solo confirma la compra.
            compraService.confirmarCompra(id);

// Si la proforma estaba marcada como recepción inmediata,
// se registra la recepción automática y recién ahí aumenta stock.
            if ("INMEDIATA".equalsIgnoreCase(compra.getTipoRecepcion())) {
                compraRecepcionService.registrarRecepcionInmediata(
                        id,
                        compra.getIdUsuario(),
                        "Recepción inmediata generada al confirmar la proforma."
                );
            }

            return "redirect:/interno/compras?confirmada=true";

        } catch (Exception e) {
            return "redirect:/interno/compras?errorConfirmar=true";
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) {
        return compraService.eliminarSiBorrador(id)
                ? "redirect:/interno/compras?eliminada=true"
                : "redirect:/interno/compras?errorEliminar=true";
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

        Row infoSede = sh.createRow(rowIndex++);
        infoSede.createCell(0).setCellValue("Sede:");
        infoSede.createCell(1).setCellValue(
                compra.getNombreSede() != null ? compra.getNombreSede() : "Sin sede"
        );

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

    private Integer obtenerSedeActivaId(HttpSession session) {
        Object sedeObj = session.getAttribute("sedeActivaId");

        if (sedeObj instanceof Integer idSede) {
            return idSede;
        }

        if (sedeObj instanceof String sedeStr) {
            try {
                return Integer.parseInt(sedeStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
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

    private void cargarCombos(Model model, Integer idSede) {

        model.addAttribute("productos", productoService.listarActivosConStockPorSede(idSede));

        model.addAttribute("proveedores", proveedorService.listarActivos());
        model.addAttribute("unidades", unidadService.listar());
        model.addAttribute("sedes", sedeService.listar());

        // En la tabla unidades, "Unidad" tiene idUnidad = 2
        model.addAttribute("idUnidadUnidad", 2);
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
        if (compra.getIdSede() == null || compra.getIdSede() <= 0) {
            erroresCampos.put("idSede", "No hay una sede activa para esta compra.");
            erroresGlobales.add("No se encontró la sede activa. Cambia o confirma tu sede desde el perfil.");
            ok = false;
        }
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

        // -------- VALIDACIÓN SERIE / NÚMERO --------
        if (!esVacio(compra.getTipoComprobante()) && !esVacio(compra.getSerie())) {

            String tipo = compra.getTipoComprobante().trim().toUpperCase();
            String serie = compra.getSerie().trim().toUpperCase();

            compra.setTipoComprobante(tipo);
            compra.setSerie(serie);

            if ("FACTURA".equals(tipo)) {
                if (!serie.matches("^F\\d+$")) {
                    erroresCampos.put("serie", "La serie de factura debe empezar con F y luego solo números. Ejemplo: F001.");
                    erroresGlobales.add("Serie inválida para FACTURA.");
                    ok = false;
                }
            }

            if ("BOLETA".equals(tipo)) {
                if (!serie.matches("^B\\d+$")) {
                    erroresCampos.put("serie", "La serie de boleta debe empezar con B y luego solo números. Ejemplo: B001.");
                    erroresGlobales.add("Serie inválida para BOLETA.");
                    ok = false;
                }
            }
        }

        if (!esVacio(compra.getNumero())) {
            String numero = compra.getNumero().trim();

            if (!numero.matches("^\\d+$")) {
                erroresCampos.put("numero", "El número solo debe contener dígitos.");
                erroresGlobales.add("Número de comprobante inválido.");
                ok = false;
            } else {
                compra.setNumero(numero);
            }
        }

// -------- PERIODO --------
        if (compra.getPeriodoMes() == null || compra.getPeriodoMes() < 1 || compra.getPeriodoMes() > 12) {
            erroresCampos.put("periodoMes", "Debe seleccionar un mes válido.");
            erroresGlobales.add("Periodo mes es obligatorio.");
            ok = false;
        }

        int anioActual = LocalDate.now().getYear();

        if (compra.getPeriodoAnio() == null || compra.getPeriodoAnio() < 2000 || compra.getPeriodoAnio() > anioActual + 1) {
            erroresCampos.put("periodoAnio", "Debe ingresar un año válido.");
            erroresGlobales.add("Periodo año es obligatorio.");
            ok = false;
        }

// -------- DUPLICADO PROVEEDOR + COMPROBANTE --------
        if (ok && compraService.existeComprobanteProveedor(compra)) {
            erroresCampos.put("numero", "Ya existe una compra registrada con este proveedor, tipo, serie y número.");
            erroresGlobales.add("El comprobante ya fue registrado para este proveedor.");
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

        if (esVacio(compra.getTipoRecepcion())) {
            erroresCampos.put("tipoRecepcion", "Debe seleccionar cómo será la recepción de la mercadería.");
            erroresGlobales.add("Debe seleccionar si la recepción será pendiente por guía o inmediata.");
            ok = false;
        } else {
            compra.setTipoRecepcion(compra.getTipoRecepcion().trim().toUpperCase());

            if (!"PENDIENTE_GUIA".equals(compra.getTipoRecepcion())
                    && !"INMEDIATA".equals(compra.getTipoRecepcion())) {
                erroresCampos.put("tipoRecepcion", "Tipo de recepción inválido.");
                erroresGlobales.add("Tipo de recepción inválido.");
                ok = false;
            }
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

                Integer idProd = obtenerEnteroSeguro(idProductos, i);
                Integer idUnd = obtenerEnteroSeguro(idUnidades, i);
                String scant = obtenerTextoSeguro(cantidades, i, "0");
                String spre  = obtenerTextoSeguro(precios, i, "0");
                String sdesc = obtenerTextoSeguro(descuentos, i, "0");

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
            if (valor == null || valor.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }

            return new BigDecimal(valor.trim());

        } catch (Exception e) {
            erroresCampos.put(campo, "Valor numérico inválido.");
            erroresGlobales.add("Hay valores numéricos inválidos en el campo " + campo + ".");
            return null;
        }
    }

    private Integer obtenerEnteroSeguro(List<Integer> lista, int index) {
        if (lista == null || index < 0 || index >= lista.size()) {
            return null;
        }
        return lista.get(index);
    }

    private String obtenerTextoSeguro(List<String> lista, int index, String valorDefecto) {
        if (lista == null || index < 0 || index >= lista.size()) {
            return valorDefecto;
        }

        String valor = lista.get(index);
        return valor == null ? valorDefecto : valor;
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

        agregarCeldaEncabezado(info, "Sede:");
        agregarCeldaDato(info, compra.getNombreSede() != null ? compra.getNombreSede() : "Sin sede");

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
                "Documento generado por DentIA — " + LocalDate.now(),
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
