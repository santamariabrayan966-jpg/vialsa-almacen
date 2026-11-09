package com.dentia.service.interno;

import com.dentia.dao.interfaces.IDetalleVentaDao;
import com.dentia.dao.interfaces.IVentaDao;
import com.dentia.dto.interno.ventas.ClienteVentaSolicitudDto;
import com.dentia.model.interno.DetalleVenta;
import com.dentia.model.interno.Venta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dentia.model.interno.Cliente;
import com.dentia.service.interno.ClienteService;
import com.dentia.model.interno.ProductoPrecioPresentacion;
import com.dentia.model.interno.Producto;
import java.math.RoundingMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class VentaService {

    private final IVentaDao ventaDao;
    private final IDetalleVentaDao detalleDao;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final ProductoPrecioPresentacionService precioPresentacionService;
    private final VentaMedidasConfirmacionService ventaMedidasConfirmacionService;

    private static final Integer ID_UNIDAD_PLANCHA = 4;
    private static final Integer ID_UNIDAD_PIE2 = 13;

    private final ClienteService clienteService;

    @Autowired
    public VentaService(
            IVentaDao ventaDao,
            IDetalleVentaDao detalleDao,
            ProductoService productoService,
            InventarioService inventarioService,
            ProductoPrecioPresentacionService precioPresentacionService,
            ClienteService clienteService,
            VentaMedidasConfirmacionService ventaMedidasConfirmacionService
    ) {
        this.ventaDao = ventaDao;
        this.detalleDao = detalleDao;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
        this.precioPresentacionService = precioPresentacionService;
        this.clienteService = clienteService;
        this.ventaMedidasConfirmacionService = ventaMedidasConfirmacionService;
    }

    // Constructor conservado para las pruebas unitarias existentes de la fase 1A.
    VentaService(
            IVentaDao ventaDao,
            IDetalleVentaDao detalleDao,
            ProductoService productoService,
            InventarioService inventarioService,
            ProductoPrecioPresentacionService precioPresentacionService,
            ClienteService clienteService
    ) {
        this(ventaDao, detalleDao, productoService, inventarioService,
                precioPresentacionService, clienteService, null);
    }

    // ============================================================
    // LISTAR Y CONSULTAR
    // ============================================================
    public List<Venta> listar() {
        return ventaDao.listar();
    }

    public BigDecimal obtenerStockDisponibleParaVenta(Integer idProducto, Integer idSede) {
        return inventarioService.obtenerStockDisponibleParaVenta(idProducto, idSede);
    }

    public Venta buscarPorId(int idVenta) {
        Venta v = ventaDao.buscarPorId(idVenta);

        if (v != null) {
            List<DetalleVenta> detalles = detalleDao.listarPorVenta(idVenta);

            for (DetalleVenta d : detalles) {

                boolean tienePreparacion =
                        d.isRequierePreparacion()
                                || "CORTE".equalsIgnoreCase(d.getTipoPreparacion())
                                || d.getCortesJson() != null
                                || d.getCroquisSvg() != null;

                if (tienePreparacion) {
                    d.setInstruccionMaterial(
                            inventarioService.generarInstruccionMaterialParaDetalle(d, v.getIdSede())
                    );
                }
            }

            v.setDetalles(detalles);
        }

        return v;
    }

    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        return ventaDao.obtenerIdUsuarioPorNombre(nombreUsuario);
    }


    // ============================================================
    // CREAR BORRADOR
    // ============================================================
    @Transactional
    public int crearBorrador(Venta venta) {
        venta.setEstadoVenta("BORRADOR");

        if (venta.getTotalVenta() == null) {
            venta.setTotalVenta(BigDecimal.ZERO);
        }

        if (venta.getDeuda() == null) {
            venta.setDeuda(BigDecimal.ZERO);
        }

        return ventaDao.registrarYObtenerId(venta);
    }

    @Transactional
    public int guardarBorradorConCliente(Venta venta) {
        String tipo = venta.getTipoComprobante() == null
                ? "NOTA"
                : venta.getTipoComprobante().trim().toUpperCase();
        boolean requiereClienteFiscal = "BOLETA".equals(tipo) || "FACTURA".equals(tipo);
        boolean tieneDocumento = venta.getDocumentoCliente() != null && !venta.getDocumentoCliente().isBlank();
        boolean tieneIdCliente = venta.getIdCliente() != null && venta.getIdCliente() > 0;

        if (requiereClienteFiscal || tieneDocumento || tieneIdCliente) {
            Cliente cliente = clienteService.resolverClienteParaVenta(new ClienteVentaSolicitudDto(
                    venta.getIdCliente(), venta.getDocumentoCliente(), venta.getClienteNombres(),
                    venta.getClienteApellidos(), venta.getClienteRazonSocial(), venta.getNombreCliente(),
                    venta.getTelefonoCliente(), venta.getClienteCorreo(), venta.getClienteDireccion(),
                    tipo, venta.getClienteOrigen()));
            venta.setIdCliente(cliente.getIdClientes());
            venta.setDocumentoCliente(cliente.getNro_documento());
            venta.setNombreCliente(clienteService.nombreParaVenta(cliente));
            venta.setTelefonoCliente(cliente.getTelefono());
        } else {
            venta.setIdCliente(null);
            clienteService.validarNombreVenta(venta.getNombreCliente());
        }

        if (venta.getIdVentas() != null && venta.getIdVentas() > 0) {
            actualizarBorrador(venta);
            return venta.getIdVentas();
        }
        return crearBorrador(venta);
    }

    @Transactional
    public void actualizarBorrador(Venta venta) {

        Venta actual = buscarPorId(venta.getIdVentas());

        if (actual == null) {
            throw new RuntimeException("La venta no existe.");
        }

        if (!"BORRADOR".equals(actual.getEstadoVenta())) {
            throw new RuntimeException("Solo se pueden editar ventas en BORRADOR.");
        }

        venta.setEstadoVenta("BORRADOR");

        if (venta.getTotalVenta() == null) {
            venta.setTotalVenta(BigDecimal.ZERO);
        }

        if (venta.getDeuda() == null) {
            venta.setDeuda(BigDecimal.ZERO);
        }

        actual.setIdCliente(venta.getIdCliente());
        actual.setNombreCliente(venta.getNombreCliente());
        actual.setDocumentoCliente(venta.getDocumentoCliente());
        actual.setTelefonoCliente(venta.getTelefonoCliente());

        String tipoComprobante = venta.getTipoComprobante();

        if (tipoComprobante == null || tipoComprobante.isBlank()) {
            tipoComprobante = "NOTA";
        }

        tipoComprobante = tipoComprobante.trim().toUpperCase();

        if ("NOTA DE VENTA".equals(tipoComprobante)) {
            tipoComprobante = "NOTA";
        }

        actual.setTipoComprobante(tipoComprobante);
        actual.setFormaPago(venta.getFormaPago());
        actual.setMetodoPago(venta.getMetodoPago());

        actual.setPagoInicial(venta.getPagoInicial());
        actual.setNumeroCuotas(venta.getNumeroCuotas());
        actual.setFrecuenciaCuotas(venta.getFrecuenciaCuotas());
        actual.setFechaPrimeraCuota(venta.getFechaPrimeraCuota());

        actual.setTotalVenta(venta.getTotalVenta());
        actual.setDeuda(venta.getDeuda());

        venta = actual;

        ventaDao.actualizarVenta(venta);
    }

    private void prepararDetalleParaStock(Venta venta, DetalleVenta d) {

        if (venta == null) {
            throw new RuntimeException("La venta no existe.");
        }

        if (d.getIdPrecioPresentacion() == null || d.getIdPrecioPresentacion() <= 0) {
            throw new RuntimeException("Seleccione una presentación de venta válida.");
        }

        ProductoPrecioPresentacion presentacion = precioPresentacionService
                .listarActivasPorProductoYSede(d.getIdProducto(), venta.getIdSede())
                .stream()
                .filter(p -> p.getIdPrecioPresentacion().equals(d.getIdPrecioPresentacion()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "La presentación seleccionada no está activa o no pertenece al producto en la sede seleccionada."
                ));

        BigDecimal factor = presentacion.getFactorConversionStock() == null
                ? BigDecimal.ONE
                : presentacion.getFactorConversionStock();

        if (factor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El factor de conversión de la presentación no es válido.");
        }

        Producto producto = productoService.obtener(d.getIdProducto());
        String tipoMedida = producto == null ? "" : producto.getTipoMedidaBase();
        boolean flujoMedidas = d.isRequierePreparacion()
                && d.getCortesJson() != null && !d.getCortesJson().isBlank()
                && normalizarGrupoMedidas(d.getGrupoMedidaUuid()) == null
                && ("AREA".equalsIgnoreCase(tipoMedida) || "LONGITUD".equalsIgnoreCase(tipoMedida));

        if (flujoMedidas) {
            BigDecimal largoCm = producto.getLargoBase() == null ? null
                    : producto.getLargoBase().multiply(new BigDecimal("100"));
            BigDecimal anchoCm = producto.getAnchoBase() == null ? null
                    : producto.getAnchoBase().multiply(new BigDecimal("100"));
            BigDecimal longitudBase = producto.getMedidaTotalBase();
            String nombrePresentacion = presentacion.getNombrePresentacion() == null
                    ? "" : presentacion.getNombrePresentacion().toLowerCase();
            String tipoCalculo = presentacion.getTipoCalculoVenta() == null
                    ? "" : presentacion.getTipoCalculoVenta().trim().toUpperCase();
            if ("MEDIA".equals(tipoCalculo) || nombrePresentacion.contains("media")) {
                if (largoCm != null) largoCm = largoCm.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
                if (longitudBase != null) longitudBase = longitudBase.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
            }

            CalculoCortesVenta.Resultado calculo = CalculoCortesVenta.calcular(
                    tipoMedida,
                    presentacion.getNombreUnidadVenta(),
                    d.getCortesJson(),
                    largoCm,
                    anchoCm,
                    longitudBase
            );
            BigDecimal cantidadCalculada = switch (tipoCalculo) {
                case "PIE2", "M2", "METRO_CUADRADO", "METRO_LINEAL", "LONGITUD", "AREA" -> calculo.medidaTotal();
                default -> BigDecimal.valueOf(calculo.unidadesFisicas());
            };
            d.setCantidad(cantidadCalculada.setScale(6, RoundingMode.HALF_UP));
        }

        BigDecimal cantidad = d.getCantidad() == null
                ? BigDecimal.ZERO
                : d.getCantidad();

        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad vendida debe ser mayor a cero.");
        }

        BigDecimal cantidadDescontada = cantidad
                .multiply(factor)
                .setScale(6, RoundingMode.HALF_UP);

        var stockOpt = inventarioService
                .listarExistencias()
                .stream()
                .filter(s ->
                        s.getIdProducto().equals(d.getIdProducto())
                                && s.getIdSede().equals(venta.getIdSede())
                )
                .findFirst();

        if (stockOpt.isEmpty()) {
            throw new RuntimeException("No existe stock en la sede seleccionada.");
        }

        BigDecimal stock = inventarioService.obtenerStockDisponibleParaVenta(d.getIdProducto(), venta.getIdSede());

        if (stock.compareTo(cantidadDescontada) < 0) {
            throw new RuntimeException(
                    "Stock insuficiente del producto ID " + d.getIdProducto()
                            + ". Disponible: " + stock
                            + ", requerido: " + cantidadDescontada
            );
        }

        BigDecimal descuento = d.getDescuento() == null
                ? BigDecimal.ZERO
                : d.getDescuento();

        BigDecimal descuentoMaximo = presentacion.getDescuentoMaximo() == null
                ? BigDecimal.ZERO
                : presentacion.getDescuentoMaximo();

        if (descuento.compareTo(descuentoMaximo) > 0) {
            throw new RuntimeException(
                    "El descuento supera el máximo permitido para la presentación."
            );
        }

        d.setIdUnidad(presentacion.getIdUnidadVenta());
        d.setNombrePresentacion(presentacion.getNombrePresentacion());
        d.setFactorConversionStock(factor);
        d.setCantidadDescontadaStock(cantidadDescontada);

        d.setPrecioUnitario(presentacion.getPrecioVenta());
    }




    // ============================================================
    // AGREGAR DETALLE
    // ============================================================
    @Transactional
    public void agregarDetalle(int idVenta, DetalleVenta d) {

        Venta venta = buscarPorId(idVenta);

        validarDetalleMedidasIndividual(d);
        prepararDetalleParaStock(venta, d);

        d.setIdVenta(idVenta);

        detalleDao.registrar(d);
    }


    @Transactional
    public void actualizarTotalesVenta(int idVenta) {

        Venta venta = buscarPorId(idVenta);

        if (venta == null) return;

        BigDecimal total = venta.getDetalles().stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        venta.setTotalVenta(total);

        // Si es crédito → recalcular deuda
        if ("CREDITO".equalsIgnoreCase(venta.getFormaPago())) {

            BigDecimal inicial = venta.getPagoInicial() == null
                    ? BigDecimal.ZERO
                    : venta.getPagoInicial();

            venta.setDeuda(total.subtract(inicial));
        }

        ventaDao.actualizarVenta(venta);
    }


    // ============================================================
    // CONFIRMAR VENTA
    // ============================================================
    @Transactional
    public void confirmarVenta(int idVenta) {

        Venta venta = buscarPorId(idVenta);

        if (venta == null)
            throw new RuntimeException("No existe la venta.");

        if (!"BORRADOR".equals(venta.getEstadoVenta()))
            throw new RuntimeException("Solo se pueden confirmar ventas en BORRADOR.");

        if (venta.getDetalles() == null || venta.getDetalles().isEmpty())
            throw new RuntimeException("La venta no tiene detalles registrados.");

        // Fecha real de la venta
        venta.setFechaVenta(LocalDateTime.now());

        // Calcular total
        BigDecimal total = venta.getDetalles().stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        venta.setTotalVenta(total);

        // Tipo de comprobante obligatorio y normalizado
        if (venta.getTipoComprobante() == null || venta.getTipoComprobante().isBlank()) {
            venta.setTipoComprobante("NOTA");
        }

        venta.setTipoComprobante(
                venta.getTipoComprobante()
                        .trim()
                        .toUpperCase()
        );

// Compatibilidad por si existen registros antiguos o llega texto visual
        if ("NOTA DE VENTA".equals(venta.getTipoComprobante())) {
            venta.setTipoComprobante("NOTA");
        }

// Generar número
        String nro = ventaDao.obtenerSiguienteComprobante(venta.getTipoComprobante());
        venta.setNroComprobante(nro);

        // Estado final
        venta.setEstadoVenta(
                "CREDITO".equalsIgnoreCase(venta.getFormaPago())
                        ? "CREDITO"
                        : "PAGADO"
        );

        validarGruposMedidas(venta.getDetalles());
        ventaMedidasConfirmacionService.validarYPreparar(venta);

        Map<Integer, BigDecimal> requeridoPorProducto = new HashMap<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            if (esLineaSecundariaMedidas(detalle)) {
                continue;
            }
            BigDecimal requerido = detalle.getCantidadDescontadaStock();
            if (requerido == null || requerido.compareTo(BigDecimal.ZERO) <= 0) {
                BigDecimal factorDetalle = detalle.getFactorConversionStock() == null
                        ? BigDecimal.ONE : detalle.getFactorConversionStock();
                requerido = detalle.getCantidad().multiply(factorDetalle).setScale(6, RoundingMode.HALF_UP);
            }
            requeridoPorProducto.merge(detalle.getIdProducto(), requerido, BigDecimal::add);
        }
        inventarioService.bloquearYValidarStockVenta(venta.getIdSede(), requeridoPorProducto);

        for (DetalleVenta d : venta.getDetalles()) {

            if (d.getIdPrecioPresentacion() == null || d.getIdPrecioPresentacion() <= 0) {
                throw new RuntimeException("El detalle de venta no tiene una presentación válida.");
            }

            ProductoPrecioPresentacion presentacion = precioPresentacionService
                    .listarActivasPorProductoYSede(d.getIdProducto(), venta.getIdSede())
                    .stream()
                    .filter(p -> p.getIdPrecioPresentacion().equals(d.getIdPrecioPresentacion()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "La presentación de venta ya no está activa o no pertenece a la sede actual."
                    ));

            String tipoCalculo = presentacion.getTipoCalculoVenta() == null
                    ? "UNIDAD"
                    : presentacion.getTipoCalculoVenta().trim().toUpperCase();

            BigDecimal factor = presentacion.getFactorConversionStock() == null
                    ? BigDecimal.ONE
                    : presentacion.getFactorConversionStock();

            if (factor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El factor de conversión de la presentación no es válido.");
            }

            BigDecimal cantidadVendida = d.getCantidad() == null
                    ? BigDecimal.ZERO
                    : d.getCantidad();

            if (cantidadVendida.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("La cantidad vendida debe ser mayor a cero.");
            }

            BigDecimal cantidadStock = d.getCantidadDescontadaStock();

            if (cantidadStock == null || cantidadStock.compareTo(BigDecimal.ZERO) <= 0) {
                cantidadStock = cantidadVendida
                        .multiply(factor)
                        .setScale(6, RoundingMode.HALF_UP);
            }

            boolean tienePlanoValidado =
                    d.getPlanoCorteJson() != null
                            && !d.getPlanoCorteJson().isBlank();

            /*
             * Todo plano validado, ya sea de “Agregar por medidas” o de una
             * venta normal automática, contiene las partes físicas exactas.
             * Se consume aquí antes de aplicar cualquier descuento genérico.
             */
            if (tienePlanoValidado) {
                inventarioService.consumirPlanoCorteValidado(
                        d,
                        venta.getIdSede(),
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );
                continue;
            }

            String grupoMedidas = normalizarGrupoMedidas(d.getGrupoMedidaUuid());
            if (grupoMedidas != null) {

                /*
                 * Las líneas secundarias pertenecen al mismo cálculo comercial,
                 * pero no deben volver a consumir inventario.
                 */
                if (esLineaSecundariaMedidas(d)) {
                    continue;
                }

                /*
                 * Compatibilidad con grupos antiguos que todavía no tengan
                 * planoCorteJson. El flujo nuevo siempre debería llegar con
                 * plano validado.
                 */
                boolean tienePartesGrupo =
                        inventarioService.tienePartesDisponibles(
                                d.getIdProducto(),
                                venta.getIdSede()
                        );

                if (tienePartesGrupo) {
                    descontarLineaGrupoMedidasPorPartes(
                            venta,
                            d,
                            tipoCalculo,
                            cantidadVendida,
                            cantidadStock
                    );
                } else {
                    inventarioService.descontarStock(
                            d.getIdProducto(),
                            venta.getIdSede(),
                            cantidadStock,
                            venta.getNroComprobante(),
                            venta.getIdUsuario()
                    );
                }

                continue;
            }

            if (esLineaSecundariaMedidas(d)) {
                continue;
            }

            boolean tienePartes = inventarioService.tienePartesDisponibles(
                    d.getIdProducto(),
                    venta.getIdSede()
            );

            if (("PIE2".equals(tipoCalculo) || "METRO_LINEAL".equals(tipoCalculo)) && tienePartes) {

                inventarioService.descontarMedidaPorPartes(
                        d.getIdProducto(),
                        venta.getIdSede(),
                        cantidadVendida,
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );

            } else if ("MEDIA".equals(tipoCalculo) && tienePartes) {

                inventarioService.descontarMediaPartePorPartes(
                        d.getIdProducto(),
                        venta.getIdSede(),
                        cantidadVendida,
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );

            } else if ("UNIDAD".equals(tipoCalculo) && tienePartes) {

                BigDecimal cantidadSinDecimales = cantidadStock.stripTrailingZeros();

                if (cantidadSinDecimales.scale() > 0) {
                    throw new RuntimeException(
                            "La venta de parte completa debe descontar una cantidad entera. Factor: " + factor
                    );
                }

                inventarioService.descontarParteCompletaPorPartes(
                        d.getIdProducto(),
                        venta.getIdSede(),
                        cantidadStock,
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );

            } else {

                /*
                 * Productos normales:
                 * - Silicona por unidad
                 * - Aluminio por metro lineal
                 * - Aluminio por media varilla
                 * - Accesorios
                 *
                 * Aquí se descuenta usando cantidadStock ya calculada:
                 * Ejemplo metro lineal:
                 * cantidadVendida = 1
                 * factor = 0.166667
                 * cantidadStock = 0.166667
                 */
                inventarioService.descontarStock(
                        d.getIdProducto(),
                        venta.getIdSede(),
                        cantidadStock,
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );
            }
        }

        // Guardar cambios
        ventaDao.actualizarVenta(venta);
    }

    private void descontarLineaGrupoMedidasPorPartes(Venta venta,
                                                     DetalleVenta detalle,
                                                     String tipoCalculo,
                                                     BigDecimal cantidadVendida,
                                                     BigDecimal cantidadStock) {
        switch (tipoCalculo) {
            case "PIE2", "METRO_LINEAL", "LONGITUD" ->
                    inventarioService.descontarMedidaPorPartes(
                            detalle.getIdProducto(), venta.getIdSede(), cantidadVendida,
                            venta.getNroComprobante(), venta.getIdUsuario());
            case "MEDIA" -> inventarioService.descontarMediaPartePorPartes(
                    detalle.getIdProducto(), venta.getIdSede(), cantidadVendida,
                    venta.getNroComprobante(), venta.getIdUsuario());
            case "UNIDAD", "COMPLETA" -> {
                BigDecimal cantidadEntera = cantidadVendida.stripTrailingZeros();
                if (cantidadEntera.scale() > 0) {
                    throw new RuntimeException(
                            "La presentación física completa del grupo debe tener cantidad entera.");
                }
                inventarioService.descontarParteCompletaPorPartes(
                        detalle.getIdProducto(), venta.getIdSede(), cantidadVendida,
                        venta.getNroComprobante(), venta.getIdUsuario());
            }
            default -> throw new RuntimeException(
                    "La presentación " + tipoCalculo
                            + " no puede descontarse de un inventario por partes sin una conversión confiable."
                            + " Consumo calculado: " + cantidadStock.stripTrailingZeros().toPlainString() + ".");
        }
    }


    void validarGruposMedidas(List<DetalleVenta> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }

        Map<String, List<DetalleVenta>> grupos = new HashMap<>();
        for (DetalleVenta detalle : detalles) {
            validarDetalleMedidasIndividual(detalle);
            String grupo = normalizarGrupoMedidas(detalle.getGrupoMedidaUuid());
            if (grupo != null) {
                grupos.computeIfAbsent(grupo, key -> new ArrayList<>()).add(detalle);
            }
        }

        for (Map.Entry<String, List<DetalleVenta>> entry : grupos.entrySet()) {
            String grupo = entry.getKey();
            List<DetalleVenta> lineas = entry.getValue();
            long principales = lineas.stream().filter(DetalleVenta::isLineaPrincipalMedida).count();
            if (principales != 1) {
                throw new RuntimeException("El grupo de medidas " + grupo
                        + " debe contener exactamente una línea principal.");
            }

            int idProducto = lineas.get(0).getIdProducto();
            boolean productoDistinto = lineas.stream().anyMatch(d -> d.getIdProducto() != idProducto);
            if (productoDistinto) {
                throw new RuntimeException("Todas las líneas del grupo de medidas " + grupo
                        + " deben pertenecer al mismo producto.");
            }

            DetalleVenta principal = lineas.stream()
                    .filter(DetalleVenta::isLineaPrincipalMedida)
                    .findFirst()
                    .orElseThrow();
            if (principal.getCortesJson() == null || principal.getCortesJson().isBlank()) {
                throw new RuntimeException("La línea principal del grupo de medidas " + grupo
                        + " debe conservar las medidas originales.");
            }

            for (DetalleVenta linea : lineas) {
                if (!linea.isLineaPrincipalMedida()) {
                    if (linea.getCortesJson() != null && !linea.getCortesJson().isBlank()) {
                        throw new RuntimeException("Las líneas secundarias del grupo " + grupo
                                + " no deben duplicar cortesJson.");
                    }
                    if (linea.getCroquisSvg() != null && !linea.getCroquisSvg().isBlank()) {
                        throw new RuntimeException("Las líneas secundarias del grupo " + grupo
                                + " no deben duplicar el croquis.");
                    }
                }
            }
        }
    }

    private void validarDetalleMedidasIndividual(DetalleVenta detalle) {
        if (detalle == null) {
            throw new RuntimeException("El detalle de venta no puede ser nulo.");
        }
        String grupo = normalizarGrupoMedidas(detalle.getGrupoMedidaUuid());
        detalle.setGrupoMedidaUuid(grupo);
        if (grupo == null && detalle.isLineaPrincipalMedida()) {
            throw new RuntimeException("Una línea principal por medidas debe pertenecer a un grupo.");
        }
        if (grupo != null && grupo.length() > 36) {
            throw new RuntimeException("El identificador del grupo de medidas no es válido.");
        }
    }

    private boolean esLineaSecundariaMedidas(DetalleVenta detalle) {
        return detalle != null
                && normalizarGrupoMedidas(detalle.getGrupoMedidaUuid()) != null
                && !detalle.isLineaPrincipalMedida();
    }

    private String normalizarGrupoMedidas(String grupo) {
        if (grupo == null) {
            return null;
        }
        String normalizado = grupo.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }


    @Transactional
    public int registrarVentaOnline(Venta venta) {

        if (venta == null) {
            throw new RuntimeException("La venta no puede ser nula.");
        }

        if (venta.getIdSede() == null || venta.getIdSede() <= 0) {
            throw new RuntimeException("La venta online no tiene sede asignada.");
        }

        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new RuntimeException("La venta no tiene productos.");
        }

        venta.setFechaVenta(LocalDateTime.now());
        venta.setCanalVenta("ONLINE");
        venta.setEstadoVenta("PAGADO");
        venta.setEstadoPago("APROBADO");
        venta.setFormaPago("ONLINE");
        venta.setTipoComprobante("BOLETA");

        venta.setNroComprobante(
                ventaDao.obtenerSiguienteComprobante("BOLETA")
        );

        String ticket = "TCK-" + System.currentTimeMillis();
        venta.setCodigoTicket(ticket);

        String codigoRecojo;

        do {
            codigoRecojo = String.valueOf(
                    100000 + new java.util.Random().nextInt(900000)
            );
        } while (ventaDao.existeCodigoRecojo(codigoRecojo));

        venta.setCodigoRecojo(codigoRecojo);

        for (DetalleVenta d : venta.getDetalles()) {

            if (d.getDescuento() == null) {
                d.setDescuento(BigDecimal.ZERO);
            }

            prepararDetalleParaStock(venta, d);
        }

        BigDecimal total = venta.getDetalles().stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        venta.setTotalVenta(total);
        venta.setDeuda(BigDecimal.ZERO);
        venta.setPagoInicial(total);

        ventaDao.registrarVentaOnline(venta);

        Integer idVenta = ventaDao.obtenerUltimoId();

        for (DetalleVenta d : venta.getDetalles()) {

            ProductoPrecioPresentacion presentacion = precioPresentacionService
                    .listarActivasPorProductoYSede(d.getIdProducto(), venta.getIdSede())
                    .stream()
                    .filter(p -> p.getIdPrecioPresentacion().equals(d.getIdPrecioPresentacion()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "La presentación seleccionada ya no está activa."
                    ));

            String tipoCalculo = presentacion.getTipoCalculoVenta() == null
                    ? "UNIDAD"
                    : presentacion.getTipoCalculoVenta().trim().toUpperCase();

            BigDecimal cantidadVendida = d.getCantidad() == null
                    ? BigDecimal.ZERO
                    : d.getCantidad();

            BigDecimal cantidadStock = d.getCantidadDescontadaStock();

            if (cantidadStock == null || cantidadStock.compareTo(BigDecimal.ZERO) <= 0) {
                cantidadStock = cantidadVendida.multiply(
                        presentacion.getFactorConversionStock() == null
                                ? BigDecimal.ONE
                                : presentacion.getFactorConversionStock()
                ).setScale(6, RoundingMode.HALF_UP);
            }

            d.setIdVenta(idVenta);

            detalleDao.registrar(d);

            boolean tienePartes = inventarioService.tienePartesDisponibles(
                    d.getIdProducto(),
                    venta.getIdSede()
            );

            if (("PIE2".equals(tipoCalculo) || "METRO_LINEAL".equals(tipoCalculo)) && tienePartes) {

                inventarioService.descontarMedidaPorPartes(
                        d.getIdProducto(),
                        venta.getIdSede(),
                        cantidadVendida,
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );

            } else if ("MEDIA".equals(tipoCalculo) && tienePartes) {

                inventarioService.descontarMediaPartePorPartes(
                        d.getIdProducto(),
                        venta.getIdSede(),
                        cantidadVendida,
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );

            } else if ("UNIDAD".equals(tipoCalculo) && tienePartes) {

                BigDecimal cantidadSinDecimales = cantidadStock.stripTrailingZeros();

                if (cantidadSinDecimales.scale() > 0) {
                    throw new RuntimeException(
                            "La venta online de parte completa debe descontar una cantidad entera."
                    );
                }

                inventarioService.descontarParteCompletaPorPartes(
                        d.getIdProducto(),
                        venta.getIdSede(),
                        cantidadStock,
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );

            } else {

                inventarioService.descontarStock(
                        d.getIdProducto(),
                        venta.getIdSede(),
                        cantidadStock,
                        venta.getNroComprobante(),
                        venta.getIdUsuario()
                );
            }
        }

        return idVenta;
    }

    // ============================================================
    // ANULAR VENTA
    // ============================================================
    public void prepararPermisoAnulacion(Venta venta) {
        if (venta == null) return;
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            venta.setDetalles(detalleDao.listarPorVenta(venta.getIdVentas()));
        }
        String motivo = motivoNoAnulacion(venta);
        venta.setPuedeAnular(motivo == null);
        venta.setMotivoNoAnulacion(motivo);
    }

    private String motivoNoAnulacion(Venta venta) {
        if (venta == null) return "Venta no encontrada.";
        if ("ANULADA".equalsIgnoreCase(venta.getEstadoVenta())) {
            return "La venta ya está anulada.";
        }
        if ("BORRADOR".equalsIgnoreCase(venta.getEstadoVenta())) {
            return "Las proformas se eliminan, no se anulan.";
        }
        if (!"PAGADO".equalsIgnoreCase(venta.getEstadoVenta())) {
            return "Solo se pueden anular ventas pagadas. Las ventas a crédito requieren un proceso de reversión de cuotas.";
        }

        String estadoPedido = venta.getNombreEstadoPedido() == null
                ? "" : venta.getNombreEstadoPedido().trim().toUpperCase();

        // Despacho: solamente antes de que el trabajador acepte el pedido.
        if ("PENDIENTE".equals(estadoPedido)) {
            return null;
        }
        if ("PREPARANDO".equals(estadoPedido)
                || "LISTO_RECOJO".equals(estadoPedido)) {
            return "El pedido ya fue aceptado o preparado por despacho.";
        }

        // Entrega inmediata: solo productos completos vendidos por unidad.
        boolean inmediata =
                "ENTREGADO".equals(estadoPedido)
                        && (
                        "INMEDIATA".equalsIgnoreCase(venta.getTipoEntregaHistorial())
                                || venta.getFechaPreparando() == null
                );
        if (!inmediata) {
            return "La entrega ya no se encuentra pendiente y no es una devolución inmediata permitida.";
        }

        List<DetalleVenta> detalles = venta.getDetalles() == null
                ? List.of() : venta.getDetalles();
        if (detalles.isEmpty()) {
            return "La venta no tiene detalles para validar la devolución.";
        }

        for (DetalleVenta detalle : detalles) {
            if (esLineaSecundariaMedidas(detalle)) continue;
            if (normalizarGrupoMedidas(detalle.getGrupoMedidaUuid()) != null) {
                return "La venta contiene productos ingresados por medidas o cortes.";
            }
            ProductoPrecioPresentacion presentacion = precioPresentacionService
                    .listarActivasPorProductoYSede(detalle.getIdProducto(), venta.getIdSede())
                    .stream()
                    .filter(p -> detalle.getIdPrecioPresentacion() != null
                            && detalle.getIdPrecioPresentacion().equals(p.getIdPrecioPresentacion()))
                    .findFirst()
                    .orElse(null);
            if (presentacion == null) {
                return "No se pudo validar la presentación original de un producto.";
            }
            String tipo = presentacion.getTipoCalculoVenta() == null
                    ? "UNIDAD" : presentacion.getTipoCalculoVenta().trim().toUpperCase();
            if (!("UNIDAD".equals(tipo) || "COMPLETA".equals(tipo))) {
                return "La entrega inmediata contiene una presentación vendida por medida.";
            }
            BigDecimal cantidad = detalle.getCantidad() == null
                    ? BigDecimal.ZERO : detalle.getCantidad().stripTrailingZeros();
            if (cantidad.signum() <= 0 || cantidad.scale() > 0) {
                return "La entrega inmediata contiene una cantidad fraccionada.";
            }
        }
        return null;
    }

    @Transactional
    public void anularVenta(int idVenta) {
        Venta venta = buscarPorId(idVenta);
        if (venta == null) {
            throw new RuntimeException("Venta no encontrada.");
        }

        String motivo = motivoNoAnulacion(venta);
        if (motivo != null) {
            throw new RuntimeException(motivo);
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            if (esLineaSecundariaMedidas(detalle)) continue;
            inventarioService.revertirDetalleVentaPorAnulacion(
                    detalle,
                    venta.getIdSede(),
                    venta.getNroComprobante(),
                    venta.getIdUsuario()
            );
        }

        ventaDao.actualizarEstado(idVenta, "ANULADA");
        ventaDao.anularPedidoPorVenta(idVenta);
    }


    // ============================================================
    // ELIMINAR BORRADOR
    // ============================================================
    @Transactional
    public void eliminarBorrador(int idVenta) {

        Venta venta = buscarPorId(idVenta);

        if (!"BORRADOR".equals(venta.getEstadoVenta()))
            throw new RuntimeException("Solo se pueden eliminar ventas BORRADOR.");

        detalleDao.eliminarPorVenta(idVenta);
        ventaDao.eliminarVenta(idVenta);
    }


    // ============================================================
    // REGISTRAR CRÉDITO
    // ============================================================
    @Transactional
    public void registrarCredito(int idVenta, BigDecimal pagoInicial, BigDecimal deuda) {

        Venta venta = buscarPorId(idVenta);

        venta.setFormaPago("CREDITO");
        venta.setPagoInicial(pagoInicial);
        venta.setDeuda(deuda);
        venta.setEstadoVenta("CREDITO");

        ventaDao.actualizarVenta(venta);
    }


    public String obtenerSiguienteComprobante(String tipoComprobante) {
        return ventaDao.obtenerSiguienteComprobante(tipoComprobante);
    }

    public List<Venta> filtrar(
            String canal,
            String estado,
            String estadoEntrega,
            String buscar,
            String fechaDesde,
            String fechaHasta){
        return ventaDao.filtrar(
                canal,
                estado,
                estadoEntrega,
                buscar,
                fechaDesde,
                fechaHasta
        );
    }

    public List<Venta> filtrarPorSede(
            Integer idSede,
            String canal,
            String estado,
            String estadoEntrega,
            String buscar,
            String fechaDesde,
            String fechaHasta) {

        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        return ventaDao.filtrarPorSede(
                idSede,
                canal,
                estado,
                estadoEntrega,
                buscar,
                fechaDesde,
                fechaHasta
        );
    }

    public void registrarEntregaInmediata(int idVenta) {
        ventaDao.registrarEntregaInmediata(idVenta);
    }

    public void registrarEntregaProgramada(int idVenta, LocalDateTime fechaRecojoProgramada) {
        ventaDao.registrarEntregaProgramada(idVenta, fechaRecojoProgramada);
    }

    // ============================================================
// ENTREGAS
// ============================================================

    public List<Venta> listarPendientesEntrega() {
        return ventaDao.listarPendientesEntrega();
    }

    public Venta obtenerPedidoActualPorUsuario(Integer idUsuario) {
        Venta pedido = ventaDao.obtenerPedidoActualPorUsuario(idUsuario);

        if (pedido == null || pedido.getIdVentas() == null) {
            return pedido;
        }

        Venta ventaCompleta = buscarPorId(pedido.getIdVentas());

        if (ventaCompleta == null) {
            return pedido;
        }

        pedido.setDetalles(ventaCompleta.getDetalles());

        return pedido;
    }

    public List<Venta> listarPedidosListos() {
        return ventaDao.listarPedidosListos();
    }

    public List<Venta> listarPendientesEntregaPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        return ventaDao.listarPendientesEntregaPorSede(idSede);
    }

    public Venta obtenerPedidoActualPorUsuarioYSede(Integer idUsuario, Integer idSede) {
        if (idUsuario == null || idSede == null || idSede <= 0) {
            return null;
        }

        Venta pedido = ventaDao.obtenerPedidoActualPorUsuarioYSede(idUsuario, idSede);

        if (pedido == null || pedido.getIdVentas() == null) {
            return pedido;
        }

        Venta ventaCompleta = buscarPorId(pedido.getIdVentas());

        if (ventaCompleta == null) {
            return pedido;
        }

        pedido.setDetalles(ventaCompleta.getDetalles());

        return pedido;
    }

    public List<Venta> listarPedidosListosPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        return ventaDao.listarPedidosListosPorSede(idSede);
    }

    public Venta buscarPorCodigoRecojo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }

        return ventaDao.buscarPorCodigoRecojo(codigo.trim());
    }

    public Venta buscarPedidoListoParaEntrega(String texto) {

        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }

        return ventaDao.buscarPedidoListoParaEntrega(texto.trim());
    }

    public Venta buscarPedidoListoParaEntregaPorSede(String texto, Integer idSede) {

        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }

        if (idSede == null || idSede <= 0) {
            return null;
        }

        return ventaDao.buscarPedidoListoParaEntregaPorSede(texto.trim(), idSede);
    }

    public Venta buscarPorCodigoRecojoYSede(String codigo, Integer idSede) {

        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }

        if (idSede == null || idSede <= 0) {
            return null;
        }

        return ventaDao.buscarPorCodigoRecojoYSede(codigo.trim(), idSede);
    }

    @Transactional
    public void aceptarPedido(Integer idPedido, Integer idUsuario) {

        Venta pedidoActual = ventaDao.obtenerPedidoActualPorUsuario(idUsuario);

        if (pedidoActual != null) {
            throw new RuntimeException("Ya tienes un pedido en preparación. Termínalo antes de aceptar otro.");
        }

        ventaDao.aceptarPedido(idPedido, idUsuario);
    }

    @Transactional
    public void aceptarPedidoPorSede(Integer idPedido, Integer idUsuario, Integer idSede) {

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("No hay sede activa seleccionada.");
        }

        Venta pedidoActual = ventaDao.obtenerPedidoActualPorUsuarioYSede(idUsuario, idSede);

        if (pedidoActual != null) {
            throw new RuntimeException("Ya tienes un pedido en preparación en esta sede. Termínalo antes de aceptar otro.");
        }

        Venta pedido = ventaDao.buscarPedidoEntregaPorId(idPedido);

        if (pedido == null) {
            throw new RuntimeException("El pedido no existe.");
        }

        if (pedido.getIdSede() == null || !pedido.getIdSede().equals(idSede)) {
            throw new RuntimeException("Este pedido no pertenece a la sede activa.");
        }

        ventaDao.aceptarPedido(idPedido, idUsuario);
    }

    @Transactional
    public void marcarPedidoListo(Integer idPedido) {
        ventaDao.marcarPedidoListo(idPedido);
    }

    @Transactional
    public void marcarPedidoListoPorSede(Integer idPedido, Integer idSede) {

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("No hay sede activa seleccionada.");
        }

        Venta pedido = ventaDao.buscarPedidoEntregaPorId(idPedido);

        if (pedido == null) {
            throw new RuntimeException("El pedido no existe.");
        }

        if (pedido.getIdSede() == null || !pedido.getIdSede().equals(idSede)) {
            throw new RuntimeException("Este pedido no pertenece a la sede activa.");
        }

        if (!"PREPARANDO".equals(pedido.getNombreEstadoPedido())) {
            throw new RuntimeException("Solo se pueden marcar como listos los pedidos en preparación.");
        }

        ventaDao.marcarPedidoListo(idPedido);
    }

    @Transactional
    public void confirmarEntrega(Integer idPedido,
                                 Integer idUsuarioEntrega,
                                 String codigoRecojo) {

        Venta pedido = ventaDao.buscarPorCodigoRecojo(codigoRecojo.trim());

        if (pedido == null || !pedido.getIdPedido().equals(idPedido)) {
            throw new RuntimeException("Código de recojo incorrecto.");
        }

        if (!"LISTO_RECOJO".equals(pedido.getNombreEstadoPedido())) {
            throw new RuntimeException("El pedido no está listo para entrega.");
        }

        ventaDao.confirmarEntrega(idPedido, idUsuarioEntrega);
    }

    @Transactional
    public void confirmarEntregaPorSede(Integer idPedido,
                                        Integer idUsuarioEntrega,
                                        String codigoRecojo,
                                        Integer idSede) {

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("No hay sede activa seleccionada.");
        }

        Venta pedido = ventaDao.buscarPorCodigoRecojoYSede(codigoRecojo.trim(), idSede);

        if (pedido == null || !pedido.getIdPedido().equals(idPedido)) {
            throw new RuntimeException("Código de recojo incorrecto o no pertenece a la sede activa.");
        }

        if (!"LISTO_RECOJO".equals(pedido.getNombreEstadoPedido())) {
            throw new RuntimeException("El pedido no está listo para entrega.");
        }

        ventaDao.confirmarEntrega(idPedido, idUsuarioEntrega);
    }

    public List<Venta> listarHistorialEntregas() {
        return ventaDao.listarHistorialEntregas();
    }

    public long contarEntregadosHoy() {
        return ventaDao.contarEntregadosHoy();
    }

    public long contarPreparando() {
        return ventaDao.contarPreparando();
    }

    public long contarEntregasInmediatasHoy() {
        return ventaDao.contarEntregasInmediatasHoy();
    }

    public long contarEntregasDespachoHoy() {
        return ventaDao.contarEntregasDespachoHoy();
    }

    public long contarAnuladosHoy() {
        return ventaDao.contarAnuladosHoy();
    }

    public List<Venta> listarHistorialEntregasPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        return ventaDao.listarHistorialEntregasPorSede(idSede);
    }

    public long contarEntregadosHoyPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) return 0;
        return ventaDao.contarEntregadosHoyPorSede(idSede);
    }

    public long contarPreparandoPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) return 0;
        return ventaDao.contarPreparandoPorSede(idSede);
    }

    public long contarEntregasInmediatasHoyPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) return 0;
        return ventaDao.contarEntregasInmediatasHoyPorSede(idSede);
    }

    public long contarEntregasDespachoHoyPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) return 0;
        return ventaDao.contarEntregasDespachoHoyPorSede(idSede);
    }

    public long contarAnuladosHoyPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) return 0;
        return ventaDao.contarAnuladosHoyPorSede(idSede);
    }

    public List<Venta> filtrarHistorialEntregas(String filtro) {

        List<Venta> historial = ventaDao.listarHistorialEntregas();

        if (filtro == null || filtro.isBlank() || "TODOS".equalsIgnoreCase(filtro)) {
            return historial;
        }

        return historial.stream().filter(v -> {

            if ("ENTREGADO".equalsIgnoreCase(filtro)) {
                return "ENTREGADO".equals(v.getNombreEstadoPedido());
            }

            if ("ANULADO".equalsIgnoreCase(filtro)) {
                return "ANULADO".equals(v.getNombreEstadoPedido())
                        || "ANULADA".equals(v.getEstadoVenta());
            }

            if ("INMEDIATA".equalsIgnoreCase(filtro)) {
                return !"ANULADO".equals(v.getNombreEstadoPedido())
                        && "INMEDIATA".equals(v.getTipoEntregaHistorial());
            }

            if ("DESPACHO".equalsIgnoreCase(filtro)) {
                return !"ANULADO".equals(v.getNombreEstadoPedido())
                        && "DESPACHO".equals(v.getTipoEntregaHistorial());
            }

            return true;

        }).toList();
    }

    public List<Venta> filtrarHistorialEntregasPorSede(String filtro, Integer idSede) {

        List<Venta> historial = listarHistorialEntregasPorSede(idSede);

        if (filtro == null || filtro.isBlank() || "TODOS".equalsIgnoreCase(filtro)) {
            return historial;
        }

        return historial.stream().filter(v -> {

            if ("ENTREGADO".equalsIgnoreCase(filtro)) {
                return "ENTREGADO".equals(v.getNombreEstadoPedido());
            }

            if ("ANULADO".equalsIgnoreCase(filtro)) {
                return "ANULADO".equals(v.getNombreEstadoPedido())
                        || "ANULADA".equals(v.getEstadoVenta());
            }

            if ("INMEDIATA".equalsIgnoreCase(filtro)) {
                return !"ANULADO".equals(v.getNombreEstadoPedido())
                        && "INMEDIATA".equals(v.getTipoEntregaHistorial());
            }

            if ("DESPACHO".equalsIgnoreCase(filtro)) {
                return !"ANULADO".equals(v.getNombreEstadoPedido())
                        && "DESPACHO".equals(v.getTipoEntregaHistorial());
            }

            return true;

        }).toList();
    }

    // ============================================================
// DASHBOARD / ANÁLISIS
// ============================================================

    public Map<String, Object> obtenerResumenDashboard(Integer idSede, String fechaDesde, String fechaHasta) {
        return ventaDao.obtenerResumenDashboard(idSede, fechaDesde, fechaHasta);
    }

    public List<Map<String, Object>> listarVentasPorDiaDashboard(Integer idSede, String fechaDesde, String fechaHasta) {
        return ventaDao.listarVentasPorDiaDashboard(idSede, fechaDesde, fechaHasta);
    }

    public List<Map<String, Object>> listarVentasPorCategoriaDashboard(Integer idSede, String fechaDesde, String fechaHasta) {
        return ventaDao.listarVentasPorCategoriaDashboard(idSede, fechaDesde, fechaHasta);
    }

    public List<Map<String, Object>> listarVentasPorSedeDashboard(String fechaDesde, String fechaHasta) {
        return ventaDao.listarVentasPorSedeDashboard(fechaDesde, fechaHasta);
    }

    public List<Map<String, Object>> listarVentasPorCanalDashboard(Integer idSede, String fechaDesde, String fechaHasta) {
        return ventaDao.listarVentasPorCanalDashboard(idSede, fechaDesde, fechaHasta);
    }

    public List<Map<String, Object>> listarVentasPorFormaPagoDashboard(Integer idSede, String fechaDesde, String fechaHasta) {
        return ventaDao.listarVentasPorFormaPagoDashboard(idSede, fechaDesde, fechaHasta);
    }

    public List<Map<String, Object>> listarUltimasVentasDashboard(Integer idSede, String fechaDesde, String fechaHasta) {
        return ventaDao.listarUltimasVentasDashboard(idSede, fechaDesde, fechaHasta);
    }

    public List<Map<String, Object>> listarTopProductosDashboard(Integer idSede, String fechaDesde, String fechaHasta) {
        return ventaDao.listarTopProductosDashboard(idSede, fechaDesde, fechaHasta);
    }
}