package com.dentia.controller.interno;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.dentia.model.interno.Venta;
import com.dentia.service.interno.VentaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ReporteVentasController {

    private final VentaService ventaService;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReporteVentasController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    // ============================================================
    // VENTAS GENERALES
    // ============================================================

    @GetMapping("/interno/reportes/ventas/pdf")
    public void ventasGeneralesPdf(HttpServletResponse response,
                                   HttpSession session,
                                   @RequestParam(required = false) String fechaDesde,
                                   @RequestParam(required = false) String fechaHasta) throws Exception {

        RangoFechas rango = resolverRangoFechas(fechaDesde, fechaHasta);

        List<Venta> ventas = obtenerVentas(
                session,
                "TODOS",
                rango.fechaDesde(),
                rango.fechaHasta()
        );

        exportarPdf(
                response,
                ventas,
                "Reporte general de ventas",
                "ventas-generales.pdf",
                rango
        );
    }

    @GetMapping("/interno/reportes/ventas/excel")
    public void ventasGeneralesExcel(HttpServletResponse response,
                                     HttpSession session,
                                     @RequestParam(required = false) String fechaDesde,
                                     @RequestParam(required = false) String fechaHasta) throws Exception {

        RangoFechas rango = resolverRangoFechas(fechaDesde, fechaHasta);

        List<Venta> ventas = obtenerVentas(
                session,
                "TODOS",
                rango.fechaDesde(),
                rango.fechaHasta()
        );

        exportarExcel(
                response,
                ventas,
                "Ventas generales",
                "ventas-generales.xlsx",
                rango
        );
    }

    // ============================================================
    // VENTAS ONLINE
    // ============================================================

    @GetMapping("/interno/reportes/ventas-online/pdf")
    public void ventasOnlinePdf(HttpServletResponse response,
                                HttpSession session,
                                @RequestParam(required = false) String fechaDesde,
                                @RequestParam(required = false) String fechaHasta) throws Exception {

        RangoFechas rango = resolverRangoFechas(fechaDesde, fechaHasta);

        List<Venta> ventas = obtenerVentas(
                session,
                "ONLINE",
                rango.fechaDesde(),
                rango.fechaHasta()
        );

        exportarPdf(
                response,
                ventas,
                "Reporte de ventas online",
                "ventas-online.pdf",
                rango
        );
    }

    @GetMapping("/interno/reportes/ventas-online/excel")
    public void ventasOnlineExcel(HttpServletResponse response,
                                  HttpSession session,
                                  @RequestParam(required = false) String fechaDesde,
                                  @RequestParam(required = false) String fechaHasta) throws Exception {

        RangoFechas rango = resolverRangoFechas(fechaDesde, fechaHasta);

        List<Venta> ventas = obtenerVentas(
                session,
                "ONLINE",
                rango.fechaDesde(),
                rango.fechaHasta()
        );

        exportarExcel(
                response,
                ventas,
                "Ventas online",
                "ventas-online.xlsx",
                rango
        );
    }

    // ============================================================
    // VENTAS PRESENCIALES
    // ============================================================

    @GetMapping("/interno/reportes/ventas-presenciales/pdf")
    public void ventasPresencialesPdf(HttpServletResponse response,
                                      HttpSession session,
                                      @RequestParam(required = false) String fechaDesde,
                                      @RequestParam(required = false) String fechaHasta) throws Exception {

        RangoFechas rango = resolverRangoFechas(fechaDesde, fechaHasta);

        List<Venta> ventas = obtenerVentas(
                session,
                "PRESENCIAL",
                rango.fechaDesde(),
                rango.fechaHasta()
        );

        exportarPdf(
                response,
                ventas,
                "Reporte de ventas presenciales",
                "ventas-presenciales.pdf",
                rango
        );
    }

    @GetMapping("/interno/reportes/ventas-presenciales/excel")
    public void ventasPresencialesExcel(HttpServletResponse response,
                                        HttpSession session,
                                        @RequestParam(required = false) String fechaDesde,
                                        @RequestParam(required = false) String fechaHasta) throws Exception {

        RangoFechas rango = resolverRangoFechas(fechaDesde, fechaHasta);

        List<Venta> ventas = obtenerVentas(
                session,
                "PRESENCIAL",
                rango.fechaDesde(),
                rango.fechaHasta()
        );

        exportarExcel(
                response,
                ventas,
                "Ventas presenciales",
                "ventas-presenciales.xlsx",
                rango
        );
    }

    // ============================================================
    // OBTENER DATOS
    // ============================================================

    private List<Venta> obtenerVentas(HttpSession session,
                                      String canal,
                                      String fechaDesde,
                                      String fechaHasta) {

        Integer idSede = obtenerSedeActiva(session);

        if (idSede != null && idSede > 0) {
            return ventaService.filtrarPorSede(
                    idSede,
                    canal,
                    "TODOS",
                    "TODOS",
                    null,
                    fechaDesde,
                    fechaHasta
            );
        }

        return ventaService.filtrar(
                canal,
                "TODOS",
                "TODOS",
                null,
                fechaDesde,
                fechaHasta
        );
    }

    private Integer obtenerSedeActiva(HttpSession session) {
        Object valor = session.getAttribute("sedeActivaId");

        if (valor instanceof Integer id) {
            return id;
        }

        if (valor instanceof String texto) {
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private RangoFechas resolverRangoFechas(String fechaDesde, String fechaHasta) {

        LocalDate hoy = LocalDate.now();

        if (fechaDesde == null || fechaDesde.isBlank()) {
            fechaDesde = hoy.withDayOfMonth(1).toString();
        }

        if (fechaHasta == null || fechaHasta.isBlank()) {
            fechaHasta = hoy.toString();
        }

        return new RangoFechas(fechaDesde, fechaHasta);
    }

    private record RangoFechas(String fechaDesde, String fechaHasta) {
    }

    // ============================================================
    // EXPORTAR PDF
    // ============================================================

    private void exportarPdf(HttpServletResponse response,
                             List<Venta> ventas,
                             String titulo,
                             String nombreArchivo,
                             RangoFechas rango) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        Document documento = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(documento, response.getOutputStream());

        documento.open();

        Paragraph tituloPdf = new Paragraph(
                titulo,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)
        );
        tituloPdf.setAlignment(Element.ALIGN_CENTER);
        documento.add(tituloPdf);

        Paragraph subtitulo = new Paragraph(
                "DentIA - Sistema de Gestión",
                FontFactory.getFont(FontFactory.HELVETICA, 10)
        );
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(15);
        documento.add(subtitulo);

        Paragraph resumen = new Paragraph(
                "Rango: " + rango.fechaDesde() + " al " + rango.fechaHasta()
                        + "   |   Total de registros: " + ventas.size()
                        + "   |   Total vendido: S/ " + calcularTotalValido(ventas),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)
        );
        resumen.setSpacingAfter(10);
        documento.add(resumen);

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.2f, 1.4f, 2.4f, 1.2f, 1.2f, 1.2f, 1.2f});

        agregarCabeceraPdf(tabla, "Comprobante");
        agregarCabeceraPdf(tabla, "Fecha");
        agregarCabeceraPdf(tabla, "Cliente");
        agregarCabeceraPdf(tabla, "Canal");
        agregarCabeceraPdf(tabla, "Pago");
        agregarCabeceraPdf(tabla, "Estado");
        agregarCabeceraPdf(tabla, "Total");

        if (ventas == null || ventas.isEmpty()) {
            PdfPCell sinDatos = new PdfPCell(new Phrase(
                    "No hay ventas registradas en el rango seleccionado.",
                    FontFactory.getFont(FontFactory.HELVETICA, 10)
            ));
            sinDatos.setColspan(7);
            sinDatos.setHorizontalAlignment(Element.ALIGN_CENTER);
            sinDatos.setPadding(12);
            tabla.addCell(sinDatos);
        } else {
            for (Venta v : ventas) {
                tabla.addCell(valor(v.getNroComprobante()));
                tabla.addCell(v.getFechaVenta() != null ? v.getFechaVenta().format(FORMATO_FECHA) : "-");
                tabla.addCell(valor(v.getNombreCliente()));
                tabla.addCell(valor(v.getCanalVenta()));
                tabla.addCell(obtenerMetodoPago(v));
                tabla.addCell(valor(v.getEstadoVenta()));
                tabla.addCell("S/ " + valorDecimal(v.getTotalVenta()));
            }
        }

        documento.add(tabla);
        documento.close();
    }

    private void agregarCabeceraPdf(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(
                texto,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)
        ));

        celda.setBackgroundColor(new Color(40, 40, 40));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(6);

        tabla.addCell(celda);
    }

    // ============================================================
    // EXPORTAR EXCEL
    // ============================================================

    private void exportarExcel(HttpServletResponse response,
                               List<Venta> ventas,
                               String nombreHoja,
                               String nombreArchivo,
                               RangoFechas rango) throws Exception {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(nombreHoja);

        Row titulo = sheet.createRow(0);
        titulo.createCell(0).setCellValue(nombreHoja);

        Row rangoRow = sheet.createRow(1);
        rangoRow.createCell(0).setCellValue("Rango");
        rangoRow.createCell(1).setCellValue(rango.fechaDesde() + " al " + rango.fechaHasta());

        Row resumen = sheet.createRow(2);
        resumen.createCell(0).setCellValue("Total de registros");
        resumen.createCell(1).setCellValue(ventas != null ? ventas.size() : 0);
        resumen.createCell(2).setCellValue("Total vendido");
        resumen.createCell(3).setCellValue("S/ " + calcularTotalValido(ventas));

        Row header = sheet.createRow(4);
        header.createCell(0).setCellValue("Comprobante");
        header.createCell(1).setCellValue("Fecha / Hora");
        header.createCell(2).setCellValue("Cliente");
        header.createCell(3).setCellValue("Documento");
        header.createCell(4).setCellValue("Canal");
        header.createCell(5).setCellValue("Método de pago");
        header.createCell(6).setCellValue("Estado");
        header.createCell(7).setCellValue("Total");

        int fila = 5;

        if (ventas != null) {
            for (Venta v : ventas) {
                Row row = sheet.createRow(fila++);

                row.createCell(0).setCellValue(valor(v.getNroComprobante()));
                row.createCell(1).setCellValue(v.getFechaVenta() != null ? v.getFechaVenta().format(FORMATO_FECHA) : "-");
                row.createCell(2).setCellValue(valor(v.getNombreCliente()));
                row.createCell(3).setCellValue(valor(v.getDocumentoCliente()));
                row.createCell(4).setCellValue(valor(v.getCanalVenta()));
                row.createCell(5).setCellValue(obtenerMetodoPago(v));
                row.createCell(6).setCellValue(valor(v.getEstadoVenta()));
                row.createCell(7).setCellValue(v.getTotalVenta() != null ? v.getTotalVenta().doubleValue() : 0);
            }
        }

        for (int i = 0; i <= 7; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ============================================================
    // UTILIDADES
    // ============================================================

    private String valor(String texto) {
        return texto != null && !texto.isBlank() ? texto : "-";
    }

    private String valorDecimal(BigDecimal valor) {
        return valor != null ? valor.toString() : "0.00";
    }

    private String obtenerMetodoPago(Venta venta) {
        if (venta.getMetodoPago() != null && !venta.getMetodoPago().isBlank()) {
            return venta.getMetodoPago();
        }

        if (venta.getFormaPago() != null && !venta.getFormaPago().isBlank()) {
            return venta.getFormaPago();
        }

        return "-";
    }

    private BigDecimal calcularTotalValido(List<Venta> ventas) {
        if (ventas == null || ventas.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return ventas.stream()
                .filter(v -> v.getEstadoVenta() == null
                        || !"ANULADA".equalsIgnoreCase(v.getEstadoVenta()))
                .map(v -> v.getTotalVenta() != null ? v.getTotalVenta() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}