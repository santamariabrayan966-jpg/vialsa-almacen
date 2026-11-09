package com.dentia.controller.interno;

import com.dentia.service.interno.InventarioService;
import com.dentia.service.interno.SedeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import com.dentia.model.interno.inventario.InventarioTransferencia;
import com.dentia.model.interno.inventario.InventarioTransferenciaDetalle;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.dentia.model.interno.inventario.InventarioAjuste;
import com.dentia.model.interno.inventario.InventarioAjusteDetalle;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import com.dentia.service.interno.ProductoPrecioPresentacionService;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import com.dentia.service.interno.pdf.InventarioPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Controller
@RequestMapping("/interno/inventario")
public class InventarioController {

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

    private Integer obtenerUsuarioActualId(HttpSession session) {
        Object usuarioObj = session.getAttribute("usuarioActualId");

        if (usuarioObj instanceof Integer idUsuario) {
            return idUsuario;
        }

        if (usuarioObj instanceof String usuarioStr) {
            try {
                return Integer.parseInt(usuarioStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private final InventarioService inventarioService;
    private final SedeService sedeService;
    private final ProductoPrecioPresentacionService precioPresentacionService;
    private final InventarioPdfService inventarioPdfService;

    public InventarioController(InventarioService inventarioService,
                                SedeService sedeService,
                                ProductoPrecioPresentacionService precioPresentacionService,
                                InventarioPdfService inventarioPdfService) {
        this.inventarioService = inventarioService;
        this.sedeService = sedeService;
        this.precioPresentacionService = precioPresentacionService;
        this.inventarioPdfService = inventarioPdfService;
    }

    @GetMapping("/existencias")
    public String existencias(Model model, HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            model.addAttribute("existencias", List.of());
            model.addAttribute("sedes", sedeService.listar());
            model.addAttribute("stockTotal", BigDecimal.ZERO);
            model.addAttribute("productosConStock", 0);
            model.addAttribute("valorTotalInventario", BigDecimal.ZERO);
            model.addAttribute("sedesActivas", 0);
            model.addAttribute("errorSedeActiva", "No se encontró una sede activa en tu sesión.");
            model.addAttribute("sedeActivaNombre", "Sin sede activa");

            return "interno/inventario/existencias";
        }

        model.addAttribute("existencias", inventarioService.listarExistenciasPorSede(sedeActivaId));
        model.addAttribute("sedes", sedeService.listar());

        model.addAttribute("stockTotal", inventarioService.obtenerStockTotalPorSede(sedeActivaId));
        model.addAttribute("productosConStock", inventarioService.contarProductosConStockPorSede(sedeActivaId));
        model.addAttribute("valorTotalInventario", inventarioService.obtenerValorTotalInventarioPorSede(sedeActivaId));

        model.addAttribute("sedesActivas",
                sedeService.listar().stream()
                        .filter(s -> s.isActivo())
                        .count()
        );

        model.addAttribute("sedeActivaId", sedeActivaId);
        model.addAttribute("sedeActivaNombre", sedeActivaNombre);

        return "interno/inventario/existencias";
    }

    @GetMapping("/existencias/pdf")
    public ResponseEntity<byte[]> exportarExistenciasPdf(HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().build();
        }

        var existencias = inventarioService.listarExistenciasPorSede(sedeActivaId);

        String[] columnas = {
                "#",
                "Código",
                "Producto",
                "Categoría",
                "Stock",
                "Stock mínimo",
                "Costo promedio",
                "Valor aprox."
        };

        List<String[]> filas = new ArrayList<>();

        int i = 1;

        for (var e : existencias) {
            filas.add(new String[]{
                    String.valueOf(i++),
                    valorPdf(e.getCodigoProducto()),
                    valorPdf(e.getNombreProducto()),
                    valorPdf(e.getCategoria()),
                    numeroPdf(e.getStockActual()),
                    numeroPdf(e.getStockMinimo()),
                    "S/ " + numeroPdf(e.getCostoPromedio()),
                    "S/ " + numeroPdf(e.getValorAprox())
            });
        }

        byte[] pdf = inventarioPdfService.generarPdfTabla(
                "Reporte general de existencias",
                "Sede: " + (sedeActivaNombre != null ? sedeActivaNombre : "Sin sede")
                        + " | Productos: " + existencias.size(),
                columnas,
                filas
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=existencias_general_dentia.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/existencias/{idProducto}/pdf")
    public ResponseEntity<byte[]> exportarDetalleExistenciaPdf(
            @PathVariable Integer idProducto,
            HttpSession session
    ) {
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().build();
        }

        var detalle = inventarioService.obtenerDetalleExistencia(idProducto, sedeActivaId);

        var stock = (com.dentia.model.interno.inventario.InventarioStock) detalle.get("stock");

        List<String[]> datos = new ArrayList<>();

        datos.add(new String[]{"Código", valorPdf(stock.getCodigoProducto())});
        datos.add(new String[]{"Producto", valorPdf(stock.getNombreProducto())});
        datos.add(new String[]{"Categoría", valorPdf(stock.getCategoria())});
        datos.add(new String[]{"Sede", valorPdf(stock.getNombreSede())});
        datos.add(new String[]{"Tipo sede", valorPdf(stock.getTipoSede())});
        datos.add(new String[]{"Stock actual", numeroPdf(stock.getStockActual())});
        datos.add(new String[]{"Stock mínimo", numeroPdf(stock.getStockMinimo())});
        datos.add(new String[]{"Stock máximo", numeroPdf(stock.getStockMaximo())});
        datos.add(new String[]{"Costo promedio", "S/ " + numeroPdf(stock.getCostoPromedio())});
        datos.add(new String[]{"Valor aproximado", "S/ " + numeroPdf(stock.getValorAprox())});

        datos.add(new String[]{"Unidades completas", valorObjetoPdf(detalle.get("partesCompletas"))});
        datos.add(new String[]{"Medias unidades", valorObjetoPdf(detalle.get("mediasPartes"))});
        datos.add(new String[]{"Retazos / cortes", calcularRetazosCortesPdf(detalle.get("partes"))});
        datos.add(new String[]{"Medida disponible total", valorObjetoPdf(detalle.get("medidaDisponibleTotal"))});
        datos.add(new String[]{"Equivalente en unidad física", valorObjetoPdf(detalle.get("equivalenteUnidadFisicaTotal"))});
        datos.add(new String[]{"Costo disponible total", "S/ " + valorObjetoPdf(detalle.get("costoDisponibleTotal"))});

        byte[] pdf = inventarioPdfService.generarPdfDetalle(
                "Detalle de existencia",
                "Producto: " + valorPdf(stock.getNombreProducto()),
                datos
        );

        String nombreArchivo = "existencia_detalle_"
                + (stock.getIdProducto() != null ? stock.getIdProducto() : 0)
                + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private String valorObjetoPdf(Object valor) {
        if (valor == null) {
            return "No aplica";
        }

        String texto = String.valueOf(valor).trim();

        if (texto.isEmpty() || "null".equalsIgnoreCase(texto)) {
            return "No aplica";
        }

        return texto;
    }
    private String calcularRetazosCortesPdf(Object partesObj) {
        if (!(partesObj instanceof List<?> partes) || partes.isEmpty()) {
            return "0";
        }

        long total = partes.stream()
                .filter(p -> {
                    try {
                        Object tipoParte = p.getClass().getMethod("getTipoParte").invoke(p);

                        if (tipoParte == null) {
                            return false;
                        }

                        String tipo = String.valueOf(tipoParte).trim().toUpperCase();

                        return tipo.equals("RETAZO") || tipo.equals("CORTE");

                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        return String.valueOf(total);
    }

    private String valorPdf(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "-";
        }

        return valor;
    }

    private String numeroPdf(BigDecimal valor) {
        if (valor == null) {
            return "0.00";
        }

        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    @GetMapping("/existencias/detalle/{idProducto}")
    @ResponseBody
    public ResponseEntity<?> detalleExistencia(
            @PathVariable Integer idProducto,
            HttpSession session
    ) {
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().body("No se encontró una sede activa en tu sesión.");
        }

        return ResponseEntity.ok(
                inventarioService.obtenerDetalleExistencia(idProducto, sedeActivaId)
        );
    }

    @GetMapping("/transferencias/pdf")
    public ResponseEntity<byte[]> exportarTransferenciasPdf(HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().build();
        }

        var transferencias = inventarioService.listarTransferenciasPorSede(sedeActivaId);

        String[] columnas = {
                "#",
                "Código",
                "Fecha",
                "Origen",
                "Destino",
                "Productos",
                "Cantidad total",
                "Estado",
                "Responsable"
        };

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        List<String[]> filas = new ArrayList<>();

        int i = 1;

        for (var t : transferencias) {

            BigDecimal cantidadTotal = t.getDetalles() != null
                    ? t.getDetalles().stream()
                    .map(InventarioTransferenciaDetalle::getCantidad)
                    .filter(c -> c != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;

            filas.add(new String[]{
                    String.valueOf(i++),
                    valorPdf(t.getCodigo()),
                    t.getFechaTransferencia() != null ? t.getFechaTransferencia().format(formato) : "-",
                    valorPdf(t.getNombreSedeOrigen()),
                    valorPdf(t.getNombreSedeDestino()),
                    t.getDetalles() != null ? String.valueOf(t.getDetalles().size()) : "0",
                    numeroPdf(cantidadTotal),
                    formatearEstadoTransferenciaPdf(t.getEstado()),
                    t.getNombreUsuario() != null ? t.getNombreUsuario() : "Sistema"
            });
        }

        byte[] pdf = inventarioPdfService.generarPdfTabla(
                "Reporte general de transferencias",
                "Sede activa: " + (sedeActivaNombre != null ? sedeActivaNombre : "Sin sede")
                        + " | Total transferencias: " + transferencias.size(),
                columnas,
                filas
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transferencias_general_dentia.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/transferencias/{idTransferencia}/pdf")
    public ResponseEntity<byte[]> exportarDetalleTransferenciaPdf(
            @PathVariable Integer idTransferencia,
            HttpSession session
    ) {
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().build();
        }

        InventarioTransferencia t = inventarioService.buscarTransferenciaPorId(idTransferencia);

        if (!sedeActivaId.equals(t.getIdSedeOrigen()) && !sedeActivaId.equals(t.getIdSedeDestino())) {
            return ResponseEntity.badRequest().build();
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        StringBuilder productos = new StringBuilder();
        BigDecimal cantidadTotal = BigDecimal.ZERO;

        if (t.getDetalles() != null && !t.getDetalles().isEmpty()) {
            for (var d : t.getDetalles()) {
                BigDecimal cantidad = d.getCantidad() != null ? d.getCantidad() : BigDecimal.ZERO;
                cantidadTotal = cantidadTotal.add(cantidad);

                productos.append("- ")
                        .append(d.getNombreProducto() != null ? d.getNombreProducto() : "Producto")
                        .append(" / ")
                        .append(d.getCodigoProducto() != null ? d.getCodigoProducto() : "-")
                        .append(" | Cantidad: ")
                        .append(numeroPdf(cantidad))
                        .append("\n");
            }
        } else {
            productos.append("Sin productos registrados.");
        }

        List<String[]> datos = new ArrayList<>();

        datos.add(new String[]{"Código", valorPdf(t.getCodigo())});
        datos.add(new String[]{"Fecha / hora", t.getFechaTransferencia() != null ? t.getFechaTransferencia().format(formato) : "-"});
        datos.add(new String[]{"Sede origen", valorPdf(t.getNombreSedeOrigen())});
        datos.add(new String[]{"Sede destino", valorPdf(t.getNombreSedeDestino())});
        datos.add(new String[]{"Responsable", t.getNombreUsuario() != null ? t.getNombreUsuario() : "Sistema"});
        datos.add(new String[]{"Estado", formatearEstadoTransferenciaPdf(t.getEstado())});
        datos.add(new String[]{"Cantidad total", numeroPdf(cantidadTotal)});
        datos.add(new String[]{"Observación", valorObjetoPdf(t.getObservacion())});
        datos.add(new String[]{"Productos transferidos", productos.toString()});

        byte[] pdf = inventarioPdfService.generarPdfDetalle(
                "Detalle de transferencia de inventario",
                "Código: " + (t.getCodigo() != null ? t.getCodigo() : "-"),
                datos
        );

        String nombreArchivo = "transferencia_detalle_"
                + (t.getIdTransferencia() != null ? t.getIdTransferencia() : 0)
                + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/transferencias")
    public String transferencias(Model model, HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            model.addAttribute("existencias", List.of());
            model.addAttribute("sedes", sedeService.listar());
            model.addAttribute("transferencias", List.of());
            model.addAttribute("codigoTransferencia", inventarioService.generarCodigoTransferencia());
            model.addAttribute("errorSedeActiva", "No se encontró una sede activa en tu sesión.");
            model.addAttribute("sedeActivaNombre", "Sin sede activa");

            return "interno/inventario/transferencias";
        }

        model.addAttribute("existencias", inventarioService.listarExistenciasPorSede(sedeActivaId));
        model.addAttribute("sedes", sedeService.listar()
                .stream()
                .filter(s -> s.isActivo())
                .filter(s -> !s.getIdSede().equals(sedeActivaId))
                .toList()
        );
        model.addAttribute("transferencias", inventarioService.listarTransferenciasPorSede(sedeActivaId));
        model.addAttribute("codigoTransferencia", inventarioService.generarCodigoTransferencia());

        model.addAttribute("sedeActivaId", sedeActivaId);
        model.addAttribute("sedeActivaNombre", sedeActivaNombre);

        return "interno/inventario/transferencias";
    }

    @PostMapping("/transferencias/guardar")
    public String guardarTransferencia(
            HttpSession session,
            @RequestParam Integer idSedeDestino,
            @RequestParam(required = false) String observacion,
            @RequestParam(required = false) String tipoTransferencia,
            @RequestParam(value = "requiereTransporte", required = false) String requiereTransporteParam,
            @RequestParam("idProducto") List<Integer> idProductos,
            @RequestParam("cantidad") List<BigDecimal> cantidades
    ) {

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/inventario/transferencias?errorSede=true";
        }

        if (sedeActivaId.equals(idSedeDestino)) {
            return "redirect:/interno/inventario/transferencias?errorMismaSede=true";
        }

        if (idProductos == null || idProductos.isEmpty()) {
            return "redirect:/interno/inventario/transferencias?errorSinProductos=true";
        }

        if (cantidades == null || cantidades.size() != idProductos.size()) {
            return "redirect:/interno/inventario/transferencias?errorCantidad=true";
        }

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        boolean requiereTransporte =
                "on".equalsIgnoreCase(requiereTransporteParam)
                        || "true".equalsIgnoreCase(requiereTransporteParam)
                        || "CON_TRANSPORTE".equalsIgnoreCase(tipoTransferencia);

        InventarioTransferencia transferencia = new InventarioTransferencia();
        transferencia.setIdSedeOrigen(sedeActivaId);
        transferencia.setIdSedeDestino(idSedeDestino);
        transferencia.setObservacion(observacion);
        transferencia.setIdUsuario(usuarioActualId);

        if (requiereTransporte) {
            transferencia.setTipoTransferencia("CON_TRANSPORTE");
            transferencia.setRequiereTransporte(true);
        } else {
            transferencia.setTipoTransferencia("INMEDIATA");
            transferencia.setRequiereTransporte(false);
        }

        List<InventarioTransferenciaDetalle> detalles = new ArrayList<>();

        for (int i = 0; i < idProductos.size(); i++) {

            Integer idProducto = idProductos.get(i);
            BigDecimal cantidad = cantidades.get(i);

            if (idProducto == null || idProducto <= 0) {
                return "redirect:/interno/inventario/transferencias?errorProducto=true";
            }

            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                return "redirect:/interno/inventario/transferencias?errorCantidad=true";
            }

            InventarioTransferenciaDetalle d = new InventarioTransferenciaDetalle();
            d.setIdProducto(idProducto);
            d.setCantidad(cantidad);
            d.setCantidadEnviada(cantidad);

            detalles.add(d);
        }

        try {
            inventarioService.registrarTransferencia(transferencia, detalles);
        } catch (RuntimeException e) {
            return "redirect:/interno/inventario/transferencias?errorTransferencia="
                    + java.net.URLEncoder.encode(
                    e.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8
            );
        }

        if (requiereTransporte) {
            return "redirect:/interno/inventario/transferencias?trasladoCreado=true";
        }

        return "redirect:/interno/inventario/transferencias?transferenciaOk=true";
    }

    @PostMapping("/transferencias/editar-productos")
    public String editarProductosTransferencia(
            HttpSession session,
            @RequestParam Integer idTransferencia,
            @RequestParam("idProducto") List<Integer> idProductos,
            @RequestParam("cantidad") List<BigDecimal> cantidades
    ) {

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/inventario/transferencias?errorSede=true";
        }

        if (idTransferencia == null || idTransferencia <= 0) {
            return "redirect:/interno/inventario/transferencias?errorTransferencia="
                    + URLEncoder.encode("No se encontró la transferencia a editar.", StandardCharsets.UTF_8);
        }

        if (idProductos == null || idProductos.isEmpty()) {
            return "redirect:/interno/inventario/transferencias?errorSinProductos=true";
        }

        if (cantidades == null || cantidades.size() != idProductos.size()) {
            return "redirect:/interno/inventario/transferencias?errorCantidad=true";
        }

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        List<InventarioTransferenciaDetalle> detalles = new ArrayList<>();

        for (int i = 0; i < idProductos.size(); i++) {

            Integer idProducto = idProductos.get(i);
            BigDecimal cantidad = cantidades.get(i);

            if (idProducto == null || idProducto <= 0) {
                return "redirect:/interno/inventario/transferencias?errorProducto=true";
            }

            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                return "redirect:/interno/inventario/transferencias?errorCantidad=true";
            }

            InventarioTransferenciaDetalle d = new InventarioTransferenciaDetalle();
            d.setIdProducto(idProducto);
            d.setCantidad(cantidad);
            d.setCantidadEnviada(cantidad);

            detalles.add(d);
        }

        try {
            inventarioService.editarProductosTransferenciaPendiente(
                    idTransferencia,
                    sedeActivaId,
                    detalles,
                    usuarioActualId
            );

        } catch (RuntimeException e) {
            return "redirect:/interno/inventario/transferencias?errorTransferencia="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }

        return "redirect:/interno/inventario/transferencias?transferenciaEditada=true";
    }
    @PostMapping("/transferencias/{idTransferencia}/anular")
    public String anularTransferencia(
            @PathVariable Integer idTransferencia,
            @RequestParam(required = false) String observacionAnulacion,
            HttpSession session
    ) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        Integer usuarioActualId = obtenerUsuarioActualId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/inventario/transferencias?errorSede=true";
        }

        try {
            inventarioService.anularTransferenciaPendiente(
                    idTransferencia,
                    sedeActivaId,
                    observacionAnulacion,
                    usuarioActualId
            );

        } catch (RuntimeException e) {
            return "redirect:/interno/inventario/transferencias?errorTransferencia="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }

        return "redirect:/interno/inventario/transferencias?transferenciaAnulada=true";
    }

    @GetMapping("/kardex")
    public String kardex(
            Model model,
            HttpSession session,
            @RequestParam(required = false) Integer idProducto,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String origenMovimiento,
            @RequestParam(required = false) String tipoMovimiento,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            model.addAttribute("movimientos", List.of());
            model.addAttribute("totalMovimientos", 0);
            model.addAttribute("errorSedeActiva", "No se encontró una sede activa en tu sesión.");
            model.addAttribute("sedeActivaNombre", "Sin sede activa");

            return "interno/inventario/kardex";
        }

        var movimientos = inventarioService.listarMovimientosKardex(
                sedeActivaId,
                idProducto,
                busqueda,
                origenMovimiento,
                tipoMovimiento,
                fechaInicio,
                fechaFin
        );

        int totalMovimientos = inventarioService.contarMovimientosKardex(
                sedeActivaId,
                idProducto,
                busqueda,
                origenMovimiento,
                tipoMovimiento,
                fechaInicio,
                fechaFin
        );

        long totalCompras = movimientos.stream()
                .filter(m -> "COMPRA".equalsIgnoreCase(m.getOrigenMovimiento()))
                .count();

        long totalTransferencias = movimientos.stream()
                .filter(m -> "TRANSFERENCIA".equalsIgnoreCase(m.getOrigenMovimiento()))
                .count();

        long totalVentas = movimientos.stream()
                .filter(m -> "VENTA".equalsIgnoreCase(m.getOrigenMovimiento()))
                .count();

        long totalAjustes = movimientos.stream()
                .filter(m -> "AJUSTE".equalsIgnoreCase(m.getOrigenMovimiento()))
                .count();

        model.addAttribute("movimientos", movimientos);
        model.addAttribute("totalMovimientos", totalMovimientos);

        model.addAttribute("totalCompras", totalCompras);
        model.addAttribute("totalTransferencias", totalTransferencias);
        model.addAttribute("totalVentas", totalVentas);
        model.addAttribute("totalAjustes", totalAjustes);

        model.addAttribute("idProductoFiltro", idProducto);
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("origenMovimiento", origenMovimiento);
        model.addAttribute("tipoMovimiento", tipoMovimiento);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        model.addAttribute("filtrosActivos",
                idProducto != null
                        || (busqueda != null && !busqueda.trim().isEmpty())
                        || (origenMovimiento != null && !origenMovimiento.trim().isEmpty())
                        || (tipoMovimiento != null && !tipoMovimiento.trim().isEmpty())
                        || fechaInicio != null
                        || fechaFin != null
        );

        model.addAttribute("sedeActivaId", sedeActivaId);
        model.addAttribute("sedeActivaNombre", sedeActivaNombre);

        return "interno/inventario/kardex";
    }

    @GetMapping("/transferencias/detalle/{idTransferencia}")
    @ResponseBody
    public ResponseEntity<?> detalleTransferencia(
            @PathVariable Integer idTransferencia,
            HttpSession session
    ) {
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().body("No se encontró una sede activa.");
        }

        InventarioTransferencia transferencia = inventarioService.buscarTransferenciaPorId(idTransferencia);

        if (!sedeActivaId.equals(transferencia.getIdSedeOrigen())
                && !sedeActivaId.equals(transferencia.getIdSedeDestino())) {
            return ResponseEntity.badRequest().body("La transferencia no pertenece a la sede activa.");
        }

        var eventos = inventarioService.listarEventosTransferencia(idTransferencia);

        Map<String, Object> respuesta = new HashMap<>();

        respuesta.put("idTransferencia", transferencia.getIdTransferencia());
        respuesta.put("codigo", transferencia.getCodigo());
        respuesta.put("estado", transferencia.getEstado());
        respuesta.put("tipoTransferencia", transferencia.getTipoTransferencia());
        respuesta.put("requiereTransporte", transferencia.getRequiereTransporte());

        respuesta.put("idSedeOrigen", transferencia.getIdSedeOrigen());
        respuesta.put("idSedeDestino", transferencia.getIdSedeDestino());
        respuesta.put("nombreSedeOrigen", transferencia.getNombreSedeOrigen());
        respuesta.put("nombreSedeDestino", transferencia.getNombreSedeDestino());

        respuesta.put("idUsuario", transferencia.getIdUsuario());
        respuesta.put("nombreUsuario", transferencia.getNombreUsuario());

        respuesta.put("idTransportista", transferencia.getIdTransportista());
        respuesta.put("nombreTransportista", transferencia.getNombreTransportista());

        respuesta.put("observacion", transferencia.getObservacion());
        respuesta.put("observacionRecepcion", transferencia.getObservacionRecepcion());

        respuesta.put("fechaTransferencia", transferencia.getFechaTransferencia());
        respuesta.put("fechaAceptacion", transferencia.getFechaAceptacion());
        respuesta.put("fechaLlegadaOrigen", transferencia.getFechaLlegadaOrigen());
        respuesta.put("fechaPedidoListo", transferencia.getFechaPedidoListo());
        respuesta.put("fechaInicioRuta", transferencia.getFechaInicioRuta());
        respuesta.put("fechaLlegadaDestino", transferencia.getFechaLlegadaDestino());
        respuesta.put("fechaRecepcion", transferencia.getFechaRecepcion());

        respuesta.put("fotoLlegadaOrigen", transferencia.getFotoLlegadaOrigen());
        respuesta.put("fotoCarga", transferencia.getFotoCarga());
        respuesta.put("fotoLlegada", transferencia.getFotoLlegada());

        respuesta.put("detalles", transferencia.getDetalles());
        respuesta.put("eventos", eventos);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/kardex/pdf")
    public ResponseEntity<byte[]> exportarKardexPdf(
            HttpSession session,
            @RequestParam(required = false) Integer idProducto,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String origenMovimiento,
            @RequestParam(required = false) String tipoMovimiento,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().build();
        }

        var movimientos = inventarioService.listarMovimientosKardex(
                sedeActivaId,
                idProducto,
                busqueda,
                origenMovimiento,
                tipoMovimiento,
                fechaInicio,
                fechaFin
        );

        String[] columnas = {
                "#",
                "Fecha / Hora",
                "Tipo",
                "Origen",
                "Documento",
                "Producto",
                "Entrada",
                "Salida",
                "Stock",
                "Usuario"
        };

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        List<String[]> filas = new ArrayList<>();

        int i = 1;

        for (var m : movimientos) {
            filas.add(new String[]{
                    String.valueOf(i++),
                    m.getFechaMovimiento() != null ? m.getFechaMovimiento().format(formato) : "-",
                    formatearTipoMovimientoPdf(m.getTipoMovimiento()),
                    formatearOrigenMovimientoPdf(m.getOrigenMovimiento()),
                    m.getDocumentoReferencia(),
                    m.getNombreProducto() + " / " + m.getCodigoProducto(),
                    m.getEntradaTexto(),
                    m.getSalidaTexto(),
                    m.getStockResultanteTexto(),
                    m.getNombreUsuario() != null ? m.getNombreUsuario() : "Sistema"
            });
        }

        byte[] pdf = inventarioPdfService.generarPdfTabla(
                "Reporte general de Kardex",
                "Sede: " + (sedeActivaNombre != null ? sedeActivaNombre : "Sin sede")
                        + " | Total movimientos: " + movimientos.size(),
                columnas,
                filas
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=kardex_general_dentia.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/kardex/movimiento/{idMovimiento}/pdf")
    public ResponseEntity<byte[]> exportarDetalleMovimientoPdf(
            @PathVariable Integer idMovimiento,
            HttpSession session
    ) {
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().build();
        }

        var m = inventarioService.buscarMovimientoPorId(idMovimiento);

        if (!sedeActivaId.equals(m.getIdSede())) {
            return ResponseEntity.badRequest().build();
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        List<String[]> datos = new ArrayList<>();

        datos.add(new String[]{"Documento", m.getDocumentoReferencia()});
        datos.add(new String[]{"Fecha / hora", m.getFechaMovimiento() != null ? m.getFechaMovimiento().format(formato) : "-"});
        datos.add(new String[]{"Sede", m.getNombreSede() + " / " + m.getTipoSede()});
        datos.add(new String[]{"Usuario", m.getNombreUsuario() != null ? m.getNombreUsuario() : "Sistema"});
        datos.add(new String[]{"Tipo movimiento", formatearTipoMovimientoPdf(m.getTipoMovimiento())});
        datos.add(new String[]{"Origen", formatearOrigenMovimientoPdf(m.getOrigenMovimiento())});
        datos.add(new String[]{"Producto", m.getNombreProducto() + " / " + m.getCodigoProducto()});
        datos.add(new String[]{"Entrada", m.getEntradaTexto()});
        datos.add(new String[]{"Salida", m.getSalidaTexto()});
        datos.add(new String[]{"Stock resultante", m.getStockResultanteTexto()});
        datos.add(new String[]{"Costo unitario", "S/ " + m.getCostoUnitarioTexto()});
        datos.add(new String[]{"Observación", m.getObservacion()});

        byte[] pdf = inventarioPdfService.generarPdfDetalle(
                "Detalle de movimiento Kardex",
                "Documento: " + (m.getDocumentoReferencia() != null ? m.getDocumentoReferencia() : "-"),
                datos
        );

        String nombreArchivo = "kardex_detalle_"
                + (m.getIdMovimiento() != null ? m.getIdMovimiento() : 0)
                + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    private String formatearTipoMovimientoPdf(String tipo) {
        if ("ENTRADA".equalsIgnoreCase(tipo)) {
            return "Entrada";
        }

        if ("SALIDA".equalsIgnoreCase(tipo)) {
            return "Salida";
        }

        return tipo != null ? tipo : "-";
    }

    private String formatearOrigenMovimientoPdf(String origen) {
        if ("COMPRA".equalsIgnoreCase(origen)) {
            return "Compra";
        }

        if ("VENTA".equalsIgnoreCase(origen)) {
            return "Venta";
        }

        if ("TRANSFERENCIA".equalsIgnoreCase(origen)) {
            return "Transferencia";
        }

        if ("AJUSTE".equalsIgnoreCase(origen)) {
            return "Ajuste";
        }

        return origen != null ? origen : "-";
    }

    @GetMapping("/ajustes/pdf")
    public ResponseEntity<byte[]> exportarAjustesPdf(HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().build();
        }

        var ajustes = inventarioService.listarAjustesPorSede(sedeActivaId);

        String[] columnas = {
                "#",
                "Código",
                "Fecha",
                "Tipo",
                "Motivo",
                "Productos",
                "Estado",
                "Usuario"
        };

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        List<String[]> filas = new ArrayList<>();

        int i = 1;

        for (var a : ajustes) {
            filas.add(new String[]{
                    String.valueOf(i++),
                    a.getCodigo(),
                    a.getFechaAjuste() != null ? a.getFechaAjuste().format(formato) : "-",
                    formatearTipoAjustePdf(a.getTipoAjuste()),
                    a.getMotivo(),
                    a.getDetalles() != null ? String.valueOf(a.getDetalles().size()) : "0",
                    a.getEstado(),
                    a.getNombreUsuario() != null ? a.getNombreUsuario() : "Sistema"
            });
        }

        byte[] pdf = inventarioPdfService.generarPdfTabla(
                "Reporte general de ajustes",
                "Sede: " + (sedeActivaNombre != null ? sedeActivaNombre : "Sin sede")
                        + " | Total ajustes: " + ajustes.size(),
                columnas,
                filas
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ajustes_general_dentia.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/ajustes/{idAjuste}/pdf")
    public ResponseEntity<byte[]> exportarDetalleAjustePdf(
            @PathVariable Integer idAjuste,
            HttpSession session
    ) {
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().build();
        }

        var a = inventarioService.buscarAjustePorId(idAjuste);

        if (!sedeActivaId.equals(a.getIdSede())) {
            return ResponseEntity.badRequest().build();
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        StringBuilder productos = new StringBuilder();

        if (a.getDetalles() != null && !a.getDetalles().isEmpty()) {
            for (var d : a.getDetalles()) {
                productos.append("- ")
                        .append(d.getNombreProducto() != null ? d.getNombreProducto() : "Producto")
                        .append(" / ")
                        .append(d.getCodigoProducto() != null ? d.getCodigoProducto() : "-")
                        .append(" | Cantidad: ")
                        .append(d.getCantidad() != null ? d.getCantidad() : "0")
                        .append(" | Stock anterior: ")
                        .append(d.getStockAnterior() != null ? d.getStockAnterior() : "0")
                        .append(" | Stock nuevo: ")
                        .append(d.getStockNuevo() != null ? d.getStockNuevo() : "0")
                        .append("\n");
            }
        } else {
            productos.append("Sin productos registrados.");
        }

        List<String[]> datos = new ArrayList<>();

        datos.add(new String[]{"Código", a.getCodigo()});
        datos.add(new String[]{"Fecha / hora", a.getFechaAjuste() != null ? a.getFechaAjuste().format(formato) : "-"});
        datos.add(new String[]{"Sede", a.getNombreSede()});
        datos.add(new String[]{"Usuario", a.getNombreUsuario() != null ? a.getNombreUsuario() : "Sistema"});
        datos.add(new String[]{"Tipo ajuste", formatearTipoAjustePdf(a.getTipoAjuste())});
        datos.add(new String[]{"Motivo", a.getMotivo()});
        datos.add(new String[]{"Estado", a.getEstado()});
        datos.add(new String[]{"Observación", a.getObservacion()});
        datos.add(new String[]{"Productos afectados", productos.toString()});

        byte[] pdf = inventarioPdfService.generarPdfDetalle(
                "Detalle de ajuste de inventario",
                "Código: " + (a.getCodigo() != null ? a.getCodigo() : "-"),
                datos
        );

        String nombreArchivo = "ajuste_detalle_"
                + (a.getIdAjuste() != null ? a.getIdAjuste() : 0)
                + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    private String formatearEstadoTransferenciaPdf(String estado) {
        if ("COMPLETADA".equalsIgnoreCase(estado)) {
            return "Completada";
        }

        if ("EN_TRANSITO".equalsIgnoreCase(estado)) {
            return "En tránsito";
        }

        if ("CANCELADA".equalsIgnoreCase(estado)) {
            return "Cancelada";
        }

        return estado != null ? estado : "-";
    }

    private String formatearTipoAjustePdf(String tipo) {
        if ("AUMENTO".equalsIgnoreCase(tipo)) {
            return "Aumento";
        }

        if ("DISMINUCION".equalsIgnoreCase(tipo)) {
            return "Disminución";
        }

        return tipo != null ? tipo : "-";
    }

    @GetMapping("/ajustes")
    public String ajustes(
            Model model,
            HttpSession session,
            @RequestParam(required = false) Integer idProducto
    ) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            model.addAttribute("existencias", List.of());
            model.addAttribute("sedes", sedeService.listar());
            model.addAttribute("ajustes", List.of());
            model.addAttribute("codigoAjuste", inventarioService.generarCodigoAjuste());
            model.addAttribute("errorSedeActiva", "No se encontró una sede activa en tu sesión.");
            model.addAttribute("sedeActivaNombre", "Sin sede activa");
            model.addAttribute("idProductoSeleccionado", idProducto);

            return "interno/inventario/ajustes";
        }

        var existencias = inventarioService.listarExistenciasPorSede(sedeActivaId);
        var ajustes = inventarioService.listarAjustesPorSede(sedeActivaId);

        long totalAjustes = ajustes.size();

        long totalAumentos = ajustes.stream()
                .filter(a -> "AUMENTO".equalsIgnoreCase(a.getTipoAjuste()))
                .count();

        long totalDisminuciones = ajustes.stream()
                .filter(a -> "DISMINUCION".equalsIgnoreCase(a.getTipoAjuste()))
                .count();

        long totalConEvidencia = ajustes.stream()
                .filter(a -> a.getEvidenciaFoto() != null && !a.getEvidenciaFoto().trim().isEmpty())
                .count();

        model.addAttribute("existencias", existencias);
        model.addAttribute("sedes", sedeService.listar());
        model.addAttribute("ajustes", ajustes);
        model.addAttribute("codigoAjuste", inventarioService.generarCodigoAjuste());

        model.addAttribute("totalAjustes", totalAjustes);
        model.addAttribute("totalAumentos", totalAumentos);
        model.addAttribute("totalDisminuciones", totalDisminuciones);
        model.addAttribute("totalConEvidencia", totalConEvidencia);

        model.addAttribute("idProductoSeleccionado", idProducto);

        model.addAttribute("productoPrecargado", existencias.stream()
                .filter(e -> idProducto != null && e.getIdProducto().equals(idProducto))
                .findFirst()
                .orElse(null)
        );

        model.addAttribute("sedeActivaId", sedeActivaId);
        model.addAttribute("sedeActivaNombre", sedeActivaNombre);

        return "interno/inventario/ajustes";
    }

    @GetMapping("/ajustes/producto/{idProducto}/presentaciones")
    @ResponseBody
    public ResponseEntity<?> presentacionesProductoAjuste(
            @PathVariable Integer idProducto,
            HttpSession session
    ) {
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return ResponseEntity.badRequest().body("No se encontró una sede activa.");
        }

        var presentacionesInventario =
                inventarioService.obtenerPresentacionesAjusteDesdeInventario(idProducto, sedeActivaId);

        if (!presentacionesInventario.isEmpty()) {
            return ResponseEntity.ok(presentacionesInventario);
        }

        var presentaciones = precioPresentacionService
                .listarActivasPorProductoYSede(idProducto, sedeActivaId)
                .stream()
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();

                    item.put("idPrecioPresentacion", p.getIdPrecioPresentacion());
                    item.put("nombrePresentacion", p.getNombrePresentacion());
                    item.put("tipoCalculoVenta", p.getTipoCalculoVenta());
                    item.put("cantidadIncluida", p.getCantidadIncluida());
                    item.put("factorConversionStock", p.getFactorConversionStock());
                    item.put("precioVenta", p.getPrecioVenta());
                    item.put("nombreUnidadVenta", p.getNombreUnidadVenta());
                    item.put("abreviaturaUnidadVenta", p.getAbreviaturaUnidadVenta());

                    return item;
                })
                .toList();

        return ResponseEntity.ok(presentaciones);
    }

    @PostMapping("/ajustes/guardar")
    public String guardarAjuste(
            HttpSession session,
            @RequestParam String motivo,
            @RequestParam String tipoAjuste,
            @RequestParam(required = false) String observacion,

            @RequestParam("idProducto") List<Integer> idProductos,
            @RequestParam("tipoPresentacion") List<String> tiposPresentacion,
            @RequestParam("cantidad") List<BigDecimal> cantidades,

            @RequestParam(value = "motivoDetalle", required = false) List<String> motivosDetalle,
            @RequestParam(value = "observacionDetalle", required = false) List<String> observacionesDetalle,
            @RequestParam(value = "evidenciaFoto", required = false) MultipartFile evidenciaFoto
    ) {

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/inventario/ajustes?errorSede=true";
        }

        if (idProductos == null || idProductos.isEmpty()) {
            return "redirect:/interno/inventario/ajustes?errorSinProductos=true";
        }

        if (tiposPresentacion == null || tiposPresentacion.size() != idProductos.size()) {
            return "redirect:/interno/inventario/ajustes?errorPresentacion=true";
        }

        if (cantidades == null || cantidades.size() != idProductos.size()) {
            return "redirect:/interno/inventario/ajustes?errorCantidad=true";
        }
        if ("OTRO".equalsIgnoreCase(motivo)
                && (observacion == null || observacion.trim().length() < 10)) {

            return "redirect:/interno/inventario/ajustes?errorAjuste="
                    + java.net.URLEncoder.encode(
                    "Cuando selecciona 'Otro', debe explicar el motivo en la observación. Mínimo 10 caracteres.",
                    java.nio.charset.StandardCharsets.UTF_8
            );
        }
        if (requiereEvidenciaFotografica(motivo)
                && (evidenciaFoto == null || evidenciaFoto.isEmpty())) {

            return "redirect:/interno/inventario/ajustes?errorAjuste="
                    + java.net.URLEncoder.encode(
                    "Para el motivo seleccionado debe adjuntar una foto como evidencia.",
                    java.nio.charset.StandardCharsets.UTF_8
            );
        }

        String rutaEvidencia = null;

        try {
            rutaEvidencia = guardarEvidenciaAjuste(evidenciaFoto, motivo);
        } catch (RuntimeException e) {
            return "redirect:/interno/inventario/ajustes?errorAjuste="
                    + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        InventarioAjuste ajuste = new InventarioAjuste();
        ajuste.setIdSede(sedeActivaId);
        ajuste.setMotivo(motivo);
        ajuste.setTipoAjuste(tipoAjuste);
        ajuste.setObservacion(observacion);
        ajuste.setEvidenciaFoto(rutaEvidencia);
        ajuste.setIdUsuario(usuarioActualId);

        List<InventarioAjusteDetalle> detalles = new ArrayList<>();

        for (int i = 0; i < idProductos.size(); i++) {

            Integer idProducto = idProductos.get(i);
            String tipoPresentacion = tiposPresentacion.get(i);
            BigDecimal cantidad = cantidades.get(i);

            String motivoDetalle = obtenerValorLista(motivosDetalle, i);
            String observacionDetalle = obtenerValorLista(observacionesDetalle, i);

            InventarioAjusteDetalle d = new InventarioAjusteDetalle();

            d.setIdProducto(idProducto);
            d.setTipoPresentacion(tipoPresentacion);
            d.setCantidad(cantidad);

            d.setMotivoDetalle(
                    motivoDetalle != null && !motivoDetalle.trim().isEmpty()
                            ? motivoDetalle
                            : motivo
            );

            d.setObservacionDetalle(observacionDetalle);

            d.setStockAnterior(BigDecimal.ZERO);
            d.setStockNuevo(BigDecimal.ZERO);
            d.setFactorConversionStock(BigDecimal.ZERO);
            d.setCantidadDescontadaStock(BigDecimal.ZERO);
            d.setValorPerdida(BigDecimal.ZERO);

            detalles.add(d);
        }

        try {
            inventarioService.registrarAjusteConInventario(ajuste, detalles);
        } catch (RuntimeException e) {
            return "redirect:/interno/inventario/ajustes?errorAjuste="
                    + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }

        return "redirect:/interno/inventario/ajustes?ajusteOk=true";
    }

    private boolean requiereEvidenciaFotografica(String motivo) {

        if (motivo == null) {
            return false;
        }

        String m = motivo.trim().toUpperCase();

        return m.equals("ROTURA")
                || m.equals("DANO")
                || m.equals("DAÑO")
                || m.equals("PERDIDA")
                || m.equals("PÉRDIDA")
                || m.equals("VENCIMIENTO");
    }

    private String guardarEvidenciaAjuste(MultipartFile archivo, String motivo) {

        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        String contenido = archivo.getContentType();

        if (contenido == null || !contenido.startsWith("image/")) {
            throw new RuntimeException("La evidencia debe ser una imagen válida.");
        }

        long maximoBytes = 5 * 1024 * 1024;

        if (archivo.getSize() > maximoBytes) {
            throw new RuntimeException("La imagen no debe superar los 5 MB.");
        }

        try {
            String extension = com.dentia.service.shared.FileStorageService.extensionPermitida(archivo);

            String motivoSeguro = motivo != null
                    ? motivo.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_")
                    : "EVIDENCIA";

            String nombreSeguro = "AJUSTE_"
                    + motivoSeguro
                    + "_"
                    + UUID.randomUUID()
                    + extension;

            Path carpeta = Path.of(
                    System.getProperty("user.dir"),
                    "uploads",
                    "ajustes"
            ).toAbsolutePath().normalize();

            Files.createDirectories(carpeta);

            Path destino = carpeta.resolve(nombreSeguro)
                    .toAbsolutePath()
                    .normalize();

            Files.copy(
                    archivo.getInputStream(),
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return nombreSeguro;

        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la evidencia fotográfica.");
        }
    }

    private String obtenerValorLista(List<String> lista, int index) {
        if (lista == null || index < 0 || index >= lista.size()) {
            return null;
        }

        return lista.get(index);
    }

    @PostMapping("/transferencias/{idTransferencia}/confirmar-recepcion")
    public String confirmarRecepcionTransferencia(
            @PathVariable Integer idTransferencia,
            @RequestParam("idDetalleTransferencia") List<Integer> idsDetalle,
            @RequestParam("cantidadRecibida") List<BigDecimal> cantidadesRecibidas,
            @RequestParam(value = "observacionDetalle", required = false) List<String> observacionesDetalle,
            @RequestParam(required = false) String observacionRecepcion,
            HttpSession session
    ) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);
        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/inventario/transferencias?errorSede=true";
        }

        try {
            InventarioTransferencia transferencia = inventarioService.buscarTransferenciaPorId(idTransferencia);

            if (!sedeActivaId.equals(transferencia.getIdSedeDestino())) {
                throw new RuntimeException("Solo la sede destino puede confirmar la recepción de esta transferencia.");
            }

            if (idsDetalle == null
                    || cantidadesRecibidas == null
                    || idsDetalle.size() != cantidadesRecibidas.size()) {
                throw new RuntimeException("Los datos de recepción están incompletos.");
            }

            List<InventarioTransferenciaDetalle> detallesRecepcion = new ArrayList<>();

            for (int i = 0; i < idsDetalle.size(); i++) {
                InventarioTransferenciaDetalle d = new InventarioTransferenciaDetalle();

                d.setIdDetalleTransferencia(idsDetalle.get(i));
                d.setCantidadRecibida(cantidadesRecibidas.get(i));

                if (observacionesDetalle != null && i < observacionesDetalle.size()) {
                    d.setObservacionRecepcion(observacionesDetalle.get(i));
                }

                detallesRecepcion.add(d);
            }

            inventarioService.confirmarRecepcionTraslado(
                    idTransferencia,
                    detallesRecepcion,
                    observacionRecepcion,
                    usuarioActualId
            );

            return "redirect:/interno/inventario/transferencias?recepcionOk=true";

        } catch (RuntimeException e) {
            return "redirect:/interno/inventario/transferencias?errorTransferencia="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }
}
