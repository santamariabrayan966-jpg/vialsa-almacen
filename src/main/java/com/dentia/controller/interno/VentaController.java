package com.dentia.controller.interno;

import com.dentia.dao.interfaces.IDetalleVentaDao;
import com.dentia.model.interno.DetalleVenta;
import com.dentia.model.interno.Venta;
import com.dentia.dto.interno.ventas.ClienteDocumentoConsultaDto;
import com.dentia.service.interno.ClienteDocumentoVentaService;
import com.dentia.service.interno.DetalleVentaService;
import com.dentia.service.interno.ProductoService;
import com.dentia.service.interno.UnidadService;
import com.dentia.service.interno.SedeService;
import com.dentia.service.interno.VentaService;
import com.dentia.service.interno.CuotaVentaService;
import com.dentia.service.interno.CalculoMedidasProvisionalService;
import com.dentia.dto.interno.ventas.CalculoMedidasSolicitudDto;
import com.dentia.service.interno.InventarioService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import java.math.RoundingMode;

// PDF
import com.lowagie.text.*;
import java.awt.Color;
import com.lowagie.text.pdf.*;
import com.dentia.model.interno.CuotaVenta;
import org.springframework.core.io.ClassPathResource;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.LinkedHashMap;

import com.dentia.model.interno.PagoCuota;
import com.dentia.service.interno.PagoCuotaService;

import java.security.Principal;

@Controller
@RequestMapping("/interno/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final CuotaVentaService cuotaVentaService;
    private final PagoCuotaService pagoCuotaService;
    private final DetalleVentaService detalleService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final UnidadService unidadService;
    private final IDetalleVentaDao detalleDao;
    private final SedeService sedeService;
    private final ClienteDocumentoVentaService clienteDocumentoVentaService;
    private final CalculoMedidasProvisionalService calculoMedidasProvisionalService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VentaController(
            VentaService ventaService,
            DetalleVentaService detalleService,
            ProductoService productoService,
            UnidadService unidadService,
            IDetalleVentaDao detalleDao,
            CuotaVentaService cuotaVentaService,
            PagoCuotaService pagoCuotaService,
            SedeService sedeService,
            ClienteDocumentoVentaService clienteDocumentoVentaService,
            CalculoMedidasProvisionalService calculoMedidasProvisionalService,
            InventarioService inventarioService
    ) {
        this.ventaService = ventaService;
        this.detalleService = detalleService;
        this.productoService = productoService;
        this.unidadService = unidadService;
        this.detalleDao = detalleDao;
        this.cuotaVentaService = cuotaVentaService;
        this.pagoCuotaService = pagoCuotaService;
        this.sedeService = sedeService;
        this.clienteDocumentoVentaService = clienteDocumentoVentaService;
        this.calculoMedidasProvisionalService = calculoMedidasProvisionalService;
        this.inventarioService = inventarioService;
    }

    private String normalizarCortesJson(String cortesJson, boolean requierePreparacion) {

        if (!requierePreparacion) {
            return null;
        }

        if (cortesJson == null || cortesJson.trim().isEmpty()) {
            return null;
        }

        String jsonPlano = decodificarBase64UrlSiAplica(cortesJson);

        try {
            Object json = objectMapper.readValue(jsonPlano, Object.class);
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("El JSON de cortes no es válido: " + jsonPlano);
        }
    }
    private String decodificarBase64UrlSiAplica(String valor) {

        if (valor == null || valor.isBlank()) {
            return valor;
        }

        if (!valor.startsWith("B64:")) {
            return valor;
        }

        String base64Url = valor.substring(4);

        int padding = (4 - base64Url.length() % 4) % 4;
        base64Url = base64Url + "=".repeat(padding);

        byte[] decoded = Base64.getUrlDecoder().decode(base64Url);

        return new String(decoded, StandardCharsets.UTF_8);
    }


    @PostMapping("/api/calcular-medidas-provisional")
    @ResponseBody
    public ResponseEntity<?> calcularMedidasProvisional(
            @RequestBody CalculoMedidasSolicitudDto solicitud,
            HttpSession session) {
        try {
            Integer idSede = obtenerSedeActivaId(session);
            return ResponseEntity.ok(calculoMedidasProvisionalService.calcular(solicitud, idSede));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "mensaje", ex.getMessage(),
                    "errores", List.of(ex.getMessage())
            ));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "ok", false,
                    "mensaje", "No fue posible calcular las medidas.",
                    "errores", List.of("Ocurrió un error interno durante el cálculo.")
            ));
        }
    }

    private Integer obtenerSedeActivaId(HttpSession session) {
        if (session == null) {
            return null;
        }

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

    @GetMapping
    public String listar(
            @RequestParam(required = false, defaultValue = "TODOS") String canal,
            @RequestParam(required = false, defaultValue = "TODOS") String estado,
            @RequestParam(required = false, defaultValue = "TODOS") String estadoEntrega,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);
        String sedeActivaNombre = (String) session.getAttribute("sedeActivaNombre");

        if (sedeActivaId == null) {
            model.addAttribute("errorSedeActiva", "No se encontró una sede activa en tu sesión.");
            model.addAttribute("titulo", "Ventas | DentIA");
            model.addAttribute("ventas", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("startItem", 0);
            model.addAttribute("endItem", 0);
            model.addAttribute("totalItems", 0);

            model.addAttribute("canalSeleccionado", canal);
            model.addAttribute("estadoSeleccionado", estado);
            model.addAttribute("estadoEntregaSeleccionado", estadoEntrega);
            model.addAttribute("buscar", buscar);
            model.addAttribute("fechaDesde", fechaDesde);
            model.addAttribute("fechaHasta", fechaHasta);

            model.addAttribute("totalBorradores", 0);
            model.addAttribute("totalCompletadas", 0);
            model.addAttribute("totalAnuladas", 0);
            model.addAttribute("totalPendientesEntrega", 0);
            model.addAttribute("totalPreparandoEntrega", 0);
            model.addAttribute("totalListosEntrega", 0);
            model.addAttribute("totalEntregadosEntrega", 0);
            model.addAttribute("totalVendido", BigDecimal.ZERO);
            model.addAttribute("dineroRecibido", BigDecimal.ZERO);
            model.addAttribute("creditoPendiente", BigDecimal.ZERO);
            model.addAttribute("totalOnline", 0);
            model.addAttribute("totalPresencial", 0);
            model.addAttribute("porcentajeOnline", 0);
            model.addAttribute("porcentajePresencial", 0);

            model.addAttribute("sedeListadoId", null);
            model.addAttribute("sedeListadoNombre", "Sin sede activa");
            model.addAttribute("mensajeSedeListado", "No se encontró una sede activa para filtrar ventas.");

            return "interno/ventas/list";
        }

        List<Venta> ventas = ventaService.filtrarPorSede(
                sedeActivaId,
                canal,
                estado,
                estadoEntrega,
                buscar,
                fechaDesde,
                fechaHasta
        );

        int pageSize = 10;

        int totalVentas = ventas.size();

        int fromIndex = page * pageSize;

        int toIndex = Math.min(fromIndex + pageSize, totalVentas);

        List<Venta> ventasPaginadas = ventas.subList(fromIndex, toIndex);
        ventasPaginadas.forEach(ventaService::prepararPermisoAnulacion);

        long totalBorradores = ventas.stream()
                .filter(v -> "BORRADOR".equals(v.getEstadoVenta()))
                .count();

        long totalCompletadas = ventas.stream()
                .filter(v -> "PAGADO".equals(v.getEstadoVenta()))
                .count();

        long totalAnuladas = ventas.stream()
                .filter(v -> "ANULADA".equals(v.getEstadoVenta()))
                .count();

        long totalOnline = ventas.stream()
                .filter(v -> "ONLINE".equals(v.getCanalVenta()))
                .count();

        long totalPresencial = ventas.stream()
                .filter(v -> "PRESENCIAL".equals(v.getCanalVenta()))
                .count();

        long totalCanales = totalOnline + totalPresencial;

        int porcentajeOnline = totalCanales == 0
                ? 0
                : (int) Math.round((totalOnline * 100.0) / totalCanales);

        int porcentajePresencial = totalCanales == 0
                ? 0
                : 100 - porcentajeOnline;

        long totalPendientesEntrega = ventas.stream()
                .filter(v -> "PENDIENTE".equals(v.getNombreEstadoPedido()))
                .count();

        long totalPreparandoEntrega = ventas.stream()
                .filter(v -> "PREPARANDO".equals(v.getNombreEstadoPedido()))
                .count();

        long totalListosEntrega = ventas.stream()
                .filter(v -> "LISTO_RECOJO".equals(v.getNombreEstadoPedido()))
                .count();

        long totalEntregadosEntrega = ventas.stream()
                .filter(v -> "ENTREGADO".equals(v.getNombreEstadoPedido()))
                .count();

        List<Venta> ventasValidas = ventas.stream()
                .filter(v -> !"BORRADOR".equalsIgnoreCase(v.getEstadoVenta()))
                .filter(v -> !"ANULADA".equalsIgnoreCase(v.getEstadoVenta()))
                .toList();

        BigDecimal totalVendido = ventasValidas.stream()
                .filter(v -> v.getTotalVenta() != null)
                .map(Venta::getTotalVenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditoPendiente = ventasValidas.stream()
                .filter(v -> "CREDITO".equalsIgnoreCase(v.getFormaPago()))
                .filter(v -> v.getDeuda() != null)
                .map(Venta::getDeuda)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dineroRecibido = ventasValidas.stream()
                .map(v -> {
                    BigDecimal total = v.getTotalVenta() == null ? BigDecimal.ZERO : v.getTotalVenta();

                    if ("CREDITO".equalsIgnoreCase(v.getFormaPago())) {
                        BigDecimal deuda = v.getDeuda() == null ? BigDecimal.ZERO : v.getDeuda();
                        return total.subtract(deuda);
                    }

                    return total;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        model.addAttribute("titulo", "Ventas | DentIA");

        model.addAttribute("ventas", ventasPaginadas);
        int totalPages = (int) Math.ceil((double) totalVentas / pageSize);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        int startItem = fromIndex + 1;
        int endItem = toIndex;

        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);
        model.addAttribute("totalItems", totalVentas);

        model.addAttribute("canalSeleccionado", canal);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("estadoEntregaSeleccionado", estadoEntrega);
        model.addAttribute("buscar", buscar);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);

        model.addAttribute("totalBorradores", totalBorradores);
        model.addAttribute("totalCompletadas", totalCompletadas);
        model.addAttribute("totalAnuladas", totalAnuladas);
        model.addAttribute("totalPendientesEntrega", totalPendientesEntrega);
        model.addAttribute("totalPreparandoEntrega", totalPreparandoEntrega);
        model.addAttribute("totalListosEntrega", totalListosEntrega);
        model.addAttribute("totalEntregadosEntrega", totalEntregadosEntrega);
        model.addAttribute("totalVendido", totalVendido);
        model.addAttribute("dineroRecibido", dineroRecibido);

        model.addAttribute("creditoPendiente", creditoPendiente);
        model.addAttribute("totalOnline", totalOnline);
        model.addAttribute("totalPresencial", totalPresencial);

        model.addAttribute("porcentajeOnline", porcentajeOnline);
        model.addAttribute("porcentajePresencial", porcentajePresencial);

        model.addAttribute("sedeListadoId", sedeActivaId);
        model.addAttribute("sedeListadoNombre", sedeActivaNombre);
        model.addAttribute("mensajeSedeListado", "Mostrando ventas registradas para la sede activa.");

        return "interno/ventas/list";
    }

    @GetMapping("/nueva")
    public String nuevaVenta(Model model, HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/dashboard?sinSedeActiva";
        }

        Venta venta = new Venta();
        venta.setIdVentas(null);
        venta.setEstadoVenta("BORRADOR");
        venta.setIdSede(sedeActivaId);

        model.addAttribute("ventaForm", venta);
        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("unidades", unidadService.listar());
        model.addAttribute("sedes", sedeService.listar());

        return "interno/ventas/form";
    }

    @GetMapping("/api/clientes/documento/{documento}")
    @ResponseBody
    public ResponseEntity<?> consultarClientePorDocumento(@PathVariable String documento) {
        try {
            ClienteDocumentoConsultaDto resultado = clienteDocumentoVentaService.consultar(documento);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", ex.getMessage()));
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable int id, Model model, HttpSession session) {

        Venta venta = ventaService.buscarPorId(id);
        if (venta == null || !"BORRADOR".equals(venta.getEstadoVenta()))
            return "redirect:/interno/ventas?error";

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/dashboard?sinSedeActiva";
        }

        if (venta.getIdSede() != null && !venta.getIdSede().equals(sedeActivaId)) {
            return "redirect:/interno/ventas?ventaOtraSede=true";
        }

        model.addAttribute("ventaForm", venta);
        model.addAttribute("detalles", detalleService.listarPorVenta(id));
        model.addAttribute("productos", productoService.listarActivos());
        model.addAttribute("unidades", unidadService.listar());
        model.addAttribute("sedes", sedeService.listar());

        return "interno/ventas/form";
    }

    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable int id, Model model) {

        Venta venta = ventaService.buscarPorId(id);
        if (venta == null) return "redirect:/interno/ventas?error";

        List<DetalleVenta> detalles = detalleService.listarPorVenta(id);

        BigDecimal subtotalBruto = detalles.stream()
                .map(d -> d.getCantidad().multiply(d.getPrecioUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal descuentoTotal = detalles.stream()
                .map(d -> d.getDescuento() == null ? BigDecimal.ZERO : d.getDescuento())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("venta", venta);
        model.addAttribute("detalles", detalles);
        model.addAttribute("subtotalBruto", subtotalBruto);
        model.addAttribute("descuentoTotal", descuentoTotal);
        boolean esNotaVenta = "NOTA".equalsIgnoreCase(venta.getTipoComprobante());

        BigDecimal totalVenta = venta.getTotalVenta() == null
                ? BigDecimal.ZERO
                : venta.getTotalVenta();

        BigDecimal subtotalGravado;
        BigDecimal igv;

        if (esNotaVenta) {
            subtotalGravado = totalVenta;
            igv = BigDecimal.ZERO;
        } else {
            subtotalGravado = totalVenta
                    .divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);

            igv = totalVenta.subtract(subtotalGravado);
        }

        BigDecimal totalItems = detalles.stream()
                .map(DetalleVenta::getCantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("subtotalGravado", subtotalGravado);
        model.addAttribute("igv", igv);
        model.addAttribute("totalItems", totalItems);

        model.addAttribute("cuotas",
                cuotaVentaService.listarPorVenta(id));


        if ("CREDITO".equalsIgnoreCase(venta.getFormaPago())) {
            model.addAttribute("cuotas", cuotaVentaService.listarPorVenta(id));
        }

        return "interno/ventas/detalle";
    }

    @GetMapping("/disponibilidad-producto/{idProducto}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDisponibilidadProducto(
            @PathVariable Integer idProducto,
            HttpSession session
    ) {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        try {
            if (idProducto == null || idProducto <= 0) {
                throw new IllegalArgumentException("El producto seleccionado no es válido.");
            }

            Integer idSede = obtenerSedeActivaId(session);

            if (idSede == null || idSede <= 0) {
                throw new IllegalArgumentException(
                        "No existe una sede activa para consultar el inventario."
                );
            }

            BigDecimal stockDisponible =
                    inventarioService.obtenerStockDisponibleParaVenta(
                            idProducto,
                            idSede
                    );

            InventarioService.DisponibilidadFisicaProducto disponibilidad =
                    inventarioService.obtenerDisponibilidadFisicaProducto(
                            idProducto,
                            idSede
                    );

            int completas = Math.max(
                    0,
                    disponibilidad.completasDisponibles()
            );

            int mediasDirectas = Math.max(
                    0,
                    disponibilidad.mediasDisponibles()
            );

            int mediasPotenciales =
                    mediasDirectas + (completas * 2);

            respuesta.put("ok", true);

            respuesta.put(
                    "stockDisponible",
                    stockDisponible == null
                            ? BigDecimal.ZERO
                            : stockDisponible.max(BigDecimal.ZERO)
            );

            respuesta.put(
                    "completasDisponibles",
                    completas
            );

            respuesta.put(
                    "mediasDirectas",
                    mediasDirectas
            );

            respuesta.put(
                    "mediasPotenciales",
                    mediasPotenciales
            );

            respuesta.put(
                    "retazosDisponibles",
                    Math.max(
                            0,
                            disponibilidad.retazosDisponibles()
                    )
            );

            respuesta.put(
                    "medidaVendibleTotal",
                    Math.max(0, disponibilidad.medidaVendibleTotal())
            );

            respuesta.put(
                    "unidadMedida",
                    disponibilidad.unidadMedida() == null
                            ? ""
                            : disponibilidad.unidadMedida()
            );

            respuesta.put(
                    "tipoMedida",
                    disponibilidad.tipoMedida() == null
                            ? ""
                            : disponibilidad.tipoMedida()
            );

            return ResponseEntity.ok(respuesta);

        } catch (IllegalArgumentException ex) {
            respuesta.put("ok", false);
            respuesta.put("mensaje", ex.getMessage());

            return ResponseEntity.badRequest().body(respuesta);

        } catch (Exception ex) {
            respuesta.put("ok", false);
            respuesta.put(
                    "mensaje",
                    "No se pudo consultar la disponibilidad física del producto."
            );

            return ResponseEntity.internalServerError().body(respuesta);
        }
    }

    @PostMapping("/guardar")
    @Transactional
    public String guardar(@ModelAttribute("ventaForm") Venta venta,
                          @RequestParam(value = "idProducto", required = false) List<Integer> idProducto,
                          @RequestParam(value = "idUnidad", required = false) List<Integer> idUnidad,
                          @RequestParam(value = "idPrecioPresentacion", required = false) List<Integer> idPrecioPresentacion,
                          @RequestParam(value = "factorConversionStock", required = false) List<BigDecimal> factorConversionStock,
                          @RequestParam(value = "cantidad", required = false) List<BigDecimal> cantidad,
                          @RequestParam(value = "precioUnitario", required = false) List<BigDecimal> precio,
                          @RequestParam(value = "descuento", required = false) List<BigDecimal> descuento,
                          @RequestParam(value = "requierePreparacion", required = false) List<Integer> requierePreparacion,
                          @RequestParam(value = "tipoPreparacion", required = false) List<String> tipoPreparacion,
                          @RequestParam(value = "entregarSobrante", required = false) List<Integer> entregarSobrante,
                          @RequestParam(value = "observacionPreparacion", required = false) List<String> observacionPreparacion,
                          @RequestParam(value = "cortesJson", required = false) String[] cortesJson,
                          @RequestParam(value = "croquisSvg", required = false) String[] croquisSvg,
                          @RequestParam(value = "grupoMedidaUuid", required = false) List<String> grupoMedidaUuid,
                          @RequestParam(value = "lineaPrincipalMedida", required = false) List<Integer> lineaPrincipalMedida,
                          @RequestParam(value = "pagoInicial", required = false) BigDecimal pagoInicial,
                          @RequestParam(value = "deudaVenta", required = false) BigDecimal deuda,
                          @RequestParam(value = "nroCuota", required = false) List<Integer> nroCuota,
                          @RequestParam(value = "fechaVencimiento", required = false) List<String> fechaVencimiento,
                          @RequestParam(value = "montoCuota", required = false) List<BigDecimal> montoCuota,
                          @RequestParam(value = "tipoEntrega", required = false, defaultValue = "INMEDIATA") String tipoEntrega,
                          @RequestParam(value = "fechaRecojoProgramadaInput", required = false) String fechaRecojoProgramadaInput,
                          @RequestParam("accion") String accion,
                          HttpSession session) {

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

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return "redirect:/interno/dashboard?sinSedeActiva";
        }

        venta.setIdSede(sedeActivaId);

        String tipoComprobante = venta.getTipoComprobante();

        if (tipoComprobante == null || tipoComprobante.isBlank()) {
            tipoComprobante = "NOTA";
        }

        tipoComprobante = tipoComprobante.trim().toUpperCase();

        if ("NOTA DE VENTA".equals(tipoComprobante)) {
            tipoComprobante = "NOTA";
        }

        venta.setTipoComprobante(tipoComprobante);

        BigDecimal totalCalculado = BigDecimal.ZERO;

        for (int i = 0; i < idProducto.size(); i++) {

            BigDecimal cant = cantidad.get(i);
            BigDecimal prec = precio.get(i);
            BigDecimal desc = descuento.get(i) == null ? BigDecimal.ZERO : descuento.get(i);

            BigDecimal subtotal = cant.multiply(prec).subtract(desc);

            totalCalculado = totalCalculado.add(subtotal);
        }

        venta.setTotalVenta(totalCalculado);

        if ("CREDITO".equalsIgnoreCase(venta.getFormaPago())) {
            BigDecimal inicial = pagoInicial == null ? BigDecimal.ZERO : pagoInicial;
            venta.setDeuda(totalCalculado.subtract(inicial));
        } else {
            venta.setDeuda(BigDecimal.ZERO);
        }

        boolean editandoBorrador = venta.getIdVentas() != null && venta.getIdVentas() > 0;
        int idVenta = ventaService.guardarBorradorConCliente(venta);
        if (editandoBorrador) detalleDao.eliminarPorVenta(idVenta);

        int detallesGuardados = 0;

        for (int i = 0; i < idProducto.size(); i++) {

            Integer unidad = (idUnidad != null && i < idUnidad.size())
                    ? idUnidad.get(i)
                    : null;

            if (unidad == null || unidad <= 0) {
                throw new IllegalArgumentException("La fila " + (i + 1)
                        + " no tiene una unidad de venta válida.");
            }

            Integer presentacion = (idPrecioPresentacion != null && i < idPrecioPresentacion.size())
                    ? idPrecioPresentacion.get(i)
                    : null;

            BigDecimal factor = (factorConversionStock != null && i < factorConversionStock.size())
                    ? factorConversionStock.get(i)
                    : BigDecimal.ONE;

            if (presentacion == null || presentacion <= 0) {
                throw new IllegalArgumentException("La fila " + (i + 1)
                        + " no tiene una presentación de venta válida.");
            }

            DetalleVenta d = new DetalleVenta();
            d.setIdVenta(idVenta);
            d.setIdProducto(idProducto.get(i));
            d.setIdUnidad(unidad);
            d.setIdPrecioPresentacion(presentacion);
            d.setFactorConversionStock(factor);
            d.setCantidad(cantidad.get(i));
            d.setPrecioUnitario(precio.get(i));
            d.setDescuento(descuento.get(i) == null ? BigDecimal.ZERO : descuento.get(i));

// ============================
// PREPARACIÓN / CORTE
// ============================
            Integer reqPrep = (requierePreparacion != null && i < requierePreparacion.size())
                    ? requierePreparacion.get(i)
                    : 0;

            String tipoPrep = (tipoPreparacion != null && i < tipoPreparacion.size())
                    ? tipoPreparacion.get(i)
                    : "TAL_CUAL";

            Integer sobrante = (entregarSobrante != null && i < entregarSobrante.size())
                    ? entregarSobrante.get(i)
                    : 0;

            String obsPrep = (observacionPreparacion != null && i < observacionPreparacion.size())
                    ? observacionPreparacion.get(i)
                    : null;

            String cortes = (cortesJson != null && i < cortesJson.length)
                    ? cortesJson[i]
                    : null;

            String croquis = (croquisSvg != null && i < croquisSvg.length)
                    ? croquisSvg[i]
                    : null;

            boolean requiere = reqPrep != null && reqPrep == 1;

            if (tipoPrep == null || tipoPrep.isBlank()) {
                tipoPrep = requiere ? "CORTE" : "TAL_CUAL";
            }

            if (!requiere) {
                tipoPrep = "TAL_CUAL";
                cortes = null;

                if (croquis != null && !croquis.isBlank()) {
                    croquis = decodificarBase64UrlSiAplica(croquis);
                } else {
                    croquis = null;
                }

            } else {
                tipoPrep = "CORTE";
                cortes = normalizarCortesJson(cortes, true);
                croquis = decodificarBase64UrlSiAplica(croquis);
            }

            d.setRequierePreparacion(requiere);
            d.setTipoPreparacion(tipoPrep);
            d.setEntregarSobrante(sobrante != null && sobrante == 1);
            d.setObservacionPreparacion(obsPrep);
            d.setCortesJson(cortes);
            d.setCroquisSvg(croquis);

            String grupoUuid = (grupoMedidaUuid != null && i < grupoMedidaUuid.size())
                    ? grupoMedidaUuid.get(i) : null;
            Integer principalMedida = (lineaPrincipalMedida != null && i < lineaPrincipalMedida.size())
                    ? lineaPrincipalMedida.get(i) : 0;
            d.setGrupoMedidaUuid(grupoUuid == null || grupoUuid.isBlank() ? null : grupoUuid.trim());
            d.setLineaPrincipalMedida(principalMedida != null && principalMedida == 1);

            ventaService.agregarDetalle(idVenta, d);
            detallesGuardados++;
        }

        if (detallesGuardados == 0) {
            return "redirect:/interno/ventas?sinDetalles=true";
        }



        ventaService.actualizarTotalesVenta(idVenta);
        if ("CREDITO".equalsIgnoreCase(venta.getFormaPago())) {

            cuotaVentaService.registrarCuotas(
                    idVenta,
                    nroCuota,
                    fechaVencimiento,
                    montoCuota
            );
        }

        if ("REGISTRAR".equalsIgnoreCase(accion.trim())) {

            Venta ventaActual = ventaService.buscarPorId(idVenta);

            if ("BORRADOR".equalsIgnoreCase(ventaActual.getEstadoVenta())) {
                ventaService.confirmarVenta(idVenta);
            }

            if ("CREDITO".equalsIgnoreCase(venta.getFormaPago())) {

                ventaService.registrarCredito(
                        idVenta,
                        pagoInicial == null ? BigDecimal.ZERO : pagoInicial,
                        deuda == null ? BigDecimal.ZERO : deuda
                );

            }
        }

        /* ENTREGA SIEMPRE: BORRADOR O REGISTRAR */
        if ("PROGRAMADA".equalsIgnoreCase(tipoEntrega)) {

            if (fechaRecojoProgramadaInput == null || fechaRecojoProgramadaInput.isBlank()) {
                throw new IllegalArgumentException("Debe seleccionar la fecha y hora de recojo.");
            }

            LocalDateTime fechaRecojoProgramada =
                    LocalDateTime.parse(fechaRecojoProgramadaInput);

            sedeService.validarHorarioRecojoDisponible(
                    sedeActivaId,
                    fechaRecojoProgramada.toLocalDate(),
                    fechaRecojoProgramada.toLocalTime()
            );

            ventaService.registrarEntregaProgramada(
                    idVenta,
                    fechaRecojoProgramada
            );

        } else {
            ventaService.registrarEntregaInmediata(idVenta);
        }

        if ("REGISTRAR".equalsIgnoreCase(accion.trim())) {
            return "redirect:/interno/ventas?confirmada=true";
        }

        return "redirect:/interno/ventas?borrador=true";
    }



    @PostMapping("/confirmar/{id}")
    public String confirmarVenta(@PathVariable int id) {

        Venta venta = ventaService.buscarPorId(id);

        ventaService.confirmarVenta(id);

        if (venta.getFechaRecojoProgramada() != null) {

            ventaService.registrarEntregaProgramada(
                    id,
                    venta.getFechaRecojoProgramada()
            );

        } else {

            ventaService.registrarEntregaInmediata(id);
        }

        return "redirect:/interno/ventas?confirmada=true";
    }

    @PostMapping("/anular/{id}")
    public String anularVenta(@PathVariable int id,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            ventaService.anularVenta(id);
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Venta anulada. El stock y el importe fueron revertidos correctamente."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/interno/ventas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarBorrador(@PathVariable int id) {
        ventaService.eliminarBorrador(id);
        return "redirect:/interno/ventas?eliminada=true";
    }
    @GetMapping("/pdf/{id}")
    public void exportarPdf(@PathVariable int id, HttpServletResponse response) throws IOException {

        Venta venta = ventaService.buscarPorId(id);
        if (venta == null) {
            response.sendRedirect("/interno/ventas?error");
            return;
        }

        List<DetalleVenta> detalles = detalleService.listarPorVenta(id);
        List<CuotaVenta> cuotas = cuotaVentaService.listarPorVenta(id);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=venta-" + id + ".pdf");

        Document document = new Document(PageSize.A4, 28, 28, 28, 28);
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        Font tituloFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
        Font normal = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
        Font bold = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
        Font whiteBold = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font totalFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
        Color colorMarca;

        if ("ANULADA".equalsIgnoreCase(venta.getEstadoVenta())) {

            // rojo suave
            colorMarca = new Color(220, 40, 40);

        } else if ("BORRADOR".equalsIgnoreCase(venta.getEstadoVenta())) {

            // azul/gris profesional
            colorMarca = new Color(70, 90, 120);

        } else if ("PAGADO".equalsIgnoreCase(venta.getEstadoVenta())) {

            // verde suave
            colorMarca = new Color(30, 140, 70);

        } else {

            // gris corporativo
            colorMarca = new Color(120, 120, 120);
        }

        Font marcaAguaFont = new Font(
                Font.HELVETICA,
                85,
                Font.BOLD,
                colorMarca
        );

        DateTimeFormatter fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // ==========================
        // MARCA DE AGUA
        // ==========================
        PdfContentByte canvas = writer.getDirectContentUnder();

        canvas.saveState();

        PdfGState gs = new PdfGState();
        gs.setFillOpacity(0.10f);
        canvas.setGState(gs);

/* =========================
   TEXTO MARCA DE AGUA
========================= */
        String estadoMarca;

        if ("ANULADA".equalsIgnoreCase(venta.getEstadoVenta())) {
            estadoMarca = "ANULADA";

        } else if ("BORRADOR".equalsIgnoreCase(venta.getEstadoVenta())) {
            estadoMarca = "PROFORMA";

        } else if ("PAGADO".equalsIgnoreCase(venta.getEstadoVenta())) {
            estadoMarca = "PAGADO";

        } else {
            estadoMarca = "DentIA";
        }

        Phrase marca = new Phrase(estadoMarca, marcaAguaFont);

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                marca,
                300,
                420,
                35
        );

        canvas.restoreState();


        // ==========================
        // CABECERA
        // ==========================
        PdfPTable cabecera = new PdfPTable(new float[]{2.2f, 5f, 3.2f});
        cabecera.setWidthPercentage(100);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);

        try {
            String logoPath = new ClassPathResource("static/img/logos/logodentia.png")
                    .getFile()
                    .getAbsolutePath();

            Image logo = Image.getInstance(logoPath);
            logo.scaleToFit(95, 95);
            logoCell.addElement(logo);
        } catch (Exception e) {
            logoCell.addElement(new Paragraph("DentIA", tituloFont));
        }

        cabecera.addCell(logoCell);

        PdfPCell empresaCell = new PdfPCell();
        empresaCell.setBorder(Rectangle.NO_BORDER);
        empresaCell.addElement(new Paragraph("-DentIA", tituloFont));
        empresaCell.addElement(new Paragraph("Sistema ERP de ventas e inventario", normal));
        empresaCell.addElement(new Paragraph(" ", normal));
        empresaCell.addElement(new Paragraph("Correo: inversionesdentiasac@hotmail.com", normal));
        cabecera.addCell(empresaCell);

        PdfPTable cajaComprobante = new PdfPTable(1);
        cajaComprobante.setWidthPercentage(100);

        String tipo = venta.getTipoComprobante() != null ? venta.getTipoComprobante() : "COMPROBANTE";

        String textoTipo;

        if ("BORRADOR".equalsIgnoreCase(venta.getEstadoVenta())) {

            textoTipo = "PROFORMA";

        } else if (tipo.equalsIgnoreCase("FACTURA")) {

            textoTipo = "FACTURA ELECTRÓNICA";

        } else if (tipo.equalsIgnoreCase("BOLETA")) {

            textoTipo = "BOLETA DE VENTA ELECTRÓNICA";

        } else {

            textoTipo = "NOTA DE VENTA";
        }

        cajaComprobante.addCell(celdaCaja("20487470601", new Font(Font.HELVETICA, 15, Font.BOLD)));
        cajaComprobante.addCell(celdaCaja(textoTipo, new Font(Font.HELVETICA, 14, Font.BOLD)));
        cajaComprobante.addCell(celdaCaja(venta.getNroComprobante(), new Font(Font.HELVETICA, 13, Font.BOLD)));

        PdfPCell comprobanteCell = new PdfPCell(cajaComprobante);
        comprobanteCell.setBorder(Rectangle.NO_BORDER);
        cabecera.addCell(comprobanteCell);

        document.add(cabecera);
        document.add(new Paragraph(" "));

        // ==========================
        // DATOS CLIENTE / FECHA
        // ==========================
        PdfPTable datos = new PdfPTable(new float[]{6.5f, 3.5f});
        datos.setWidthPercentage(100);

        PdfPCell cliente = new PdfPCell();
        cliente.setBorder(Rectangle.NO_BORDER);
        cliente.addElement(linea("RUC/DNI: ", venta.getDocumentoCliente(), bold, normal));
        cliente.addElement(linea("CLIENTE: ", venta.getNombreCliente(), bold, normal));
        cliente.addElement(linea("TELÉFONO: ", venta.getTelefonoCliente(), bold, normal));
        cliente.addElement(linea("DIRECCIÓN: ", "-", bold, normal));
        datos.addCell(cliente);

        PdfPCell fechas = new PdfPCell();
        fechas.setBorder(Rectangle.NO_BORDER);
        fechas.addElement(linea("F. EMISIÓN: ",
                venta.getFechaVenta() != null ? venta.getFechaVenta().format(fecha) : "-",
                bold, normal));
        fechas.addElement(linea("MONEDA: ", "SOLES", bold, normal));
        fechas.addElement(linea("FORMA PAGO: ", venta.getFormaPago(), bold, normal));
        fechas.addElement(linea("CANAL: ", venta.getCanalVenta(), bold, normal));
        fechas.addElement(linea("SEDE: ", venta.getNombreSede(), bold, normal));
        datos.addCell(fechas);

        document.add(datos);
        document.add(new Paragraph(" "));

        // ==========================
        // PRODUCTOS
        // ==========================
        PdfPTable tabla = new PdfPTable(new float[]{0.8f, 1.1f, 1.4f, 5f, 1.5f, 1.5f});
        tabla.setWidthPercentage(100);

        tabla.addCell(headerPdf("NRO.", whiteBold));
        tabla.addCell(headerPdf("CANT.", whiteBold));
        tabla.addCell(headerPdf("UNIDAD", whiteBold));
        tabla.addCell(headerPdf("DESCRIPCIÓN", whiteBold));
        tabla.addCell(headerPdf("P. UNIT.", whiteBold));
        tabla.addCell(headerPdf("TOTAL", whiteBold));

        BigDecimal subtotalBruto = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;

        int contador = 1;

        for (DetalleVenta d : detalles) {

            BigDecimal desc = d.getDescuento() == null ? BigDecimal.ZERO : d.getDescuento();

            BigDecimal bruto = d.getCantidad().multiply(d.getPrecioUnitario());
            BigDecimal sub = bruto.subtract(desc);

            subtotalBruto = subtotalBruto.add(bruto);
            descuentoTotal = descuentoTotal.add(desc);

            tabla.addCell(bodyPdf(String.valueOf(contador++), normal, Element.ALIGN_CENTER));
            tabla.addCell(bodyPdf(d.getCantidad().toString(), normal, Element.ALIGN_CENTER));
            tabla.addCell(bodyPdf(d.getNombreUnidad(), normal, Element.ALIGN_LEFT));
            tabla.addCell(bodyPdf(d.getNombreProducto(), normal, Element.ALIGN_LEFT));
            tabla.addCell(bodyPdf("S/ " + valor(d.getPrecioUnitario()), normal, Element.ALIGN_RIGHT));
            tabla.addCell(bodyPdf("S/ " + valor(sub), normal, Element.ALIGN_RIGHT));
        }

        document.add(tabla);

        // ==========================
        // TOTALES
        // ==========================
        BigDecimal total = venta.getTotalVenta() == null ? BigDecimal.ZERO : venta.getTotalVenta();

        boolean esNotaVenta = "NOTA".equalsIgnoreCase(venta.getTipoComprobante());

        BigDecimal subtotalGravado;
        BigDecimal igv;

        if (esNotaVenta) {
            subtotalGravado = total;
            igv = BigDecimal.ZERO;
        } else {
            subtotalGravado = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
            igv = total.subtract(subtotalGravado);
        }

        PdfPTable totales = new PdfPTable(new float[]{6.5f, 3.5f});
        totales.setWidthPercentage(100);

        PdfPCell letras = new PdfPCell(new Phrase("TOTAL EN SOLES: S/ " + total, bold));
        letras.setPadding(7);
        totales.addCell(letras);

        PdfPTable resumen = new PdfPTable(new float[]{1.6f, 0.4f, 1.2f});
        resumen.setWidthPercentage(100);

        resumen.addCell(totalLabel(esNotaVenta ? "IMPORTE" : "GRAVADO", bold));
        resumen.addCell(totalLabel("S/", bold));
        resumen.addCell(totalValor(valor(subtotalGravado), bold));

        if (!esNotaVenta) {
            resumen.addCell(totalLabel("IGV", bold));
            resumen.addCell(totalLabel("S/", bold));
            resumen.addCell(totalValor(valor(igv), bold));
        }

        resumen.addCell(totalLabel("TOTAL", totalFont));
        resumen.addCell(totalLabel("S/", totalFont));
        resumen.addCell(totalValor(valor(total), totalFont));

        PdfPCell resumenCell = new PdfPCell(resumen);
        resumenCell.setPadding(0);
        totales.addCell(resumenCell);

        document.add(totales);
        document.add(new Paragraph(" "));

        // ==========================
        // CRÉDITO
        // ==========================
        if ("CREDITO".equalsIgnoreCase(venta.getFormaPago())) {

            PdfPTable credito = new PdfPTable(2);
            credito.setWidthPercentage(100);
            credito.setSpacingBefore(8);

            credito.addCell(infoPdf("Pago inicial", "S/ " + valor(venta.getPagoInicial()), bold, normal));
            credito.addCell(infoPdf("Deuda pendiente", "S/ " + valor(venta.getDeuda()), bold, normal));
            credito.addCell(infoPdf("N° cuotas", String.valueOf(venta.getNumeroCuotas()), bold, normal));
            credito.addCell(infoPdf("Frecuencia", venta.getFrecuenciaCuotas(), bold, normal));

            document.add(new Paragraph("INFORMACIÓN DEL CRÉDITO", bold));
            document.add(credito);

            if (cuotas != null && !cuotas.isEmpty()) {
                document.add(new Paragraph(" "));
                Paragraph tituloCuotas = new Paragraph("CRONOGRAMA DE CUOTAS", bold);
                tituloCuotas.setSpacingBefore(10);
                tituloCuotas.setSpacingAfter(8);
                document.add(tituloCuotas);

                PdfPTable tablaCuotas = new PdfPTable(new float[]{1.2f, 2f, 2f, 2f});
                tablaCuotas.setWidthPercentage(100);

                tablaCuotas.addCell(headerPdf("CUOTA", whiteBold));
                tablaCuotas.addCell(headerPdf("VENCIMIENTO", whiteBold));
                tablaCuotas.addCell(headerPdf("MONTO", whiteBold));
                tablaCuotas.addCell(headerPdf("ESTADO", whiteBold));

                for (CuotaVenta c : cuotas) {
                    tablaCuotas.addCell(bodyPdf("Cuota " + c.getNroCuota(), normal, Element.ALIGN_CENTER));
                    tablaCuotas.addCell(bodyPdf(c.getFechaVencimiento() != null ? c.getFechaVencimiento().format(fecha) : "-", normal, Element.ALIGN_CENTER));
                    tablaCuotas.addCell(bodyPdf("S/ " + c.getMontoCuota(), normal, Element.ALIGN_RIGHT));
                    tablaCuotas.addCell(bodyPdf(c.getEstado(), normal, Element.ALIGN_CENTER));
                }

                document.add(tablaCuotas);
            }
        }

        // ==========================
        // ONLINE / RECOJO
        // ==========================
        if (venta.getCodigoRecojo() != null && !venta.getCodigoRecojo().isBlank()) {
            Paragraph espacioRecojo = new Paragraph(" ");
            espacioRecojo.setSpacingBefore(18);
            document.add(espacioRecojo);

            Paragraph tituloRecojo = new Paragraph("DATOS DE RECOJO", bold);
            tituloRecojo.setSpacingAfter(12);
            document.add(tituloRecojo);

            PdfPTable recojo = new PdfPTable(2);
            recojo.getDefaultCell().setBorderColor(new Color(210, 210, 210));
            recojo.setWidthPercentage(100);
            recojo.setSpacingBefore(6);

            recojo.addCell(infoPdf("CÓDIGO DE RECOJO", venta.getCodigoRecojo(), bold, totalFont));
            recojo.addCell(infoPdf("Estado entrega", venta.getNombreEstadoPedido(), bold, normal));
            recojo.addCell(infoPdf("Recojo programado",
                    venta.getFechaRecojoProgramada() != null ? venta.getFechaRecojoProgramada().format(fechaHora) : "-",
                    bold, normal));
            recojo.addCell(infoPdf("Observación", venta.getObservacionRecojo(), bold, normal));

            document.add(recojo);
        }

        // ==========================
        // PIE
        // ==========================
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Usuario que realizo la venta: " + valorTexto(venta.getNombreUsuario()), normal));
        document.add(new Paragraph("Gracias por su compra.", bold));

        document.close();
    }

    private PdfPCell headerPdf(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBackgroundColor(Color.BLACK);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell bodyPdf(String texto, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(valorTexto(texto), font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(7);
        return cell;
    }

    private PdfPCell celdaCaja(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(valorTexto(texto), font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        return cell;
    }

    private Paragraph linea(String label, String value, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, labelFont));
        p.add(new Chunk(valorTexto(value), valueFont));
        return p;
    }

    private PdfPCell infoPdf(String label, String value, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", labelFont));
        p.add(new Chunk(valorTexto(value), valueFont));

        PdfPCell cell = new PdfPCell(p);
        cell.setPaddingTop(8);
        cell.setPaddingBottom(8);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        return cell;
    }

    private PdfPCell totalLabel(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell totalValor(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(3);
        return cell;
    }

    private String valor(BigDecimal valor) {
        return valor == null ? "0.00" : valor.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String valorTexto(String texto) {
        return texto == null || texto.trim().isEmpty() ? "-" : texto;
    }

    @GetMapping("/siguiente-numero")
    @ResponseBody
    public String obtenerSiguienteNumero(@RequestParam String tipo) {
        return ventaService.obtenerSiguienteComprobante(tipo);
    }
    @GetMapping("/horarios-disponibles")
    @ResponseBody
    public List<Map<String, Object>> horariosDisponibles(@RequestParam("fecha") String fecha,
                                                         HttpSession session) {

        Integer sedeActivaId = obtenerSedeActivaId(session);

        if (sedeActivaId == null) {
            return List.of();
        }

        if (fecha == null || fecha.isBlank()) {
            return List.of();
        }

        LocalDate fechaRecojo = LocalDate.parse(fecha);

        return sedeService.listarHorariosDisponiblesConCupos(sedeActivaId, fechaRecojo);
    }


    @GetMapping("/cuotas/{id}")
    public String verCuotasVenta(@PathVariable Integer id,
                                 Model model) {

        model.addAttribute(
                "cuotas",
                cuotaVentaService.listarPorVenta(id)
        );

        model.addAttribute("idVenta", id);

        return "interno/ventas/cuotas";
    }

    @PostMapping("/cuotas/pagar")
    public String pagarCuota(
            @RequestParam Integer idCuotaVenta,
            @RequestParam BigDecimal montoPago,
            @RequestParam String metodoPago,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) String observacion,
            Principal principal
    ) {

        CuotaVenta cuota = cuotaVentaService.buscarPorId(idCuotaVenta);

        if (cuota == null) {
            throw new IllegalArgumentException("La cuota no existe.");
        }

        Integer idUsuario = ventaService
                .obtenerIdUsuarioPorNombre(principal.getName());

        PagoCuota pago = new PagoCuota();

        pago.setIdCuotaVenta(cuota.getIdCuotaVenta());
        pago.setIdVenta(cuota.getIdVenta());

        pago.setMontoPagado(montoPago);

        pago.setMetodoPago(metodoPago);

        pago.setReferencia(referencia);
        pago.setObservacion(observacion);

        pago.setIdUsuario(idUsuario);

        cuotaVentaService.registrarPagoCuota(cuota, pago);

        return "redirect:/interno/ventas/cuotas/" + cuota.getIdVenta() + "?pagoRegistrado=true";
    }

}
