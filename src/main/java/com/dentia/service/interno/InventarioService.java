package com.dentia.service.interno;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.dao.interfaces.IInventarioMovimientoDao;
import com.dentia.dao.interfaces.IInventarioStockDao;
import com.dentia.model.interno.inventario.InventarioMovimiento;
import com.dentia.model.interno.inventario.InventarioStock;
import com.dentia.dao.interfaces.IInventarioTransferenciaDao;
import com.dentia.model.interno.inventario.InventarioTransferencia;
import com.dentia.model.interno.inventario.InventarioTransferenciaDetalle;
import com.dentia.dao.interfaces.IInventarioAjusteDao;
import com.dentia.model.interno.inventario.InventarioAjuste;
import com.dentia.model.interno.inventario.InventarioAjusteDetalle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dentia.dao.interfaces.IInventarioParteDao;
import com.dentia.dao.interfaces.IInventarioReservaDao;
import com.dentia.dao.interfaces.IProductoDao;
import com.dentia.model.interno.Producto;
import com.dentia.model.interno.inventario.InventarioParte;
import com.dentia.model.interno.inventario.InventarioTransferenciaEvento;
import com.dentia.model.interno.DetalleVenta;
import com.dentia.dto.interno.ventas.CalculoMedidasRespuestaDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;

@Service
public class InventarioService {

    private static final ObjectMapper PLANO_OBJECT_MAPPER = new ObjectMapper();
    private static final BigDecimal METROS_CUADRADOS_A_PIE2 =
            new BigDecimal("10.763910416709722");

    private final IInventarioStockDao stockDao;
    private final IInventarioMovimientoDao movimientoDao;
    private final IInventarioTransferenciaDao transferenciaDao;
    private final IInventarioAjusteDao ajusteDao;
    private final IInventarioParteDao parteDao;
    private final IInventarioReservaDao reservaDao;
    private final IProductoDao productoDao;

    public InventarioService(IInventarioStockDao stockDao,
                             IInventarioMovimientoDao movimientoDao,
                             IInventarioTransferenciaDao transferenciaDao,
                             IInventarioAjusteDao ajusteDao,
                             IInventarioParteDao parteDao,
                             IInventarioReservaDao reservaDao,
                             IProductoDao productoDao) {
        this.stockDao = stockDao;
        this.movimientoDao = movimientoDao;
        this.transferenciaDao = transferenciaDao;
        this.ajusteDao = ajusteDao;
        this.parteDao = parteDao;
        this.reservaDao = reservaDao;
        this.productoDao = productoDao;
    }

    public List<InventarioStock> listarExistencias() {
        return stockDao.listarExistencias();
    }

    public List<InventarioStock> listarExistenciasPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        return stockDao.listarExistenciasPorSede(idSede);
    }

    public BigDecimal obtenerStockDisponibleParaVenta(Integer idProducto, Integer idSede) {
        BigDecimal stock = stockDao.buscarPorProductoYSede(idProducto, idSede)
                .map(InventarioStock::getStockActual)
                .orElse(BigDecimal.ZERO);
        BigDecimal reservado = reservaDao.sumarReservadoActivoPorProductoYSede(idProducto, idSede);
        return stock.subtract(reservado == null ? BigDecimal.ZERO : reservado).max(BigDecimal.ZERO);
    }

    /** Bloquea el stock en orden fijo y valida todo el carrito antes de efectuar una salida. */
    public void bloquearYValidarStockVenta(Integer idSede, Map<Integer, BigDecimal> requeridoPorProducto) {
        List<Integer> ids = requeridoPorProducto.keySet().stream().sorted().toList();
        Map<Integer, InventarioStock> bloqueados = new HashMap<>();
        stockDao.bloquearPorSedeYProductos(idSede, ids)
                .forEach(stock -> bloqueados.put(stock.getIdProducto(), stock));

        for (Integer idProducto : ids) {
            BigDecimal requerido = requeridoPorProducto.getOrDefault(idProducto, BigDecimal.ZERO);
            InventarioStock stock = bloqueados.get(idProducto);
            if (stock == null) {
                throw new RuntimeException("No existe stock activo para el producto ID " + idProducto + ".");
            }
            BigDecimal reservado = reservaDao.sumarReservadoActivoPorProductoYSede(idProducto, idSede);
            BigDecimal disponible = stock.getStockActual()
                    .subtract(reservado == null ? BigDecimal.ZERO : reservado)
                    .max(BigDecimal.ZERO);
            if (requerido.compareTo(disponible) > 0) {
                throw new RuntimeException("Stock insuficiente del producto ID " + idProducto
                        + ". Necesario: " + requerido + ", disponible: " + disponible + ".");
            }
        }
    }

    public boolean tienePartesDisponibles(Integer idProducto, Integer idSede) {
        if (idProducto == null || idProducto <= 0 || idSede == null || idSede <= 0) {
            return false;
        }

        return !parteDao.listarDisponiblesPorProductoYSede(idProducto, idSede).isEmpty();
    }

    public DisponibilidadFisicaProducto obtenerDisponibilidadFisicaProducto(
            Integer idProducto,
            Integer idSede
    ) {
        if (idProducto == null || idProducto <= 0) {
            throw new RuntimeException("Producto inválido.");
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("Sede inválida.");
        }

        List<InventarioParte> partes =
                parteDao.listarDisponiblesPorProductoYSede(idProducto, idSede);

        if (partes == null) {
            partes = List.of();
        }

        int completas = partes.stream()
                .filter(this::esParteDisponible)
                .filter(p -> "COMPLETA".equalsIgnoreCase(p.getTipoParte()))
                .mapToInt(p -> p.getCantidadPartes() != null
                        ? Math.max(0, p.getCantidadPartes())
                        : 1)
                .sum();

        int medias = partes.stream()
                .filter(this::esParteDisponible)
                .filter(p -> "MEDIA".equalsIgnoreCase(p.getTipoParte()))
                .mapToInt(p -> p.getCantidadPartes() != null
                        ? Math.max(0, p.getCantidadPartes())
                        : 1)
                .sum();

        int retazos = (int) partes.stream()
                .filter(this::esParteDisponible)
                .filter(p ->
                        "RETAZO".equalsIgnoreCase(p.getTipoParte())
                                || "CORTE".equalsIgnoreCase(p.getTipoParte())
                )
                .count();

        int medidaVendibleTotal = partes.stream()
                .filter(this::esParteDisponible)
                .map(InventarioParte::getMedidaDisponible)
                .filter(v -> v != null && v > 0)
                .reduce(0, Integer::sum);

        int medidaEnCompletas = partes.stream()
                .filter(this::esParteDisponible)
                .filter(p -> "COMPLETA".equalsIgnoreCase(p.getTipoParte()))
                .mapToInt(p -> p.getMedidaDisponible() != null
                        ? Math.max(0, p.getMedidaDisponible())
                        : 0)
                .sum();

        int medidaEnMedias = partes.stream()
                .filter(this::esParteDisponible)
                .filter(p -> "MEDIA".equalsIgnoreCase(p.getTipoParte()))
                .mapToInt(p -> p.getMedidaDisponible() != null
                        ? Math.max(0, p.getMedidaDisponible())
                        : 0)
                .sum();

        int medidaEnRetazos = partes.stream()
                .filter(this::esParteDisponible)
                .filter(p ->
                        "RETAZO".equalsIgnoreCase(p.getTipoParte())
                                || "CORTE".equalsIgnoreCase(p.getTipoParte())
                )
                .mapToInt(p -> p.getMedidaDisponible() != null
                        ? Math.max(0, p.getMedidaDisponible())
                        : 0)
                .sum();

        String tipoMedida = partes.stream()
                .filter(this::esParteDisponible)
                .map(InventarioParte::getTipoMedida)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);

        String unidadMedida = partes.stream()
                .filter(this::esParteDisponible)
                .map(InventarioParte::getUnidadMedida)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);

        BigDecimal stockEquivalenteDisponible =
                obtenerStockDisponibleParaVenta(idProducto, idSede);

        return new DisponibilidadFisicaProducto(
                completas,
                medias,
                retazos,
                medidaVendibleTotal,
                medidaEnCompletas,
                medidaEnMedias,
                medidaEnRetazos,
                stockEquivalenteDisponible,
                tipoMedida,
                unidadMedida,
                List.copyOf(partes)
        );
    }

    private boolean esParteDisponible(InventarioParte parte) {
        if (parte == null) {
            return false;
        }

        if (parte.getEstado() != null
                && !"DISPONIBLE".equalsIgnoreCase(parte.getEstado())) {
            return false;
        }

        boolean tieneCantidad = parte.getCantidadPartes() != null
                && parte.getCantidadPartes() > 0;

        boolean tieneMedida = parte.getMedidaDisponible() != null
                && parte.getMedidaDisponible() > 0;

        return tieneCantidad || tieneMedida;
    }

    public List<Map<String, Object>> obtenerPresentacionesAjusteDesdeInventario(Integer idProducto, Integer idSede) {

        if (idProducto == null || idProducto <= 0 || idSede == null || idSede <= 0) {
            return List.of();
        }

        var stockOpt = stockDao.buscarPorProductoYSede(idProducto, idSede);

        if (stockOpt.isEmpty()) {
            return List.of();
        }

        List<InventarioParte> partesDisponibles =
                parteDao.listarDisponiblesPorProductoYSede(idProducto, idSede);

        Map<String, Map<String, Object>> tipos = new java.util.LinkedHashMap<>();

        if (partesDisponibles == null || partesDisponibles.isEmpty()) {
            agregarPresentacionAjuste(tipos, "UNIDAD", "Unidad completa");
            return List.copyOf(tipos.values());
        }

        boolean tieneCompleta = partesDisponibles.stream()
                .anyMatch(p -> "COMPLETA".equalsIgnoreCase(p.getTipoParte())
                        && p.getCantidadPartes() != null
                        && p.getCantidadPartes() > 0);

        boolean tieneMedia = partesDisponibles.stream()
                .anyMatch(p -> "MEDIA".equalsIgnoreCase(p.getTipoParte())
                        && p.getMedidaDisponible() != null
                        && p.getMedidaDisponible() > 0);

        boolean tieneMedidaDisponible = partesDisponibles.stream()
                .anyMatch(p -> p.getMedidaDisponible() != null && p.getMedidaDisponible() > 0);

        String tipoMedida = partesDisponibles.stream()
                .map(InventarioParte::getTipoMedida)
                .filter(t -> t != null && !t.trim().isEmpty())
                .findFirst()
                .orElse("AREA");

        if (tieneCompleta) {
            agregarPresentacionAjuste(tipos, "UNIDAD", "Unidad completa");
        }

        if (tieneMedia || tieneCompleta) {
            agregarPresentacionAjuste(tipos, "MEDIA", "Media unidad");
        }

        if (tieneMedidaDisponible) {
            if ("LONGITUD".equalsIgnoreCase(tipoMedida)) {
                agregarPresentacionAjuste(tipos, "METRO_LINEAL", "Metro lineal");
            } else {
                agregarPresentacionAjuste(tipos, "PIE2", "Pie²");
            }
        }

        if (tipos.isEmpty()) {
            agregarPresentacionAjuste(tipos, "UNIDAD", "Unidad completa");
        }

        return List.copyOf(tipos.values());
    }

    private void agregarPresentacionAjuste(Map<String, Map<String, Object>> tipos,
                                           String tipo,
                                           String nombre) {

        Map<String, Object> item = new HashMap<>();

        item.put("idPrecioPresentacion", null);
        item.put("nombrePresentacion", nombre);
        item.put("tipoCalculoVenta", tipo);
        item.put("cantidadIncluida", BigDecimal.ONE);
        item.put("factorConversionStock", BigDecimal.ONE);
        item.put("precioVenta", BigDecimal.ZERO);
        item.put("nombreUnidadVenta", nombre);
        item.put("abreviaturaUnidadVenta", "");

        tipos.put(tipo, item);
    }

    public List<InventarioMovimiento> listarMovimientos() {
        return movimientoDao.listarMovimientos();
    }

    public List<InventarioMovimiento> listarMovimientosPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        return movimientoDao.listarMovimientosPorSede(idSede);
    }

    public List<InventarioMovimiento> listarMovimientosKardex(
            Integer idSede,
            Integer idProducto,
            String busqueda,
            String origenMovimiento,
            String tipoMovimiento,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        return movimientoDao.listarMovimientosPorSedeConFiltros(
                idSede,
                idProducto,
                busqueda,
                origenMovimiento,
                tipoMovimiento,
                fechaInicio,
                fechaFin
        );
    }

    public int contarMovimientosKardex(
            Integer idSede,
            Integer idProducto,
            String busqueda,
            String origenMovimiento,
            String tipoMovimiento,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        if (idSede == null || idSede <= 0) {
            return 0;
        }

        return movimientoDao.contarMovimientosPorSedeConFiltros(
                idSede,
                idProducto,
                busqueda,
                origenMovimiento,
                tipoMovimiento,
                fechaInicio,
                fechaFin
        );
    }

    public BigDecimal obtenerStockTotal() {
        return stockDao.obtenerStockTotal();
    }

    public BigDecimal obtenerStockTotalPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return BigDecimal.ZERO;
        }

        return stockDao.obtenerStockTotalPorSede(idSede);
    }

    public int contarProductosConStock() {
        return stockDao.contarProductosConStock();
    }

    public int contarProductosConStockPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return 0;
        }

        return stockDao.contarProductosConStockPorSede(idSede);
    }

    public BigDecimal obtenerValorTotalInventario() {
        return stockDao.obtenerValorTotalInventario();
    }

    public BigDecimal obtenerValorTotalInventarioPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return BigDecimal.ZERO;
        }

        return stockDao.obtenerValorTotalInventarioPorSede(idSede);
    }

    public Map<String, Object> obtenerDetalleExistencia(
            Integer idProducto,
            Integer idSede
    ) {
        if (idProducto == null || idProducto <= 0) {
            throw new RuntimeException("Producto inválido.");
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("Sede inválida.");
        }

        InventarioStock stock = stockDao
                .buscarPorProductoYSede(idProducto, idSede)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró stock para este producto en la sede activa."
                ));

        List<InventarioParte> partesDisponibles =
                parteDao.listarDisponiblesPorProductoYSede(
                        idProducto,
                        idSede
                );

        if (partesDisponibles == null) {
            partesDisponibles = List.of();
        }

        BigDecimal stockActualBase =
                stock.getStockActual() != null
                        ? stock.getStockActual()
                        : BigDecimal.ZERO;

        BigDecimal costoPromedio =
                stock.getCostoPromedio() != null
                        ? stock.getCostoPromedio()
                        : BigDecimal.ZERO;

        long partesCompletas = partesDisponibles.stream()
                .filter(this::esParteDisponible)
                .filter(p ->
                        "COMPLETA".equalsIgnoreCase(
                                p.getTipoParte()
                        )
                )
                .mapToLong(p ->
                        p.getCantidadPartes() != null
                                ? Math.max(0, p.getCantidadPartes())
                                : 1
                )
                .sum();

        long mediasPartes = partesDisponibles.stream()
                .filter(this::esParteDisponible)
                .filter(p ->
                        "MEDIA".equalsIgnoreCase(
                                p.getTipoParte()
                        )
                )
                .mapToLong(p ->
                        p.getCantidadPartes() != null
                                ? Math.max(0, p.getCantidadPartes())
                                : 1
                )
                .sum();

        long retazos = partesDisponibles.stream()
                .filter(this::esParteDisponible)
                .filter(p ->
                        "RETAZO".equalsIgnoreCase(p.getTipoParte())
                                || "CORTE".equalsIgnoreCase(
                                p.getTipoParte()
                        )
                )
                .count();

        BigDecimal medidaDisponibleTotal = partesDisponibles.stream()
                .filter(this::esParteDisponible)
                .map(this::obtenerMedidaFisicaMostrable)
                .filter(v -> v != null && v.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        /*
         * Medida vendible correspondiente a una plancha o unidad completa.
         *
         * En el ejemplo:
         * una plancha completa = 76 Pie².
         */
        int medidaBaseCompleta = partesDisponibles.stream()
                .filter(this::esParteDisponible)
                .filter(p ->
                        "COMPLETA".equalsIgnoreCase(p.getTipoParte())
                                && p.getMedidaPorParte() != null
                                && p.getMedidaPorParte() > 0
                )
                .map(InventarioParte::getMedidaPorParte)
                .findFirst()
                .orElse(0);

        /*
         * Normaliza en memoria la equivalencia mostrada en la tabla.
         *
         * Ejemplo:
         * retazo de 20 Pie² / plancha de 76 Pie²
         * = 0.263158 planchas.
         */
        if (medidaBaseCompleta > 0) {
            BigDecimal base =
                    BigDecimal.valueOf(medidaBaseCompleta);

            partesDisponibles.forEach(parte -> {
                if (!esParteDisponible(parte)) {
                    return;
                }

                BigDecimal medidaDisponible = obtenerMedidaFisicaMostrable(parte);

                BigDecimal equivalenciaCorregida =
                        medidaDisponible
                                .divide(
                                        base,
                                        6,
                                        RoundingMode.HALF_UP
                                );

                parte.setEquivalenteUnidadFisica(
                        equivalenciaCorregida
                );
            });
        }

        BigDecimal equivalenteUnidadFisicaTotal;

        if (medidaBaseCompleta > 0) {
            equivalenteUnidadFisicaTotal =
                    medidaDisponibleTotal
                            .divide(
                                    BigDecimal.valueOf(
                                            medidaBaseCompleta
                                    ),
                                    6,
                                    RoundingMode.HALF_UP
                            );
        } else {
            equivalenteUnidadFisicaTotal =
                    partesDisponibles.stream()
                            .filter(this::esParteDisponible)
                            .map(
                                    InventarioParte::
                                            getEquivalenteUnidadFisica
                            )
                            .filter(v -> v != null)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            )
                            .setScale(
                                    6,
                                    RoundingMode.HALF_UP
                            );
        }

        BigDecimal costoDisponibleTotal =
                partesDisponibles.stream()
                        .filter(this::esParteDisponible)
                        .map(
                                InventarioParte::
                                        getCostoTotalDisponible
                        )
                        .filter(v -> v != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * Cuando existen partes físicas, el valor más confiable
         * es la suma de sus costos disponibles.
         */
        BigDecimal valorAprox =
                partesDisponibles.isEmpty()
                        ? stockActualBase
                        .multiply(costoPromedio)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                        : costoDisponibleTotal;

        BigDecimal stockActualMostrado =
                partesDisponibles.isEmpty()
                        ? stockActualBase
                        : equivalenteUnidadFisicaTotal;
        List<InventarioParte> ultimasPartes =
                partesDisponibles;

        List<InventarioMovimiento> ultimosMovimientos =
                movimientoDao
                        .listarUltimosMovimientosPorProductoYSede(
                                idProducto,
                                idSede,
                                5
                        );

        Map<String, Object> detalle =
                new HashMap<>();

        detalle.put("stock", stock);
        detalle.put("partes", partesDisponibles);
        detalle.put("movimientos", ultimosMovimientos);

        detalle.put(
                "mostrandoLimiteMovimientos",
                ultimosMovimientos.size() >= 5
        );

        detalle.put(
                "stockActual",
                stockActualMostrado
        );

        detalle.put(
                "costoPromedio",
                costoPromedio
        );

        detalle.put(
                "valorAprox",
                valorAprox
        );

        detalle.put(
                "partesCompletas",
                partesCompletas
        );

        detalle.put(
                "mediasPartes",
                mediasPartes
        );

        detalle.put(
                "retazos",
                retazos
        );

        detalle.put(
                "medidaDisponibleTotal",
                medidaDisponibleTotal
        );

        detalle.put(
                "equivalenteUnidadFisicaTotal",
                equivalenteUnidadFisicaTotal
        );

        detalle.put(
                "costoDisponibleTotal",
                costoDisponibleTotal
        );

        detalle.put(
                "totalPartes",
                partesDisponibles.size()
        );

        Producto producto = productoDao.buscarPorId(idProducto)
                .orElse(null);

        BigDecimal largoCompletoMm = null;
        BigDecimal anchoCompletoMm = null;

        if (producto != null
                && producto.getLargoBase() != null
                && producto.getAnchoBase() != null
                && producto.getLargoBase().signum() > 0
                && producto.getAnchoBase().signum() > 0) {

            /*
             * largoBase y anchoBase están registrados en metros.
             * Se convierten a milímetros para mostrarlos junto con los retazos.
             */
            largoCompletoMm = producto.getLargoBase()
                    .multiply(new BigDecimal("1000"));

            anchoCompletoMm = producto.getAnchoBase()
                    .multiply(new BigDecimal("1000"));
        }

        detalle.put("largoCompletoMm", largoCompletoMm);
        detalle.put("anchoCompletoMm", anchoCompletoMm);

        return detalle;
    }

    public InventarioMovimiento buscarMovimientoPorId(Integer idMovimiento) {
        if (idMovimiento == null || idMovimiento <= 0) {
            throw new RuntimeException("Movimiento inválido.");
        }

        return movimientoDao.buscarPorId(idMovimiento)
                .orElseThrow(() -> new RuntimeException("No se encontró el movimiento."));
    }

    public int contarMovimientos() {
        return movimientoDao.contarMovimientos();
    }

    public int contarMovimientosPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return 0;
        }

        return movimientoDao.contarMovimientosPorSede(idSede);
    }
    public void aumentarStock(Integer idProducto,
                              Integer idSede,
                              BigDecimal cantidad,
                              String documentoReferencia,
                              Integer idUsuario,
                              BigDecimal costoUnitario) {

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad de ingreso debe ser mayor a 0.");
        }

        if (costoUnitario == null || costoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            costoUnitario = BigDecimal.ZERO;
        }

        var stockOpt = stockDao.buscarPorProductoYSede(idProducto, idSede);

        BigDecimal nuevoStock;
        BigDecimal nuevoCostoPromedio;

        if (stockOpt.isPresent()) {
            InventarioStock stock = stockOpt.get();

            BigDecimal stockActual = stock.getStockActual() != null
                    ? stock.getStockActual()
                    : BigDecimal.ZERO;

            BigDecimal costoActual = stock.getCostoPromedio() != null
                    ? stock.getCostoPromedio()
                    : BigDecimal.ZERO;

            nuevoStock = stockActual.add(cantidad);

            if (stockActual.compareTo(BigDecimal.ZERO) <= 0) {
                nuevoCostoPromedio = costoUnitario;
            } else {
                nuevoCostoPromedio = stockActual.multiply(costoActual)
                        .add(cantidad.multiply(costoUnitario))
                        .divide(nuevoStock, 6, RoundingMode.HALF_UP);
            }

            stockDao.actualizarStockYCostoPromedio(
                    idProducto,
                    idSede,
                    nuevoStock,
                    nuevoCostoPromedio
            );

        } else {
            nuevoStock = cantidad;
            nuevoCostoPromedio = costoUnitario;

            stockDao.crearStockInicial(
                    idProducto,
                    idSede,
                    cantidad,
                    nuevoCostoPromedio
            );
        }

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("ENTRADA");
        mov.setOrigenMovimiento("COMPRA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(cantidad);
        mov.setSalida(BigDecimal.ZERO);
        mov.setStockResultante(nuevoStock);
        mov.setCostoUnitario(costoUnitario);
        mov.setObservacion("Ingreso automático por compra");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    public void descontarStock(Integer idProducto,
                               Integer idSede,
                               BigDecimal cantidad,
                               String documentoReferencia,
                               Integer idUsuario) {

        var stockOpt = stockDao.buscarPorProductoYSede(idProducto, idSede);

        if (stockOpt.isEmpty()) {
            throw new RuntimeException("No existe stock para este producto en la sede activa.");
        }

        InventarioStock stock = stockOpt.get();

        if (stock.getStockActual().compareTo(cantidad) < 0) {
            throw new RuntimeException("Stock insuficiente para el producto.");
        }
        BigDecimal nuevoStock = stock.getStockActual().subtract(cantidad);

        stockDao.actualizarStock(idProducto, idSede, nuevoStock);

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("SALIDA");
        mov.setOrigenMovimiento("VENTA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(BigDecimal.ZERO);
        mov.setSalida(cantidad);
        mov.setStockResultante(nuevoStock);
        mov.setCostoUnitario(null);
        mov.setObservacion("Salida automática por venta");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    public List<InventarioTransferencia> listarTransferencias() {
        List<InventarioTransferencia> transferencias = transferenciaDao.listarTransferencias();

        for (InventarioTransferencia t : transferencias) {
            t.setDetalles(transferenciaDao.listarDetalles(t.getIdTransferencia()));
        }

        return transferencias;
    }

    public List<InventarioTransferencia> listarTransferenciasPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        List<InventarioTransferencia> transferencias = transferenciaDao.listarTransferenciasPorSede(idSede);

        for (InventarioTransferencia t : transferencias) {
            t.setDetalles(transferenciaDao.listarDetalles(t.getIdTransferencia()));
        }

        return transferencias;
    }

    public String generarCodigoTransferencia() {
        return transferenciaDao.generarCodigo();
    }
    public InventarioTransferencia buscarTransferenciaPorId(Integer idTransferencia) {
        if (idTransferencia == null || idTransferencia <= 0) {
            throw new RuntimeException("Transferencia inválida.");
        }

        InventarioTransferencia transferencia = transferenciaDao.buscarPorId(idTransferencia)
                .orElseThrow(() -> new RuntimeException("No se encontró la transferencia."));

        transferencia.setDetalles(transferenciaDao.listarDetalles(idTransferencia));

        return transferencia;
    }

    public List<InventarioTransferencia> listarTrasladosPendientes() {
        List<InventarioTransferencia> traslados = transferenciaDao.listarTrasladosPendientes();

        for (InventarioTransferencia t : traslados) {
            t.setDetalles(transferenciaDao.listarDetalles(t.getIdTransferencia()));
        }

        return traslados;
    }

    public List<InventarioTransferencia> listarTrasladosEnProceso() {
        List<InventarioTransferencia> traslados = transferenciaDao.listarTrasladosEnProceso();

        for (InventarioTransferencia t : traslados) {
            t.setDetalles(transferenciaDao.listarDetalles(t.getIdTransferencia()));
        }

        return traslados;
    }

    public List<InventarioTransferencia> listarTrasladosPorRecepcion() {
        List<InventarioTransferencia> traslados = transferenciaDao.listarTrasladosPorRecepcion();

        for (InventarioTransferencia t : traslados) {
            t.setDetalles(transferenciaDao.listarDetalles(t.getIdTransferencia()));
        }

        return traslados;
    }

    public List<InventarioTransferencia> listarHistorialTraslados() {
        List<InventarioTransferencia> traslados = transferenciaDao.listarHistorialTraslados();

        for (InventarioTransferencia t : traslados) {
            t.setDetalles(transferenciaDao.listarDetalles(t.getIdTransferencia()));
        }

        return traslados;
    }

    public List<InventarioTransferenciaEvento> listarEventosTransferencia(Integer idTransferencia) {
        if (idTransferencia == null || idTransferencia <= 0) {
            throw new RuntimeException("Transferencia inválida.");
        }

        return transferenciaDao.listarEventos(idTransferencia);
    }

    public Map<String, Object> buscarNotificacionCambioTraslado(List<Integer> idsTransferencias) {

        Map<String, Object> respuesta = new HashMap<>();

        respuesta.put("hayCambios", false);

        if (idsTransferencias == null || idsTransferencias.isEmpty()) {
            return respuesta;
        }

        InventarioTransferenciaEvento ultimoEventoDetectado = null;
        InventarioTransferencia transferenciaDetectada = null;
        String tipoDetectado = null;

        for (Integer idTransferencia : idsTransferencias) {

            if (idTransferencia == null || idTransferencia <= 0) {
                continue;
            }

            InventarioTransferencia transferencia;

            try {
                transferencia = buscarTransferenciaPorId(idTransferencia);
            } catch (RuntimeException e) {
                continue;
            }

            List<InventarioTransferenciaEvento> eventos =
                    transferenciaDao.listarEventos(idTransferencia);

            for (InventarioTransferenciaEvento evento : eventos) {

                String estadoNuevo = evento.getEstadoNuevo() != null
                        ? evento.getEstadoNuevo()
                        : "";

                String observacion = evento.getObservacion() != null
                        ? evento.getObservacion()
                        : "";

                boolean esCancelacion = "CANCELADA".equalsIgnoreCase(estadoNuevo);

                boolean esEdicionAdmin = observacion.toLowerCase()
                        .contains("administrador editó los productos");

                if (!esCancelacion && !esEdicionAdmin) {
                    continue;
                }

                if (ultimoEventoDetectado == null
                        || evento.getIdEvento() > ultimoEventoDetectado.getIdEvento()) {

                    ultimoEventoDetectado = evento;
                    transferenciaDetectada = transferencia;
                    tipoDetectado = esCancelacion ? "CANCELADA" : "EDITADA";
                }
            }
        }

        if (ultimoEventoDetectado == null || transferenciaDetectada == null) {
            return respuesta;
        }

        respuesta.put("hayCambios", true);
        respuesta.put("idEvento", ultimoEventoDetectado.getIdEvento());
        respuesta.put("idTransferencia", transferenciaDetectada.getIdTransferencia());
        respuesta.put("codigo", transferenciaDetectada.getCodigo());
        respuesta.put("tipo", tipoDetectado);

        if ("CANCELADA".equals(tipoDetectado)) {
            respuesta.put(
                    "mensaje",
                    "El administrador anuló este pedido. Ya no debes continuar con este traslado."
            );
        } else {
            respuesta.put(
                    "mensaje",
                    "El administrador editó este pedido. Verifica nuevamente los productos antes de continuar."
            );
        }

        return respuesta;
    }

    @Transactional
    public void aceptarTrasladoInterno(Integer idTransferencia, Integer idUsuario) {

        if (idUsuario == null || idUsuario <= 0) {
            throw new RuntimeException("No se pudo identificar al transportista actual.");
        }

        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        if (!"CREADA".equalsIgnoreCase(transferencia.getEstado())) {
            throw new RuntimeException("Solo se pueden aceptar traslados en estado CREADA.");
        }

        validarTransportistaSinTrasladoActivo(idUsuario);

        transferenciaDao.aceptarTraslado(idTransferencia, idUsuario);

        registrarEventoTransferencia(
                idTransferencia,
                transferencia.getEstado(),
                "ACEPTADA",
                "El transportista aceptó el traslado.",
                idUsuario
        );
    }

    private void validarTransportistaSinTrasladoActivo(Integer idUsuario) {

        boolean tieneEnProceso = listarTrasladosEnProceso()
                .stream()
                .anyMatch(t -> idUsuario.equals(t.getIdTransportista()));

        boolean tienePorRecepcion = listarTrasladosPorRecepcion()
                .stream()
                .anyMatch(t -> idUsuario.equals(t.getIdTransportista()));

        if (tieneEnProceso || tienePorRecepcion) {
            throw new RuntimeException(
                    "Ya tienes un traslado activo. Debes terminarlo o esperar la recepción antes de aceptar otro."
            );
        }
    }

    public boolean transportistaTieneTrasladoActivo(Integer idUsuario) {

        if (idUsuario == null || idUsuario <= 0) {
            return false;
        }

        boolean tieneEnProceso = listarTrasladosEnProceso()
                .stream()
                .anyMatch(t -> idUsuario.equals(t.getIdTransportista()));

        boolean tienePorRecepcion = listarTrasladosPorRecepcion()
                .stream()
                .anyMatch(t -> idUsuario.equals(t.getIdTransportista()));

        return tieneEnProceso || tienePorRecepcion;
    }

    @Transactional
    public void marcarLlegadaOrigenTraslado(Integer idTransferencia,
                                            Integer idUsuario,
                                            String fotoLlegadaOrigen) {

        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        if (!"ACEPTADA".equalsIgnoreCase(transferencia.getEstado())) {
            throw new RuntimeException("Primero debes aceptar el traslado.");
        }

        if (fotoLlegadaOrigen == null || fotoLlegadaOrigen.trim().isEmpty()) {
            throw new RuntimeException("Debe subir una foto de llegada al origen.");
        }

        transferenciaDao.marcarLlegadaOrigen(idTransferencia, fotoLlegadaOrigen);

        registrarEventoTransferencia(
                idTransferencia,
                transferencia.getEstado(),
                "EN_RECOJO",
                "El transportista llegó a la sede de origen con evidencia fotográfica.",
                idUsuario
        );
    }

    @Transactional
    public void marcarPedidoListoTraslado(Integer idTransferencia,
                                          Integer idUsuario,
                                          String fotoCarga) {

        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        if (!"EN_RECOJO".equalsIgnoreCase(transferencia.getEstado())) {
            throw new RuntimeException("Primero se debe marcar la llegada a origen.");
        }

        if (fotoCarga == null || fotoCarga.trim().isEmpty()) {
            throw new RuntimeException("Debe subir una foto del pedido listo o carga preparada.");
        }

        transferenciaDao.marcarPedidoListo(idTransferencia, fotoCarga);

        registrarEventoTransferencia(
                idTransferencia,
                transferencia.getEstado(),
                "LISTO_PARA_SALIR",
                "El pedido fue preparado y quedó listo para salir con evidencia fotográfica.",
                idUsuario
        );
    }

    @Transactional
    public void iniciarRutaTraslado(Integer idTransferencia, Integer idUsuario) {
        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        if (!"LISTO_PARA_SALIR".equalsIgnoreCase(transferencia.getEstado())) {
            throw new RuntimeException("Primero el pedido debe estar listo para salir.");
        }

        transferenciaDao.iniciarRuta(idTransferencia);

        registrarEventoTransferencia(
                idTransferencia,
                transferencia.getEstado(),
                "EN_RUTA",
                "El traslado inició ruta hacia la sede destino.",
                idUsuario
        );
    }

    @Transactional
    public void marcarLlegadaDestinoTraslado(Integer idTransferencia,
                                             Integer idUsuario,
                                             String fotoLlegada) {

        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        if (!"EN_RUTA".equalsIgnoreCase(transferencia.getEstado())) {
            throw new RuntimeException("Primero se debe iniciar la ruta.");
        }

        if (fotoLlegada == null || fotoLlegada.trim().isEmpty()) {
            throw new RuntimeException("Debe subir una foto de llegada al destino.");
        }

        transferenciaDao.marcarLlegadaDestino(idTransferencia, fotoLlegada);

        registrarEventoTransferencia(
                idTransferencia,
                transferencia.getEstado(),
                "PENDIENTE_RECEPCION",
                "El traslado llegó a destino con evidencia fotográfica y quedó pendiente de recepción.",
                idUsuario
        );
    }

    @Transactional
    public void reportarIncidenciaTraslado(Integer idTransferencia,
                                           Integer idUsuario,
                                           String motivo,
                                           String descripcion,
                                           String fotoIncidencia) {

        if (idUsuario == null || idUsuario <= 0) {
            throw new RuntimeException("No se pudo identificar al usuario actual.");
        }

        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        String estadoActual = transferencia.getEstado();

        boolean puedeReportar =
                "ACEPTADA".equalsIgnoreCase(estadoActual)
                        || "EN_RECOJO".equalsIgnoreCase(estadoActual)
                        || "LISTO_PARA_SALIR".equalsIgnoreCase(estadoActual)
                        || "EN_RUTA".equalsIgnoreCase(estadoActual)
                        || "PENDIENTE_RECEPCION".equalsIgnoreCase(estadoActual);

        if (!puedeReportar) {
            throw new RuntimeException("Solo se pueden reportar imprevistos en traslados activos.");
        }

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("Debe seleccionar el motivo del imprevisto.");
        }

        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new RuntimeException("Debe ingresar una explicación del imprevisto.");
        }

        if (fotoIncidencia == null || fotoIncidencia.trim().isEmpty()) {
            throw new RuntimeException("Debe subir una foto como evidencia del imprevisto.");
        }

        String observacion = "IMPREVISTO: "
                + motivo.trim()
                + ". "
                + descripcion.trim()
                + " | FOTO_EVIDENCIA="
                + fotoIncidencia.trim();

        registrarEventoTransferencia(
                idTransferencia,
                estadoActual,
                estadoActual,
                observacion,
                idUsuario
        );
    }

    @Transactional
    public void cancelarTrasladoInterno(Integer idTransferencia, String observacion, Integer idUsuario) {
        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        if ("COMPLETADA".equalsIgnoreCase(transferencia.getEstado())
                || "COMPLETADA_CON_DIFERENCIAS".equalsIgnoreCase(transferencia.getEstado())) {
            throw new RuntimeException("No se puede cancelar un traslado ya completado.");
        }

        transferenciaDao.cancelarTraslado(idTransferencia, observacion);

        registrarEventoTransferencia(
                idTransferencia,
                transferencia.getEstado(),
                "CANCELADA",
                observacion != null && !observacion.trim().isEmpty()
                        ? observacion
                        : "Traslado cancelado.",
                idUsuario
        );
    }

    @Transactional
    public void confirmarRecepcionTraslado(Integer idTransferencia,
                                           List<InventarioTransferenciaDetalle> detallesRecepcion,
                                           String observacionRecepcion,
                                           Integer idUsuario) {

        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        if (!"PENDIENTE_RECEPCION".equalsIgnoreCase(transferencia.getEstado())) {
            throw new RuntimeException("El traslado aún no está pendiente de recepción.");
        }

        if (detallesRecepcion == null || detallesRecepcion.isEmpty()) {
            throw new RuntimeException("Debe confirmar al menos un producto recibido.");
        }

        List<InventarioTransferenciaDetalle> detallesBD =
                transferenciaDao.listarDetalles(idTransferencia);

        boolean hayDiferencias = false;

        for (InventarioTransferenciaDetalle detalleBD : detallesBD) {

            InventarioTransferenciaDetalle recibido = detallesRecepcion.stream()
                    .filter(d -> d.getIdDetalleTransferencia().equals(detalleBD.getIdDetalleTransferencia()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "Falta confirmar recepción del producto: " + detalleBD.getNombreProducto()
                    ));

            BigDecimal cantidadEnviada = detalleBD.getCantidadEnviada() != null
                    ? detalleBD.getCantidadEnviada()
                    : detalleBD.getCantidad();

            BigDecimal cantidadRecibida = recibido.getCantidadRecibida() != null
                    ? recibido.getCantidadRecibida()
                    : BigDecimal.ZERO;

            if (cantidadRecibida.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("La cantidad recibida no puede ser negativa.");
            }

            if (cantidadRecibida.compareTo(cantidadEnviada) > 0) {
                throw new RuntimeException(
                        "La cantidad recibida no puede ser mayor a la enviada para: "
                                + detalleBD.getNombreProducto()
                );
            }

            BigDecimal faltante = cantidadEnviada.subtract(cantidadRecibida);

            if (faltante.compareTo(BigDecimal.ZERO) > 0) {
                hayDiferencias = true;
            }

            transferenciaDao.actualizarRecepcionDetalle(
                    detalleBD.getIdDetalleTransferencia(),
                    cantidadRecibida,
                    faltante,
                    recibido.getObservacionRecepcion()
            );

            if (cantidadRecibida.compareTo(BigDecimal.ZERO) > 0) {
                transferirStockInventario(
                        detalleBD.getIdProducto(),
                        transferencia.getIdSedeOrigen(),
                        transferencia.getIdSedeDestino(),
                        cantidadRecibida,
                        transferencia.getCodigo(),
                        idUsuario
                );
            }
        }

        String estadoFinal = "COMPLETADA";

        String observacionEvento = hayDiferencias
                ? "Recepción confirmada. Se registraron diferencias en el detalle de productos."
                : "Recepción confirmada correctamente.";

        transferenciaDao.confirmarRecepcion(
                idTransferencia,
                estadoFinal,
                observacionRecepcion
        );

        registrarEventoTransferencia(
                idTransferencia,
                transferencia.getEstado(),
                estadoFinal,
                observacionEvento,
                idUsuario
        );
    }

    @Transactional
    public void registrarTransferencia(InventarioTransferencia transferencia,
                                       List<InventarioTransferenciaDetalle> detalles) {

        if (transferencia.getIdSedeOrigen().equals(transferencia.getIdSedeDestino())) {
            throw new RuntimeException("La sede origen y destino no pueden ser iguales.");
        }

        if (detalles == null || detalles.isEmpty()) {
            throw new RuntimeException("Debe agregar al menos un producto.");
        }

        boolean requiereTransporte = Boolean.TRUE.equals(transferencia.getRequiereTransporte())
                || "CON_TRANSPORTE".equalsIgnoreCase(transferencia.getTipoTransferencia());

        transferencia.setCodigo(transferenciaDao.generarCodigo());

        if (requiereTransporte) {
            transferencia.setTipoTransferencia("CON_TRANSPORTE");
            transferencia.setRequiereTransporte(true);
            transferencia.setEstado("CREADA");
        } else {
            transferencia.setTipoTransferencia("INMEDIATA");
            transferencia.setRequiereTransporte(false);
            transferencia.setEstado("COMPLETADA");
        }

        for (InventarioTransferenciaDetalle d : detalles) {
            validarStockDisponibleParaTransferencia(
                    d.getIdProducto(),
                    transferencia.getIdSedeOrigen(),
                    d.getCantidad()
            );
        }

        int idTransferencia = transferenciaDao.registrarTransferencia(transferencia);

        String documento = transferencia.getCodigo();

        for (InventarioTransferenciaDetalle d : detalles) {

            d.setIdTransferencia(idTransferencia);
            d.setCantidadEnviada(d.getCantidad());

            transferenciaDao.registrarDetalle(d);

            if (!requiereTransporte) {
                transferirStockInventario(
                        d.getIdProducto(),
                        transferencia.getIdSedeOrigen(),
                        transferencia.getIdSedeDestino(),
                        d.getCantidad(),
                        documento,
                        transferencia.getIdUsuario()
                );
            }
        }

        registrarEventoTransferencia(
                idTransferencia,
                null,
                transferencia.getEstado(),
                requiereTransporte
                        ? "Transferencia creada con transporte. Pendiente de aceptación."
                        : "Transferencia inmediata completada.",
                transferencia.getIdUsuario()
        );
    }

    @Transactional
    public void editarProductosTransferenciaPendiente(Integer idTransferencia,
                                                      Integer idSedeActiva,
                                                      List<InventarioTransferenciaDetalle> detalles,
                                                      Integer idUsuario) {

        if (idTransferencia == null || idTransferencia <= 0) {
            throw new RuntimeException("Transferencia inválida.");
        }

        if (idSedeActiva == null || idSedeActiva <= 0) {
            throw new RuntimeException("Sede activa inválida.");
        }

        InventarioTransferencia transferenciaActual = buscarTransferenciaPorId(idTransferencia);

        if (!idSedeActiva.equals(transferenciaActual.getIdSedeOrigen())) {
            throw new RuntimeException("Solo la sede origen puede editar los productos de esta transferencia.");
        }

        String estadoActual = transferenciaActual.getEstado();

        boolean puedeEditar =
                "CREADA".equalsIgnoreCase(estadoActual)
                        || "ACEPTADA".equalsIgnoreCase(estadoActual)
                        || "EN_RECOJO".equalsIgnoreCase(estadoActual);

        if (!puedeEditar) {
            throw new RuntimeException("Esta transferencia ya no se puede editar porque el pedido ya fue preparado o está en ruta.");
        }

        if (detalles == null || detalles.isEmpty()) {
            throw new RuntimeException("Debe agregar al menos un producto.");
        }

        java.util.Set<Integer> productosUsados = new java.util.HashSet<>();

        for (InventarioTransferenciaDetalle d : detalles) {

            if (d.getIdProducto() == null || d.getIdProducto() <= 0) {
                throw new RuntimeException("Hay un producto inválido en la transferencia.");
            }

            if (productosUsados.contains(d.getIdProducto())) {
                throw new RuntimeException("No puede repetir el mismo producto en la transferencia.");
            }

            productosUsados.add(d.getIdProducto());

            if (d.getCantidad() == null || d.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("La cantidad debe ser mayor a 0.");
            }

            validarCantidadEntera(
                    d.getCantidad(),
                    "La transferencia debe ser en unidades completas."
            );

            validarStockDisponibleParaTransferencia(
                    d.getIdProducto(),
                    transferenciaActual.getIdSedeOrigen(),
                    d.getCantidad()
            );

            d.setIdTransferencia(idTransferencia);
            d.setCantidadEnviada(d.getCantidad());
        }

        transferenciaDao.eliminarDetallesPorTransferencia(idTransferencia);

        for (InventarioTransferenciaDetalle d : detalles) {
            transferenciaDao.registrarDetalle(d);
        }

        registrarEventoTransferencia(
                idTransferencia,
                estadoActual,
                estadoActual,
                "El administrador editó los productos y cantidades antes de que el pedido esté listo.",
                idUsuario
        );
    }

    @Transactional
    public void anularTransferenciaPendiente(Integer idTransferencia,
                                             Integer idSedeActiva,
                                             String observacion,
                                             Integer idUsuario) {

        if (idTransferencia == null || idTransferencia <= 0) {
            throw new RuntimeException("Transferencia inválida.");
        }

        if (idSedeActiva == null || idSedeActiva <= 0) {
            throw new RuntimeException("Sede activa inválida.");
        }

        InventarioTransferencia transferencia = buscarTransferenciaPorId(idTransferencia);

        if (!idSedeActiva.equals(transferencia.getIdSedeOrigen())) {
            throw new RuntimeException("Solo la sede origen puede anular esta transferencia.");
        }

        String estadoActual = transferencia.getEstado();

        boolean puedeAnular =
                "CREADA".equalsIgnoreCase(estadoActual)
                        || "ACEPTADA".equalsIgnoreCase(estadoActual)
                        || "EN_RECOJO".equalsIgnoreCase(estadoActual);

        if (!puedeAnular) {
            throw new RuntimeException("Esta transferencia ya no se puede anular porque el pedido ya fue preparado o está en ruta.");
        }

        String motivo = observacion != null && !observacion.trim().isEmpty()
                ? observacion.trim()
                : "Transferencia anulada por el administrador antes de preparar el pedido.";

        transferenciaDao.cancelarTraslado(idTransferencia, motivo);

        registrarEventoTransferencia(
                idTransferencia,
                estadoActual,
                "CANCELADA",
                motivo,
                idUsuario
        );
    }

    private void registrarEventoTransferencia(Integer idTransferencia,
                                              String estadoAnterior,
                                              String estadoNuevo,
                                              String observacion,
                                              Integer idUsuario) {

        InventarioTransferenciaEvento evento = new InventarioTransferenciaEvento();

        evento.setIdTransferencia(idTransferencia);
        evento.setEstadoAnterior(estadoAnterior);
        evento.setEstadoNuevo(estadoNuevo);
        evento.setObservacion(observacion);
        evento.setIdUsuario(idUsuario);

        transferenciaDao.registrarEvento(evento);
    }

    private void validarStockDisponibleParaTransferencia(Integer idProducto,
                                                         Integer idSedeOrigen,
                                                         BigDecimal cantidad) {

        if (idProducto == null || idProducto <= 0) {
            throw new RuntimeException("Producto inválido en la transferencia.");
        }

        if (idSedeOrigen == null || idSedeOrigen <= 0) {
            throw new RuntimeException("Sede origen inválida.");
        }

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad a transferir debe ser mayor a 0.");
        }

        boolean productoConPartes = esProductoConPartes(idProducto, idSedeOrigen);

        if (productoConPartes) {
            validarCantidadEntera(cantidad, "La transferencia de partes completas debe ser entera.");

            InventarioParte loteOrigen = parteDao.buscarLoteCompletoDisponible(idProducto, idSedeOrigen)
                    .orElseThrow(() -> new RuntimeException("No hay unidades completas disponibles para transferir."));

            int cantidadDisponibleOrigen = loteOrigen.getCantidadPartes() != null
                    ? loteOrigen.getCantidadPartes()
                    : 0;

            if (cantidadDisponibleOrigen < cantidad.intValueExact()) {
                throw new RuntimeException(
                        "Stock insuficiente de unidades completas. Disponible: "
                                + cantidadDisponibleOrigen
                                + ", solicitado: "
                                + cantidad.intValueExact()
                );
            }

            return;
        }

        InventarioStock stock = stockDao.buscarPorProductoYSede(idProducto, idSedeOrigen)
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto en la sede origen."));

        BigDecimal stockActual = stock.getStockActual() != null
                ? stock.getStockActual()
                : BigDecimal.ZERO;

        if (stockActual.compareTo(cantidad) < 0) {
            throw new RuntimeException(
                    "Stock insuficiente para el producto: "
                            + stock.getNombreProducto()
                            + ". Disponible: "
                            + stockActual
                            + ", solicitado: "
                            + cantidad
            );
        }
    }

    private void registrarFaltanteTraslado(Integer idProducto,
                                           Integer idSedeOrigen,
                                           BigDecimal cantidadFaltante,
                                           String documentoReferencia,
                                           Integer idUsuario) {

        if (cantidadFaltante == null || cantidadFaltante.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        boolean productoConPartes = esProductoConPartes(idProducto, idSedeOrigen);

        if (productoConPartes) {
            throw new RuntimeException(
                    "Hay diferencia en un producto con partes. Para evitar inconsistencias, registra la recepción exacta o revisamos el manejo de faltantes por piezas."
            );
        }

        descontarStockTransferencia(
                idProducto,
                idSedeOrigen,
                cantidadFaltante,
                documentoReferencia,
                idUsuario
        );
    }

    private void transferirStockInventario(Integer idProducto,
                                           Integer idSedeOrigen,
                                           Integer idSedeDestino,
                                           BigDecimal cantidad,
                                           String documentoReferencia,
                                           Integer idUsuario) {

        boolean productoConPartes = esProductoConPartes(idProducto, idSedeOrigen);

        if (productoConPartes) {
            transferirParteCompleta(
                    idProducto,
                    idSedeOrigen,
                    idSedeDestino,
                    cantidad,
                    documentoReferencia,
                    idUsuario
            );
            return;
        }

        descontarStockTransferencia(
                idProducto,
                idSedeOrigen,
                cantidad,
                documentoReferencia,
                idUsuario
        );

        aumentarStockTransferencia(
                idProducto,
                idSedeDestino,
                cantidad,
                documentoReferencia,
                idUsuario
        );
    }
    private void transferirParteCompleta(Integer idProducto,
                                         Integer idSedeOrigen,
                                         Integer idSedeDestino,
                                         BigDecimal cantidad,
                                         String documentoReferencia,
                                         Integer idUsuario) {

        validarCantidadEntera(cantidad, "La transferencia de unidades completas debe ser entera.");

        int cantidadEntera = cantidad.intValueExact();

        InventarioParte loteOrigen = parteDao.buscarLoteCompletoDisponible(idProducto, idSedeOrigen)
                .orElseThrow(() -> new RuntimeException("No hay unidades completas disponibles para transferir."));

        int cantidadDisponibleOrigen = loteOrigen.getCantidadPartes() != null
                ? loteOrigen.getCantidadPartes()
                : 0;

        if (cantidadDisponibleOrigen < cantidadEntera) {
            throw new RuntimeException(
                    "Stock insuficiente de unidades completas. Disponible: "
                            + cantidadDisponibleOrigen
                            + ", solicitado: "
                            + cantidadEntera
            );
        }

        int medidaPorParte = loteOrigen.getMedidaPorParte() != null && loteOrigen.getMedidaPorParte() > 0
                ? loteOrigen.getMedidaPorParte()
                : obtenerMedidaBaseDesdePartes(idProducto, idSedeOrigen);

        BigDecimal costoUnitarioParte = loteOrigen.getCostoUnitarioParte() != null
                ? loteOrigen.getCostoUnitarioParte()
                : loteOrigen.getCostoTotalDisponible()
                .divide(BigDecimal.valueOf(cantidadDisponibleOrigen), 2, RoundingMode.HALF_UP);

        BigDecimal costoPorMedida = loteOrigen.getCostoPorMedida() != null
                ? loteOrigen.getCostoPorMedida()
                : costoUnitarioParte.divide(BigDecimal.valueOf(medidaPorParte), 6, RoundingMode.HALF_UP);

        int nuevaCantidadOrigen = cantidadDisponibleOrigen - cantidadEntera;
        int nuevaMedidaOrigen = nuevaCantidadOrigen * medidaPorParte;

        BigDecimal nuevoCostoOrigen = costoUnitarioParte
                .multiply(BigDecimal.valueOf(nuevaCantidadOrigen))
                .setScale(2, RoundingMode.HALF_UP);

        parteDao.actualizarLoteParte(
                loteOrigen.getIdParte(),
                nuevaCantidadOrigen,
                nuevaMedidaOrigen,
                BigDecimal.valueOf(nuevaCantidadOrigen).setScale(6, RoundingMode.HALF_UP),
                nuevoCostoOrigen,
                costoUnitarioParte,
                nuevaCantidadOrigen > 0 ? "DISPONIBLE" : "AGOTADA"
        );

        BigDecimal costoTotalTransferido = costoUnitarioParte
                .multiply(BigDecimal.valueOf(cantidadEntera))
                .setScale(2, RoundingMode.HALF_UP);

        int medidaTransferida = cantidadEntera * medidaPorParte;

        var loteDestinoOpt = parteDao.buscarLoteCompletoDisponible(idProducto, idSedeDestino);

        if (loteDestinoOpt.isPresent()) {
            InventarioParte loteDestino = loteDestinoOpt.get();

            parteDao.incrementarLoteParte(
                    loteDestino.getIdParte(),
                    cantidadEntera,
                    medidaTransferida,
                    BigDecimal.valueOf(cantidadEntera).setScale(6, RoundingMode.HALF_UP),
                    costoTotalTransferido,
                    "DISPONIBLE"
            );

        } else {
            InventarioParte nuevaParteDestino = new InventarioParte();

            nuevaParteDestino.setIdProducto(idProducto);
            nuevaParteDestino.setIdSede(idSedeDestino);
            nuevaParteDestino.setTipoParte("COMPLETA");
            nuevaParteDestino.setCantidadPartes(cantidadEntera);
            nuevaParteDestino.setDescripcion("Lote transferido desde otra sede");
            nuevaParteDestino.setTipoMedida(loteOrigen.getTipoMedida());
            nuevaParteDestino.setUnidadMedida(loteOrigen.getUnidadMedida());
            nuevaParteDestino.setMedidaDisponible(medidaTransferida);
            nuevaParteDestino.setMedidaPorParte(medidaPorParte);
            nuevaParteDestino.setEquivalenteUnidadFisica(BigDecimal.valueOf(cantidadEntera).setScale(6, RoundingMode.HALF_UP));
            nuevaParteDestino.setCostoTotalOriginal(costoTotalTransferido);
            nuevaParteDestino.setCostoTotalDisponible(costoTotalTransferido);
            nuevaParteDestino.setCostoUnitarioParte(costoUnitarioParte);
            nuevaParteDestino.setCostoPorMedida(costoPorMedida);
            nuevaParteDestino.setEstado("DISPONIBLE");

            parteDao.registrar(nuevaParteDestino);
        }

        stockDao.recalcularStockDesdePartes(idProducto, idSedeOrigen);
        stockDao.recalcularValorDesdePartes(idProducto, idSedeOrigen);

        stockDao.recalcularStockDesdePartes(idProducto, idSedeDestino);
        stockDao.recalcularValorDesdePartes(idProducto, idSedeDestino);

        InventarioStock stockOrigen = stockDao.buscarPorProductoYSede(idProducto, idSedeOrigen)
                .orElseThrow(() -> new RuntimeException("No se pudo recalcular el stock de origen."));

        InventarioStock stockDestino = stockDao.buscarPorProductoYSede(idProducto, idSedeDestino)
                .orElseThrow(() -> new RuntimeException("No se pudo recalcular el stock de destino."));

        InventarioMovimiento salida = new InventarioMovimiento();
        salida.setIdProducto(idProducto);
        salida.setIdSede(idSedeOrigen);
        salida.setTipoMovimiento("SALIDA");
        salida.setOrigenMovimiento("TRANSFERENCIA");
        salida.setDocumentoReferencia(documentoReferencia);
        salida.setEntrada(BigDecimal.ZERO);
        salida.setSalida(BigDecimal.valueOf(cantidadEntera));
        salida.setStockResultante(stockOrigen.getStockActual());
        salida.setCostoUnitario(costoUnitarioParte);
        salida.setObservacion("Salida por transferencia de unidad completa");
        salida.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(salida);

        InventarioMovimiento entrada = new InventarioMovimiento();
        entrada.setIdProducto(idProducto);
        entrada.setIdSede(idSedeDestino);
        entrada.setTipoMovimiento("ENTRADA");
        entrada.setOrigenMovimiento("TRANSFERENCIA");
        entrada.setDocumentoReferencia(documentoReferencia);
        entrada.setEntrada(BigDecimal.valueOf(cantidadEntera));
        entrada.setSalida(BigDecimal.ZERO);
        entrada.setStockResultante(stockDestino.getStockActual());
        entrada.setCostoUnitario(costoUnitarioParte);
        entrada.setObservacion("Entrada por transferencia de unidad completa");
        entrada.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(entrada);
    }

    private void aumentarStockTransferencia(Integer idProducto,
                                            Integer idSede,
                                            BigDecimal cantidad,
                                            String documentoReferencia,
                                            Integer idUsuario) {

        var stockOpt = stockDao.buscarPorProductoYSede(idProducto, idSede);

        BigDecimal nuevoStock;

        if (stockOpt.isPresent()) {
            InventarioStock stock = stockOpt.get();
            nuevoStock = stock.getStockActual().add(cantidad);
            stockDao.actualizarStock(idProducto, idSede, nuevoStock);
        } else {
            nuevoStock = cantidad;
            stockDao.crearStockInicial(idProducto, idSede, cantidad, BigDecimal.ZERO);
        }

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("ENTRADA");
        mov.setOrigenMovimiento("TRANSFERENCIA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(cantidad);
        mov.setSalida(BigDecimal.ZERO);
        mov.setStockResultante(nuevoStock);
        mov.setCostoUnitario(null);
        mov.setObservacion("Entrada por transferencia");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    private void descontarStockTransferencia(Integer idProducto,
                                             Integer idSede,
                                             BigDecimal cantidad,
                                             String documentoReferencia,
                                             Integer idUsuario) {

        var stockOpt = stockDao.buscarPorProductoYSede(idProducto, idSede);

        if (stockOpt.isEmpty()) {
            throw new RuntimeException("No existe stock para el producto.");
        }

        InventarioStock stock = stockOpt.get();

        if (stock.getStockActual().compareTo(cantidad) < 0) {
            throw new RuntimeException("Stock insuficiente para el producto.");
        }

        BigDecimal nuevoStock = stock.getStockActual().subtract(cantidad);

        stockDao.actualizarStock(idProducto, idSede, nuevoStock);

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("SALIDA");
        mov.setOrigenMovimiento("TRANSFERENCIA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(BigDecimal.ZERO);
        mov.setSalida(cantidad);
        mov.setStockResultante(nuevoStock);
        mov.setCostoUnitario(null);
        mov.setObservacion("Salida por transferencia");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    public List<InventarioAjuste> listarAjustes() {
        List<InventarioAjuste> ajustes = ajusteDao.listarAjustes();

        for (InventarioAjuste a : ajustes) {
            a.setDetalles(ajusteDao.listarDetalles(a.getIdAjuste()));
        }

        return ajustes;
    }

    public List<InventarioAjuste> listarAjustesPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        List<InventarioAjuste> ajustes = ajusteDao.listarAjustesPorSede(idSede);

        for (InventarioAjuste a : ajustes) {
            a.setDetalles(ajusteDao.listarDetalles(a.getIdAjuste()));
        }

        return ajustes;
    }

    public InventarioAjuste buscarAjustePorId(Integer idAjuste) {
        if (idAjuste == null || idAjuste <= 0) {
            throw new RuntimeException("Ajuste inválido.");
        }

        InventarioAjuste ajuste = ajusteDao.buscarPorId(idAjuste)
                .orElseThrow(() -> new RuntimeException("No se encontró el ajuste."));

        ajuste.setDetalles(ajusteDao.listarDetalles(idAjuste));

        return ajuste;
    }

    public String generarCodigoAjuste() {
        return ajusteDao.generarCodigo();
    }

    @Transactional
    public void registrarAjusteConInventario(InventarioAjuste ajuste,
                                             List<InventarioAjusteDetalle> detalles) {

        validarCabeceraAjuste(ajuste, detalles);

        ajuste.setCodigo(ajusteDao.generarCodigo());
        ajuste.setEstado("APLICADO");

        int idAjuste = ajusteDao.registrarAjuste(ajuste);

        for (InventarioAjusteDetalle d : detalles) {

            validarDetalleAjuste(d);

            d.setIdAjuste(idAjuste);

            String tipoPresentacion = normalizarTipoPresentacion(d.getTipoPresentacion());
            d.setTipoPresentacion(tipoPresentacion);

            String motivoFinal = d.getMotivoDetalle() != null && !d.getMotivoDetalle().trim().isEmpty()
                    ? d.getMotivoDetalle().trim().toUpperCase()
                    : ajuste.getMotivo().trim().toUpperCase();

            d.setMotivoDetalle(motivoFinal);

            boolean esAumento = "AUMENTO".equalsIgnoreCase(ajuste.getTipoAjuste());

            ResultadoAjusteInventario resultado;

            if (esAumento) {
                resultado = procesarAumentoPorPresentacion(ajuste, d, tipoPresentacion);
            } else {
                resultado = procesarDisminucionPorPresentacion(ajuste, d, tipoPresentacion);
            }

            d.setFactorConversionStock(resultado.factorConversionStock());
            d.setCantidadDescontadaStock(resultado.cantidadAfectadaStock());
            d.setStockAnterior(resultado.stockAnterior());
            d.setStockNuevo(resultado.stockNuevo());

            if (esPerdida(motivoFinal) && !esAumento) {
                d.setValorPerdida(resultado.valorCostoAfectado());
            } else {
                d.setValorPerdida(BigDecimal.ZERO);
            }

            ajusteDao.registrarDetalle(d);

            registrarMovimientoAjuste(
                    ajuste,
                    d,
                    esAumento,
                    resultado.cantidadAfectadaStock(),
                    resultado.stockNuevo(),
                    resultado.costoUnitarioMovimiento()
            );
        }
    }

    private void validarCabeceraAjuste(InventarioAjuste ajuste,
                                       List<InventarioAjusteDetalle> detalles) {

        if (ajuste == null) {
            throw new RuntimeException("El ajuste no puede ser nulo.");
        }

        if (ajuste.getIdSede() == null || ajuste.getIdSede() <= 0) {
            throw new RuntimeException("No se encontró una sede válida para el ajuste.");
        }

        if (ajuste.getMotivo() == null || ajuste.getMotivo().trim().isEmpty()) {
            throw new RuntimeException("Debe seleccionar un motivo para el ajuste.");
        }

        if (ajuste.getTipoAjuste() == null || ajuste.getTipoAjuste().trim().isEmpty()) {
            throw new RuntimeException("Debe seleccionar el tipo de ajuste.");
        }

        String tipo = ajuste.getTipoAjuste().trim().toUpperCase();

        if (!"AUMENTO".equals(tipo) && !"DISMINUCION".equals(tipo)) {
            throw new RuntimeException("El tipo de ajuste debe ser AUMENTO o DISMINUCION.");
        }

        if (detalles == null || detalles.isEmpty()) {
            throw new RuntimeException("Debe agregar al menos un producto al ajuste.");
        }
    }

    private void validarDetalleAjuste(InventarioAjusteDetalle d) {

        if (d == null) {
            throw new RuntimeException("Hay un detalle de ajuste inválido.");
        }

        if (d.getIdProducto() == null || d.getIdProducto() <= 0) {
            throw new RuntimeException("Hay un producto inválido en el ajuste.");
        }

        if (d.getCantidad() == null || d.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad del ajuste debe ser mayor a 0.");
        }

        if (d.getTipoPresentacion() == null || d.getTipoPresentacion().trim().isEmpty()) {
            throw new RuntimeException("Debe seleccionar la presentación del producto.");
        }
    }

    private String normalizarTipoPresentacion(String tipoPresentacion) {
        String tipo = tipoPresentacion == null ? "" : tipoPresentacion.trim().toUpperCase();

        return switch (tipo) {
            case "UNIDAD" -> "UNIDAD";
            case "MEDIA" -> "MEDIA";
            case "PIE2", "PIE_2", "PIE²", "PIE" -> "PIE2";
            case "METRO_LINEAL", "ML", "METRO" -> "METRO_LINEAL";
            default -> throw new RuntimeException("Tipo de presentación no válido: " + tipoPresentacion);
        };
    }

    private ResultadoAjusteInventario procesarAumentoPorPresentacion(InventarioAjuste ajuste,
                                                                     InventarioAjusteDetalle d,
                                                                     String tipoPresentacion) {

        boolean productoConPartes = esProductoConPartes(d.getIdProducto(), ajuste.getIdSede());

        if (productoConPartes) {
            return switch (tipoPresentacion) {
                case "UNIDAD" -> aumentarParteCompletaPorAjuste(ajuste, d);
                case "MEDIA" -> aumentarMediaPartePorAjuste(ajuste, d);
                case "PIE2", "METRO_LINEAL" -> aumentarMedidaPorAjuste(ajuste, d);
                default -> throw new RuntimeException("Presentación no válida para aumento.");
            };
        }

        return switch (tipoPresentacion) {
            case "UNIDAD", "METRO_LINEAL", "PIE2" -> aumentarStockNormalPorAjuste(ajuste, d, BigDecimal.ONE);
            case "MEDIA" -> aumentarStockNormalPorAjuste(ajuste, d, new BigDecimal("0.5"));
            default -> throw new RuntimeException("Presentación no válida para aumento.");
        };
    }

    private ResultadoAjusteInventario procesarDisminucionPorPresentacion(InventarioAjuste ajuste,
                                                                         InventarioAjusteDetalle d,
                                                                         String tipoPresentacion) {

        boolean productoConPartes = esProductoConPartes(d.getIdProducto(), ajuste.getIdSede());

        if (productoConPartes) {
            return switch (tipoPresentacion) {
                case "UNIDAD" -> descontarParteCompletaPorAjuste(ajuste, d);
                case "MEDIA" -> descontarMediaPartePorAjuste(ajuste, d);
                case "PIE2", "METRO_LINEAL" -> descontarMedidaPorAjuste(ajuste, d);
                default -> throw new RuntimeException("Presentación no válida para disminución.");
            };
        }

        return switch (tipoPresentacion) {
            case "UNIDAD", "METRO_LINEAL", "PIE2" -> descontarStockNormalPorAjuste(ajuste, d, BigDecimal.ONE);
            case "MEDIA" -> descontarStockNormalPorAjuste(ajuste, d, new BigDecimal("0.5"));
            default -> throw new RuntimeException("Presentación no válida para disminución.");
        };
    }

    private ResultadoAjusteInventario aumentarStockNormalPorAjuste(InventarioAjuste ajuste,
                                                                   InventarioAjusteDetalle d,
                                                                   BigDecimal factor) {

        InventarioStock stock = stockDao.buscarPorProductoYSede(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto seleccionado."));

        BigDecimal stockAnterior = nvl(stock.getStockActual());
        BigDecimal cantidadAfectada = d.getCantidad().multiply(factor).setScale(6, RoundingMode.HALF_UP);
        BigDecimal stockNuevo = stockAnterior.add(cantidadAfectada);

        stockDao.actualizarStock(d.getIdProducto(), ajuste.getIdSede(), stockNuevo);

        BigDecimal costoUnitario = nvl(stock.getCostoPromedio());
        BigDecimal valorCosto = costoUnitario.multiply(cantidadAfectada).setScale(2, RoundingMode.HALF_UP);

        return new ResultadoAjusteInventario(
                stockAnterior,
                stockNuevo,
                cantidadAfectada,
                factor,
                costoUnitario,
                valorCosto
        );
    }

    private ResultadoAjusteInventario descontarStockNormalPorAjuste(InventarioAjuste ajuste,
                                                                    InventarioAjusteDetalle d,
                                                                    BigDecimal factor) {

        InventarioStock stock = stockDao.buscarPorProductoYSede(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto seleccionado."));

        BigDecimal stockAnterior = nvl(stock.getStockActual());
        BigDecimal cantidadAfectada = d.getCantidad().multiply(factor).setScale(6, RoundingMode.HALF_UP);

        if (stockAnterior.compareTo(cantidadAfectada) < 0) {
            throw new RuntimeException(
                    "Stock insuficiente para disminuir el producto: "
                            + stock.getNombreProducto()
                            + ". Disponible: "
                            + stockAnterior
                            + ", solicitado: "
                            + cantidadAfectada
            );
        }

        BigDecimal stockNuevo = stockAnterior.subtract(cantidadAfectada);

        stockDao.actualizarStock(d.getIdProducto(), ajuste.getIdSede(), stockNuevo);

        BigDecimal costoUnitario = nvl(stock.getCostoPromedio());
        BigDecimal valorCosto = costoUnitario.multiply(cantidadAfectada).setScale(2, RoundingMode.HALF_UP);

        return new ResultadoAjusteInventario(
                stockAnterior,
                stockNuevo,
                cantidadAfectada,
                factor,
                costoUnitario,
                valorCosto
        );
    }

    private ResultadoAjusteInventario aumentarParteCompletaPorAjuste(InventarioAjuste ajuste,
                                                                     InventarioAjusteDetalle d) {

        validarCantidadEntera(d.getCantidad(), "El aumento de partes completas debe ser entero.");

        int cantidad = d.getCantidad().intValueExact();

        InventarioStock stockAntes = stockDao.buscarPorProductoYSede(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto seleccionado."));

        BigDecimal stockAnterior = nvl(stockAntes.getStockActual());
        BigDecimal costoUnitarioParte = nvl(stockAntes.getCostoPromedio());

        if (costoUnitarioParte.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El producto no tiene costo promedio para registrar el aumento por parte.");
        }

        Integer medidaVendibleBase = obtenerMedidaBaseDesdePartes(d.getIdProducto(), ajuste.getIdSede());

        BigDecimal costoTotalIngreso = costoUnitarioParte
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal costoPorMedida = costoUnitarioParte.divide(
                BigDecimal.valueOf(medidaVendibleBase),
                6,
                RoundingMode.HALF_UP
        );

        var loteOpt = parteDao.buscarLoteCompletoDisponible(d.getIdProducto(), ajuste.getIdSede());

        if (loteOpt.isPresent()) {
            InventarioParte lote = loteOpt.get();

            int cantidadActual = lote.getCantidadPartes() == null ? 0 : lote.getCantidadPartes();
            int nuevaCantidad = cantidadActual + cantidad;

            int medidaActual = lote.getMedidaDisponible() == null ? 0 : lote.getMedidaDisponible();
            int nuevaMedidaDisponible = medidaActual + (cantidad * medidaVendibleBase);

            BigDecimal costoActual = nvl(lote.getCostoTotalDisponible());
            BigDecimal nuevoCostoTotal = costoActual.add(costoTotalIngreso).setScale(2, RoundingMode.HALF_UP);

            BigDecimal nuevoCostoUnitario = nuevoCostoTotal
                    .divide(BigDecimal.valueOf(nuevaCantidad), 2, RoundingMode.HALF_UP);

            parteDao.actualizarLoteParte(
                    lote.getIdParte(),
                    nuevaCantidad,
                    nuevaMedidaDisponible,
                    BigDecimal.valueOf(nuevaCantidad).setScale(6, RoundingMode.HALF_UP),
                    nuevoCostoTotal,
                    nuevoCostoUnitario,
                    "DISPONIBLE"
            );

        } else {
            InventarioParte parte = new InventarioParte();
            parte.setIdProducto(d.getIdProducto());
            parte.setIdSede(ajuste.getIdSede());
            String tipoMedidaDetectada = obtenerTipoMedidaDesdePartes(d.getIdProducto(), ajuste.getIdSede());
            String unidadMedidaDetectada = "LONGITUD".equalsIgnoreCase(tipoMedidaDetectada) ? "METRO" : "PIE2";

            parte.setTipoParte("COMPLETA");
            parte.setCantidadPartes(cantidad);
            parte.setDescripcion("Lote de partes completas ingresado por ajuste");
            parte.setTipoMedida(tipoMedidaDetectada);
            parte.setUnidadMedida(unidadMedidaDetectada);


            parte.setMedidaDisponible(cantidad * medidaVendibleBase);
            parte.setMedidaPorParte(medidaVendibleBase);

            parte.setEquivalenteUnidadFisica(BigDecimal.valueOf(cantidad).setScale(6, RoundingMode.HALF_UP));

            parte.setCostoTotalOriginal(costoTotalIngreso);
            parte.setCostoTotalDisponible(costoTotalIngreso);
            parte.setCostoUnitarioParte(costoUnitarioParte);
            parte.setCostoPorMedida(costoPorMedida);

            parte.setEstado("DISPONIBLE");

            parteDao.registrar(parte);
        }

        InventarioStock stockDespues = recalcularYObtenerStockActualizado(
                d.getIdProducto(),
                ajuste.getIdSede()
        );

        BigDecimal stockNuevo = nvl(stockDespues.getStockActual());
        BigDecimal cantidadAfectada = stockNuevo.subtract(stockAnterior).abs();

        return new ResultadoAjusteInventario(
                stockAnterior,
                stockNuevo,
                cantidadAfectada,
                BigDecimal.ONE,
                costoUnitarioParte,
                costoTotalIngreso
        );
    }

    private ResultadoAjusteInventario aumentarMediaPartePorAjuste(InventarioAjuste ajuste,
                                                                  InventarioAjusteDetalle d) {

        validarCantidadEntera(d.getCantidad(), "El aumento de medias partes debe ser entero.");

        int cantidad = d.getCantidad().intValueExact();

        InventarioStock stockAntes = stockDao.buscarPorProductoYSede(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto seleccionado."));

        BigDecimal stockAnterior = nvl(stockAntes.getStockActual());
        BigDecimal costoParte = nvl(stockAntes.getCostoPromedio());

        if (costoParte.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El producto no tiene costo promedio para registrar medias por ajuste.");
        }

        Integer medidaBase = obtenerMedidaBaseDesdePartes(d.getIdProducto(), ajuste.getIdSede());
        int medidaMedia = Math.max(1, medidaBase / 2);

        BigDecimal costoMedia = costoParte.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal costoPorMedida = costoMedia.divide(BigDecimal.valueOf(medidaMedia), 6, RoundingMode.HALF_UP);

        for (int i = 0; i < cantidad; i++) {
            InventarioParte parte = new InventarioParte();
            parte.setIdProducto(d.getIdProducto());
            parte.setIdSede(ajuste.getIdSede());
            String tipoMedidaDetectada = obtenerTipoMedidaDesdePartes(d.getIdProducto(), ajuste.getIdSede());
            String unidadMedidaDetectada = "LONGITUD".equalsIgnoreCase(tipoMedidaDetectada) ? "METRO" : "PIE2";

            parte.setTipoParte("MEDIA");
            parte.setCantidadPartes(1);
            parte.setDescripcion(
                    "LONGITUD".equalsIgnoreCase(tipoMedidaDetectada)
                            ? "Media unidad de longitud ingresada por ajuste"
                            : "Media parte ingresada por ajuste"
            );
            parte.setTipoMedida(tipoMedidaDetectada);
            parte.setUnidadMedida(unidadMedidaDetectada);


            parte.setMedidaDisponible(medidaMedia);
            parte.setMedidaPorParte(medidaMedia);

            parte.setEquivalenteUnidadFisica(new BigDecimal("0.500000"));

            parte.setCostoTotalOriginal(costoMedia);
            parte.setCostoTotalDisponible(costoMedia);
            parte.setCostoUnitarioParte(costoMedia);
            parte.setCostoPorMedida(costoPorMedida);

            parte.setEstado("DISPONIBLE");

            parteDao.registrar(parte);
        }

        InventarioStock stockDespues = recalcularYObtenerStockActualizado(
                d.getIdProducto(),
                ajuste.getIdSede()
        );

        BigDecimal stockNuevo = nvl(stockDespues.getStockActual());
        BigDecimal cantidadAfectada = stockNuevo.subtract(stockAnterior).abs();
        BigDecimal valorCosto = costoMedia.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);

        return new ResultadoAjusteInventario(
                stockAnterior,
                stockNuevo,
                cantidadAfectada,
                new BigDecimal("0.5"),
                costoMedia,
                valorCosto
        );
    }

    private ResultadoAjusteInventario aumentarMedidaPorAjuste(InventarioAjuste ajuste,
                                                              InventarioAjusteDetalle d) {

        validarCantidadEntera(d.getCantidad(), "El aumento por medida debe ser entero.");

        int medida = d.getCantidad().intValueExact();

        InventarioStock stockAntes = stockDao.buscarPorProductoYSede(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto seleccionado."));

        BigDecimal stockAnterior = nvl(stockAntes.getStockActual());
        BigDecimal costoParte = nvl(stockAntes.getCostoPromedio());

        if (costoParte.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El producto no tiene costo promedio para registrar medida por ajuste.");
        }

        Integer medidaBase = obtenerMedidaBaseDesdePartes(d.getIdProducto(), ajuste.getIdSede());

        BigDecimal costoPorMedida = costoParte.divide(BigDecimal.valueOf(medidaBase), 6, RoundingMode.HALF_UP);
        BigDecimal costoDisponible = costoPorMedida.multiply(BigDecimal.valueOf(medida)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal equivalente = BigDecimal.valueOf(medida)
                .divide(BigDecimal.valueOf(medidaBase), 6, RoundingMode.HALF_UP);

        InventarioParte retazo = new InventarioParte();
        retazo.setIdProducto(d.getIdProducto());
        retazo.setIdSede(ajuste.getIdSede());
        String tipoMedidaDetectada = obtenerTipoMedidaDesdePartes(d.getIdProducto(), ajuste.getIdSede());
        String unidadMedidaDetectada = "LONGITUD".equalsIgnoreCase(tipoMedidaDetectada) ? "METRO" : "PIE2";

        retazo.setTipoParte("LONGITUD".equalsIgnoreCase(tipoMedidaDetectada) ? "CORTE" : "RETAZO");
        retazo.setCantidadPartes(1);
        retazo.setDescripcion("Ajuste de medida ingresado al inventario");
        retazo.setTipoMedida(tipoMedidaDetectada);
        retazo.setUnidadMedida(unidadMedidaDetectada);


        retazo.setMedidaDisponible(medida);
        retazo.setMedidaPorParte(medida);

        retazo.setEquivalenteUnidadFisica(equivalente);

        retazo.setCostoTotalOriginal(costoDisponible);
        retazo.setCostoTotalDisponible(costoDisponible);
        retazo.setCostoUnitarioParte(costoDisponible);
        retazo.setCostoPorMedida(costoPorMedida);

        retazo.setEstado("DISPONIBLE");

        parteDao.registrar(retazo);

        InventarioStock stockDespues = recalcularYObtenerStockActualizado(
                d.getIdProducto(),
                ajuste.getIdSede()
        );

        BigDecimal stockNuevo = nvl(stockDespues.getStockActual());
        BigDecimal cantidadAfectada = equivalente;

        return new ResultadoAjusteInventario(
                stockAnterior,
                stockNuevo,
                cantidadAfectada,
                equivalente,
                costoPorMedida,
                costoDisponible
        );
    }

    private ResultadoAjusteInventario descontarParteCompletaPorAjuste(InventarioAjuste ajuste,
                                                                      InventarioAjusteDetalle d) {

        validarCantidadEntera(d.getCantidad(), "La disminución de partes completas debe ser entera.");

        int cantidad = d.getCantidad().intValueExact();

        InventarioStock stockAntes = stockDao.buscarPorProductoYSede(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto seleccionado."));

        BigDecimal stockAnterior = nvl(stockAntes.getStockActual());

        InventarioParte lote = parteDao.buscarLoteCompletoDisponible(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No hay lote de partes completas disponible."));

        int cantidadDisponible = lote.getCantidadPartes() == null ? 0 : lote.getCantidadPartes();

        if (cantidadDisponible < cantidad) {
            throw new RuntimeException("Stock insuficiente de partes completas disponibles.");
        }

        BigDecimal costoUnitario = lote.getCostoUnitarioParte() != null
                ? lote.getCostoUnitarioParte()
                : nvl(lote.getCostoTotalDisponible()).divide(BigDecimal.valueOf(cantidadDisponible), 2, RoundingMode.HALF_UP);

        int nuevaCantidad = cantidadDisponible - cantidad;

        int medidaPorParte = lote.getMedidaPorParte() != null && lote.getMedidaPorParte() > 0
                ? lote.getMedidaPorParte()
                : obtenerMedidaBaseDesdePartes(d.getIdProducto(), ajuste.getIdSede());

        int nuevaMedidaDisponible = nuevaCantidad * medidaPorParte;

        BigDecimal nuevoCostoTotal = costoUnitario
                .multiply(BigDecimal.valueOf(nuevaCantidad))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal costoTotalSalida = costoUnitario
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(2, RoundingMode.HALF_UP);

        parteDao.actualizarLoteParte(
                lote.getIdParte(),
                nuevaCantidad,
                nuevaMedidaDisponible,
                BigDecimal.valueOf(nuevaCantidad).setScale(6, RoundingMode.HALF_UP),
                nuevoCostoTotal,
                costoUnitario,
                nuevaCantidad > 0 ? "DISPONIBLE" : "AGOTADA"
        );

        InventarioStock stockDespues = recalcularYObtenerStockActualizado(
                d.getIdProducto(),
                ajuste.getIdSede()
        );

        BigDecimal stockNuevo = nvl(stockDespues.getStockActual());
        BigDecimal cantidadAfectada = stockAnterior.subtract(stockNuevo).abs();

        return new ResultadoAjusteInventario(
                stockAnterior,
                stockNuevo,
                cantidadAfectada,
                BigDecimal.ONE,
                costoUnitario,
                costoTotalSalida
        );
    }

    private ResultadoAjusteInventario descontarMediaPartePorAjuste(InventarioAjuste ajuste,
                                                                   InventarioAjusteDetalle d) {

        validarCantidadEntera(d.getCantidad(), "La disminución de medias partes debe ser entera.");

        int cantidad = d.getCantidad().intValueExact();

        InventarioStock stockAntes = stockDao.buscarPorProductoYSede(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto seleccionado."));

        BigDecimal stockAnterior = nvl(stockAntes.getStockActual());
        BigDecimal costoTotalSalida = BigDecimal.ZERO;

        for (int i = 1; i <= cantidad; i++) {
            List<InventarioParte> partesDisponibles =
                    parteDao.listarDisponiblesPorProductoYSede(d.getIdProducto(), ajuste.getIdSede());

            InventarioParte mediaDisponible = partesDisponibles.stream()
                    .filter(p -> "MEDIA".equalsIgnoreCase(p.getTipoParte()))
                    .findFirst()
                    .orElse(null);

            if (mediaDisponible != null) {
                BigDecimal costoDisponible = nvl(mediaDisponible.getCostoTotalDisponible());
                costoTotalSalida = costoTotalSalida.add(costoDisponible);

                parteDao.actualizarDisponibilidadParte(
                        mediaDisponible.getIdParte(),
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "AGOTADA"
                );

                continue;
            }

            InventarioParte completaDisponible = parteDao.buscarLoteCompletoDisponible(d.getIdProducto(), ajuste.getIdSede())
                    .orElse(null);

            if (completaDisponible == null) {
                throw new RuntimeException("Stock insuficiente de medias partes o partes completas disponibles.");
            }

            int cantidadLote = completaDisponible.getCantidadPartes() == null ? 0 : completaDisponible.getCantidadPartes();

            if (cantidadLote <= 0) {
                throw new RuntimeException("No hay partes completas disponibles en el lote.");
            }

            BigDecimal costoCompleta = completaDisponible.getCostoUnitarioParte() != null
                    ? completaDisponible.getCostoUnitarioParte()
                    : nvl(completaDisponible.getCostoTotalDisponible())
                    .divide(BigDecimal.valueOf(cantidadLote), 2, RoundingMode.HALF_UP);

            BigDecimal costoMedia = costoCompleta.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

            BigDecimal costoPorMedida = completaDisponible.getCostoPorMedida() != null
                    ? completaDisponible.getCostoPorMedida()
                    : BigDecimal.ZERO;

            Integer medidaCompleta = completaDisponible.getMedidaPorParte() != null
                    ? completaDisponible.getMedidaPorParte()
                    : obtenerMedidaBaseDesdePartes(d.getIdProducto(), ajuste.getIdSede());

            int medidaMedia = medidaCompleta / 2;

            costoTotalSalida = costoTotalSalida.add(costoMedia);

            int nuevaCantidadLote = cantidadLote - 1;
            int nuevaMedidaDisponibleLote = nuevaCantidadLote * medidaCompleta;

            BigDecimal nuevoCostoTotalLote = costoCompleta
                    .multiply(BigDecimal.valueOf(nuevaCantidadLote))
                    .setScale(2, RoundingMode.HALF_UP);

            parteDao.actualizarLoteParte(
                    completaDisponible.getIdParte(),
                    nuevaCantidadLote,
                    nuevaMedidaDisponibleLote,
                    BigDecimal.valueOf(nuevaCantidadLote).setScale(6, RoundingMode.HALF_UP),
                    nuevoCostoTotalLote,
                    costoCompleta,
                    nuevaCantidadLote > 0 ? "DISPONIBLE" : "AGOTADA"
            );

            InventarioParte mediaRestante = new InventarioParte();
            mediaRestante.setIdProducto(d.getIdProducto());
            mediaRestante.setIdSede(ajuste.getIdSede());
            boolean esLongitud = "LONGITUD".equalsIgnoreCase(completaDisponible.getTipoMedida());

            mediaRestante.setTipoParte("MEDIA");
            mediaRestante.setCantidadPartes(1);
            mediaRestante.setDescripcion(
                    esLongitud
                            ? "Media unidad de longitud restante por ajuste"
                            : "Media parte restante por ajuste de media parte"
            );
            mediaRestante.setTipoMedida(completaDisponible.getTipoMedida());
            mediaRestante.setUnidadMedida(completaDisponible.getUnidadMedida());


            mediaRestante.setMedidaDisponible(medidaMedia);
            mediaRestante.setMedidaPorParte(medidaMedia);

            mediaRestante.setEquivalenteUnidadFisica(new BigDecimal("0.500000"));

            mediaRestante.setCostoTotalOriginal(costoMedia);
            mediaRestante.setCostoTotalDisponible(costoMedia);
            mediaRestante.setCostoUnitarioParte(costoMedia);
            mediaRestante.setCostoPorMedida(costoPorMedida);

            mediaRestante.setEstado("DISPONIBLE");

            parteDao.registrar(mediaRestante);
        }

        InventarioStock stockDespues = recalcularYObtenerStockActualizado(
                d.getIdProducto(),
                ajuste.getIdSede()
        );

        BigDecimal stockNuevo = nvl(stockDespues.getStockActual());
        BigDecimal cantidadAfectada = stockAnterior.subtract(stockNuevo).abs();

        BigDecimal costoUnitario = cantidad > 0
                ? costoTotalSalida.divide(BigDecimal.valueOf(cantidad), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ResultadoAjusteInventario(
                stockAnterior,
                stockNuevo,
                cantidadAfectada,
                new BigDecimal("0.5"),
                costoUnitario,
                costoTotalSalida
        );
    }

    private ResultadoAjusteInventario descontarMedidaPorAjuste(InventarioAjuste ajuste,
                                                               InventarioAjusteDetalle d) {

        validarCantidadEntera(d.getCantidad(), "La disminución por medida debe ser entera.");

        int medidaSolicitada = d.getCantidad().intValueExact();

        InventarioStock stockAntes = stockDao.buscarPorProductoYSede(d.getIdProducto(), ajuste.getIdSede())
                .orElseThrow(() -> new RuntimeException("No existe stock para el producto seleccionado."));

        BigDecimal stockAnterior = nvl(stockAntes.getStockActual());

        Integer medidaBaseParte = obtenerMedidaBaseDesdePartes(d.getIdProducto(), ajuste.getIdSede());

        List<InventarioParte> partesDisponibles =
                parteDao.listarDisponiblesPorProductoYSede(d.getIdProducto(), ajuste.getIdSede());

        int medidaDisponibleTotal = partesDisponibles.stream()
                .map(InventarioParte::getMedidaDisponible)
                .filter(v -> v != null && v > 0)
                .reduce(0, Integer::sum);

        if (medidaDisponibleTotal < medidaSolicitada) {
            throw new RuntimeException(
                    "Stock insuficiente por partes. Disponible: "
                            + medidaDisponibleTotal + " medida, requerido: "
                            + medidaSolicitada + " medida."
            );
        }

        int restantePorDescontar = medidaSolicitada;
        BigDecimal costoTotalSalida = BigDecimal.ZERO;

        for (InventarioParte parte : partesDisponibles) {

            if (restantePorDescontar <= 0) {
                break;
            }

            Integer medidaParte = parte.getMedidaDisponible();

            if (medidaParte == null || medidaParte <= 0) {
                continue;
            }

            boolean esLoteCompleta = "COMPLETA".equalsIgnoreCase(parte.getTipoParte())
                    && parte.getCantidadPartes() != null
                    && parte.getCantidadPartes() > 1;

            BigDecimal costoDisponible = nvl(parte.getCostoTotalDisponible());

            BigDecimal costoPorMedida = parte.getCostoPorMedida();

            if (costoPorMedida == null || costoPorMedida.compareTo(BigDecimal.ZERO) <= 0) {
                costoPorMedida = costoDisponible.divide(
                        BigDecimal.valueOf(medidaParte),
                        6,
                        RoundingMode.HALF_UP
                );
            }

            if (medidaParte <= restantePorDescontar) {
                costoTotalSalida = costoTotalSalida.add(costoDisponible);

                parteDao.actualizarDisponibilidadParte(
                        parte.getIdParte(),
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "AGOTADA"
                );

                restantePorDescontar -= medidaParte;

            } else {
                int medidaAfectada = restantePorDescontar;

                BigDecimal costoSalida = costoPorMedida
                        .multiply(BigDecimal.valueOf(medidaAfectada))
                        .setScale(2, RoundingMode.HALF_UP);

                costoTotalSalida = costoTotalSalida.add(costoSalida);

                if (esLoteCompleta) {
                    int cantidadLote = parte.getCantidadPartes() == null ? 0 : parte.getCantidadPartes();

                    int medidaPorParte = parte.getMedidaPorParte() != null && parte.getMedidaPorParte() > 0
                            ? parte.getMedidaPorParte()
                            : medidaBaseParte;

                    BigDecimal costoUnitarioParte = parte.getCostoUnitarioParte() != null
                            ? parte.getCostoUnitarioParte()
                            : parte.getCostoTotalDisponible()
                            .divide(BigDecimal.valueOf(cantidadLote), 2, RoundingMode.HALF_UP);

                    int nuevaCantidadLote = cantidadLote - 1;
                    int nuevaMedidaDisponibleLote = nuevaCantidadLote * medidaPorParte;

                    BigDecimal nuevoCostoTotalLote = costoUnitarioParte
                            .multiply(BigDecimal.valueOf(nuevaCantidadLote))
                            .setScale(2, RoundingMode.HALF_UP);

                    parteDao.actualizarLoteParte(
                            parte.getIdParte(),
                            nuevaCantidadLote,
                            nuevaMedidaDisponibleLote,
                            BigDecimal.valueOf(nuevaCantidadLote).setScale(6, RoundingMode.HALF_UP),
                            nuevoCostoTotalLote,
                            costoUnitarioParte,
                            nuevaCantidadLote > 0 ? "DISPONIBLE" : "AGOTADA"
                    );

                    int medidaRestante = medidaPorParte - medidaAfectada;

                    if (medidaRestante > 0) {
                        BigDecimal costoRestante = costoPorMedida
                                .multiply(BigDecimal.valueOf(medidaRestante))
                                .setScale(2, RoundingMode.HALF_UP);

                        BigDecimal equivalenteRestante = BigDecimal.valueOf(medidaRestante)
                                .divide(BigDecimal.valueOf(medidaPorParte), 6, RoundingMode.HALF_UP);

                        InventarioParte retazoRestante = new InventarioParte();
                        retazoRestante.setIdProducto(d.getIdProducto());
                        retazoRestante.setIdSede(ajuste.getIdSede());
                        retazoRestante.setTipoParte("LONGITUD".equalsIgnoreCase(parte.getTipoMedida()) ? "CORTE" : "RETAZO");
                        retazoRestante.setCantidadPartes(1);
                        retazoRestante.setDescripcion("Retazo restante por ajuste de medida desde lote");
                        retazoRestante.setTipoMedida(parte.getTipoMedida());
                        retazoRestante.setUnidadMedida(parte.getUnidadMedida());


                        retazoRestante.setMedidaDisponible(medidaRestante);
                        retazoRestante.setMedidaPorParte(medidaRestante);

                        retazoRestante.setEquivalenteUnidadFisica(equivalenteRestante);

                        retazoRestante.setCostoTotalOriginal(costoRestante);
                        retazoRestante.setCostoTotalDisponible(costoRestante);
                        retazoRestante.setCostoUnitarioParte(costoRestante);
                        retazoRestante.setCostoPorMedida(costoPorMedida);

                        retazoRestante.setEstado("DISPONIBLE");

                        parteDao.registrar(retazoRestante);
                    }

                } else {
                    int medidaRestante = medidaParte - medidaAfectada;

                    BigDecimal costoRestante = costoPorMedida
                            .multiply(BigDecimal.valueOf(medidaRestante))
                            .setScale(2, RoundingMode.HALF_UP);

                    BigDecimal equivalenteRestante = BigDecimal.valueOf(medidaRestante)
                            .divide(BigDecimal.valueOf(medidaBaseParte), 6, RoundingMode.HALF_UP);

                    String tipoParteRestante = "LONGITUD".equalsIgnoreCase(parte.getTipoMedida()) ? "CORTE" : "RETAZO";

                    parteDao.convertirParteEnCorte(
                            parte.getIdParte(),
                            tipoParteRestante,
                            medidaRestante,
                            medidaRestante,
                            null,
                            null,
                            parte.getIdParteOrigen(),
                            parte.getIdDetalleVentaOrigen(),
                            parte.getNumeroPlanoOrigen(),
                            equivalenteRestante,
                            costoRestante,
                            costoRestante,
                            costoPorMedida,
                            "Parte convertida a " + tipoParteRestante.toLowerCase() + " por ajuste de medida"
                    );
                }

                restantePorDescontar = 0;
            }
        }

        InventarioStock stockDespues = recalcularYObtenerStockActualizado(
                d.getIdProducto(),
                ajuste.getIdSede()
        );

        BigDecimal stockNuevo = nvl(stockDespues.getStockActual());

        BigDecimal factor = BigDecimal.ONE.divide(
                BigDecimal.valueOf(medidaBaseParte),
                6,
                RoundingMode.HALF_UP
        );

        BigDecimal cantidadAfectada = d.getCantidad()
                .multiply(factor)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal costoUnitarioMovimiento = costoTotalSalida.divide(
                d.getCantidad(),
                6,
                RoundingMode.HALF_UP
        );

        return new ResultadoAjusteInventario(
                stockAnterior,
                stockNuevo,
                cantidadAfectada,
                factor,
                costoUnitarioMovimiento,
                costoTotalSalida
        );
    }
    private InventarioStock recalcularYObtenerStockActualizado(Integer idProducto, Integer idSede) {

        stockDao.recalcularStockDesdePartes(idProducto, idSede);
        stockDao.recalcularValorDesdePartes(idProducto, idSede);

        return stockDao.buscarPorProductoYSede(idProducto, idSede)
                .orElseThrow(() -> new RuntimeException("No se pudo recalcular el stock después del ajuste."));
    }

    private void registrarMovimientoAjuste(InventarioAjuste ajuste,
                                           InventarioAjusteDetalle d,
                                           boolean esAumento,
                                           BigDecimal cantidadAfectadaStock,
                                           BigDecimal stockNuevo,
                                           BigDecimal costoUnitarioMovimiento) {

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(d.getIdProducto());
        mov.setIdSede(ajuste.getIdSede());
        mov.setTipoMovimiento(esAumento ? "ENTRADA" : "SALIDA");
        mov.setOrigenMovimiento("AJUSTE");
        mov.setDocumentoReferencia(ajuste.getCodigo());
        mov.setEntrada(esAumento ? cantidadAfectadaStock : BigDecimal.ZERO);
        mov.setSalida(esAumento ? BigDecimal.ZERO : cantidadAfectadaStock);
        mov.setStockResultante(stockNuevo);
        mov.setCostoUnitario(costoUnitarioMovimiento);
        mov.setObservacion(construirObservacionAjuste(ajuste, d));
        mov.setIdUsuario(ajuste.getIdUsuario());

        movimientoDao.registrarMovimiento(mov);
    }

    private String construirObservacionAjuste(InventarioAjuste ajuste,
                                              InventarioAjusteDetalle d) {

        String motivo = d.getMotivoDetalle() != null && !d.getMotivoDetalle().trim().isEmpty()
                ? d.getMotivoDetalle()
                : ajuste.getMotivo();

        String presentacion = nombrePresentacionAjuste(d.getTipoPresentacion());

        String observacionDetalle = d.getObservacionDetalle() != null
                ? d.getObservacionDetalle().trim()
                : "";

        String observacionGeneral = ajuste.getObservacion() != null
                ? ajuste.getObservacion().trim()
                : "";

        String base = "Ajuste de inventario por motivo: "
                + motivo
                + ". Cantidad solicitada: "
                + d.getCantidad()
                + " "
                + presentacion
                + ". Equivalente stock: "
                + (d.getCantidadDescontadaStock() != null ? d.getCantidadDescontadaStock() : d.getCantidad());

        if (!observacionDetalle.isEmpty()) {
            return base + ". " + observacionDetalle;
        }

        if (!observacionGeneral.isEmpty()) {
            return base + ". " + observacionGeneral;
        }

        return base;
    }

    private String nombrePresentacionAjuste(String tipoPresentacion) {
        if (tipoPresentacion == null) {
            return "unidad completa";
        }

        return switch (tipoPresentacion.trim().toUpperCase()) {
            case "UNIDAD" -> "unidad completa";
            case "MEDIA" -> "media unidad";
            case "PIE2", "PIE_2", "PIE²", "PIE" -> "pie²";
            case "METRO_LINEAL", "ML", "METRO" -> "metro lineal";
            default -> tipoPresentacion.toLowerCase();
        };
    }

    private boolean esProductoConPartes(Integer idProducto, Integer idSede) {
        return !parteDao.listarDisponiblesPorProductoYSede(idProducto, idSede).isEmpty();
    }

    private void validarCantidadEntera(BigDecimal cantidad, String mensaje) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0.");
        }

        BigDecimal limpia = cantidad.stripTrailingZeros();

        if (limpia.scale() > 0) {
            throw new RuntimeException(mensaje);
        }
    }

    private boolean esPerdida(String motivo) {
        if (motivo == null) {
            return false;
        }

        String m = motivo.trim().toUpperCase();

        return m.equals("PERDIDA")
                || m.equals("PÉRDIDA")
                || m.equals("DANO")
                || m.equals("DAÑO")
                || m.equals("VENCIMIENTO")
                || m.equals("ROTURA");
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private record ResultadoAjusteInventario(
            BigDecimal stockAnterior,
            BigDecimal stockNuevo,
            BigDecimal cantidadAfectadaStock,
            BigDecimal factorConversionStock,
            BigDecimal costoUnitarioMovimiento,
            BigDecimal valorCostoAfectado
    ) {
    }

    @Transactional
    public void registrarIngresoLongitudPorPartes(Integer idProducto,
                                                  Integer idSede,
                                                  Integer cantidadUnidades,
                                                  BigDecimal costoUnitarioUnidad,
                                                  BigDecimal metrosVendiblesPorUnidad,
                                                  String documentoReferencia,
                                                  Integer idUsuario) {

        if (idProducto == null || idProducto <= 0) {
            throw new RuntimeException("Producto inválido.");
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("Sede inválida.");
        }

        if (cantidadUnidades == null || cantidadUnidades <= 0) {
            throw new RuntimeException("La cantidad de unidades recibidas debe ser mayor a 0.");
        }

        if (costoUnitarioUnidad == null || costoUnitarioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El costo unitario debe ser mayor a 0.");
        }

        if (metrosVendiblesPorUnidad == null || metrosVendiblesPorUnidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Debe indicar los metros vendibles por unidad física.");
        }

        BigDecimal costoTotalIngreso = costoUnitarioUnidad
                .multiply(BigDecimal.valueOf(cantidadUnidades))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal costoPorMetro = costoUnitarioUnidad.divide(
                metrosVendiblesPorUnidad,
                6,
                RoundingMode.HALF_UP
        );

        BigDecimal metrosTotales = metrosVendiblesPorUnidad
                .multiply(BigDecimal.valueOf(cantidadUnidades))
                .setScale(6, RoundingMode.HALF_UP);

        Integer metrosPorUnidadEntero = metrosVendiblesPorUnidad
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        Integer metrosTotalesEntero = metrosTotales
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        var loteOpt = parteDao.buscarLoteCompletoDisponible(idProducto, idSede);

        if (loteOpt.isPresent()) {

            InventarioParte lote = loteOpt.get();

            int cantidadActual = lote.getCantidadPartes() == null ? 0 : lote.getCantidadPartes();
            int nuevaCantidad = cantidadActual + cantidadUnidades;

            int medidaActual = lote.getMedidaDisponible() == null ? 0 : lote.getMedidaDisponible();
            int nuevaMedidaDisponible = medidaActual + metrosTotalesEntero;

            BigDecimal equivalenteNuevo = BigDecimal.valueOf(nuevaCantidad)
                    .setScale(6, RoundingMode.HALF_UP);

            BigDecimal costoActual = lote.getCostoTotalDisponible() == null
                    ? BigDecimal.ZERO
                    : lote.getCostoTotalDisponible();

            BigDecimal nuevoCostoTotal = costoActual.add(costoTotalIngreso)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal nuevoCostoUnitario = nuevoCostoTotal
                    .divide(BigDecimal.valueOf(nuevaCantidad), 2, RoundingMode.HALF_UP);

            parteDao.actualizarLoteParte(
                    lote.getIdParte(),
                    nuevaCantidad,
                    nuevaMedidaDisponible,
                    equivalenteNuevo,
                    nuevoCostoTotal,
                    nuevoCostoUnitario,
                    "DISPONIBLE"
            );

        } else {

            InventarioParte parte = new InventarioParte();
            parte.setIdProducto(idProducto);
            parte.setIdSede(idSede);
            parte.setTipoParte("COMPLETA");
            parte.setCantidadPartes(cantidadUnidades);
            parte.setDescripcion("Lote de unidades completas por longitud ingresado por compra");

            parte.setTipoMedida("LONGITUD");
            parte.setUnidadMedida("METRO");

            parte.setMedidaDisponible(metrosTotalesEntero);
            parte.setMedidaPorParte(metrosPorUnidadEntero);
            parte.setEquivalenteUnidadFisica(
                    BigDecimal.valueOf(cantidadUnidades).setScale(6, RoundingMode.HALF_UP)
            );

            parte.setCostoTotalOriginal(costoTotalIngreso);
            parte.setCostoTotalDisponible(costoTotalIngreso);
            parte.setCostoUnitarioParte(costoUnitarioUnidad);
            parte.setCostoPorMedida(costoPorMetro);

            parte.setEstado("DISPONIBLE");

            parteDao.registrar(parte);
        }

        stockDao.recalcularStockDesdePartes(idProducto, idSede);
        stockDao.recalcularValorDesdePartes(idProducto, idSede);

        InventarioStock stockActualizado = stockDao.buscarPorProductoYSede(idProducto, idSede)
                .orElse(null);

        BigDecimal stockResultante = stockActualizado != null && stockActualizado.getStockActual() != null
                ? stockActualizado.getStockActual()
                : BigDecimal.ZERO;

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("ENTRADA");
        mov.setOrigenMovimiento("COMPRA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(BigDecimal.valueOf(cantidadUnidades));
        mov.setSalida(BigDecimal.ZERO);
        mov.setStockResultante(stockResultante);
        mov.setCostoUnitario(costoUnitarioUnidad);
        mov.setObservacion("Ingreso automático de unidades completas por longitud");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    @Transactional
    public void registrarIngresoAreaPorPartes(Integer idProducto,
                                              Integer idSede,
                                              Integer cantidadPartes,
                                              BigDecimal costoUnitarioParte,
                                              Integer medidaVendibleBase,
                                              String documentoReferencia,
                                              Integer idUsuario) {

        if (idProducto == null || idProducto <= 0) {
            throw new RuntimeException("Producto inválido.");
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("Sede inválida.");
        }

        if (cantidadPartes == null || cantidadPartes <= 0) {
            throw new RuntimeException("La cantidad de partes debe ser mayor a 0.");
        }

        if (costoUnitarioParte == null || costoUnitarioParte.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El costo unitario de la parte debe ser mayor a 0.");
        }

        if (medidaVendibleBase == null || medidaVendibleBase <= 0) {
            throw new RuntimeException("Debe indicar los medida vendibles de la parte.");
        }

        BigDecimal costoTotalIngreso = costoUnitarioParte
                .multiply(BigDecimal.valueOf(cantidadPartes))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal costoPorMedida = costoUnitarioParte.divide(
                BigDecimal.valueOf(medidaVendibleBase),
                6,
                RoundingMode.HALF_UP
        );

        var loteOpt = parteDao.buscarLoteCompletoDisponible(idProducto, idSede);

        if (loteOpt.isPresent()) {

            InventarioParte lote = loteOpt.get();

            int cantidadActual = lote.getCantidadPartes() == null ? 0 : lote.getCantidadPartes();
            int nuevaCantidad = cantidadActual + cantidadPartes;

            int medidaActual = lote.getMedidaDisponible() == null ? 0 : lote.getMedidaDisponible();
            int nuevaMedidaDisponible = medidaActual + (cantidadPartes * medidaVendibleBase);

            BigDecimal costoActual = lote.getCostoTotalDisponible() == null
                    ? BigDecimal.ZERO
                    : lote.getCostoTotalDisponible();

            BigDecimal nuevoCostoTotal = costoActual.add(costoTotalIngreso)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal nuevoCostoUnitario = nuevoCostoTotal
                    .divide(BigDecimal.valueOf(nuevaCantidad), 2, RoundingMode.HALF_UP);

            parteDao.actualizarLoteParte(
                    lote.getIdParte(),
                    nuevaCantidad,
                    nuevaMedidaDisponible,
                    BigDecimal.valueOf(nuevaCantidad).setScale(6, RoundingMode.HALF_UP),
                    nuevoCostoTotal,
                    nuevoCostoUnitario,
                    "DISPONIBLE"
            );

        } else {

            InventarioParte lote = new InventarioParte();
            lote.setIdProducto(idProducto);
            lote.setIdSede(idSede);
            lote.setTipoParte("COMPLETA");
            lote.setCantidadPartes(cantidadPartes);
            lote.setDescripcion("Lote de partes completas ingresado por compra");
            lote.setTipoMedida("AREA");
            lote.setUnidadMedida("PIE2");


            lote.setMedidaDisponible(cantidadPartes * medidaVendibleBase);
            lote.setMedidaPorParte(medidaVendibleBase);

            lote.setEquivalenteUnidadFisica(BigDecimal.valueOf(cantidadPartes).setScale(6, RoundingMode.HALF_UP));

            lote.setCostoTotalOriginal(costoTotalIngreso);
            lote.setCostoTotalDisponible(costoTotalIngreso);
            lote.setCostoUnitarioParte(costoUnitarioParte);
            lote.setCostoPorMedida(costoPorMedida);

            lote.setEstado("DISPONIBLE");

            parteDao.registrar(lote);
        }

        stockDao.recalcularStockDesdePartes(idProducto, idSede);
        stockDao.recalcularValorDesdePartes(idProducto, idSede);

        InventarioStock stockActualizado = stockDao.buscarPorProductoYSede(idProducto, idSede)
                .orElse(null);

        BigDecimal stockResultante = stockActualizado != null && stockActualizado.getStockActual() != null
                ? stockActualizado.getStockActual()
                : BigDecimal.ZERO;

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("ENTRADA");
        mov.setOrigenMovimiento("COMPRA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(BigDecimal.valueOf(cantidadPartes));
        mov.setSalida(BigDecimal.ZERO);
        mov.setStockResultante(stockResultante);
        mov.setCostoUnitario(costoUnitarioParte);
        mov.setObservacion("Ingreso automático de partes completas por lote");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    @Transactional
    public void registrarMediaParte(Integer idProducto,
                                    Integer idSede,
                                    BigDecimal costoMediaParte,
                                    Integer medidaVendibleMedia,
                                    Integer medidaVendibleBase) {

        if (medidaVendibleBase == null || medidaVendibleBase <= 0) {
            throw new RuntimeException("Debe indicar los medida vendibles de la parte completa.");
        }

        if (medidaVendibleMedia == null || medidaVendibleMedia <= 0) {
            throw new RuntimeException("Debe indicar los medida vendibles de la media parte.");
        }

        if (costoMediaParte == null || costoMediaParte.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El costo de la media parte debe ser mayor a 0.");
        }

        BigDecimal equivalente = BigDecimal.valueOf(medidaVendibleMedia)
                .divide(BigDecimal.valueOf(medidaVendibleBase), 6, RoundingMode.HALF_UP);

        BigDecimal costoPorMedida = costoMediaParte.divide(
                BigDecimal.valueOf(medidaVendibleMedia),
                6,
                RoundingMode.HALF_UP
        );

        InventarioParte parte = new InventarioParte();
        parte.setIdProducto(idProducto);
        parte.setIdSede(idSede);
        parte.setTipoParte("MEDIA");
        parte.setDescripcion("Media parte disponible");
        parte.setTipoMedida("AREA");
        parte.setUnidadMedida("PIE2");


        parte.setMedidaDisponible(medidaVendibleMedia);

        parte.setEquivalenteUnidadFisica(equivalente);

        parte.setCostoTotalOriginal(costoMediaParte);
        parte.setCostoTotalDisponible(costoMediaParte);
        parte.setCostoPorMedida(costoPorMedida);

        parte.setEstado("DISPONIBLE");

        parteDao.registrar(parte);

        stockDao.recalcularStockDesdePartes(idProducto, idSede);
        stockDao.recalcularValorDesdePartes(idProducto, idSede);
    }

    @Transactional
    public void registrarRetazo(Integer idProducto,
                                Integer idSede,
                                BigDecimal medidaReal,
                                Integer medidaVendible,
                                Integer medidaVendibleBase,
                                BigDecimal costoPorMedidaVendible) {

        if (medidaReal == null || medidaReal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El medida real del retazo debe ser mayor a 0.");
        }

        if (medidaVendible == null || medidaVendible <= 0) {
            throw new RuntimeException("El medida vendible del retazo debe ser mayor a 0.");
        }

        if (medidaVendibleBase == null || medidaVendibleBase <= 0) {
            throw new RuntimeException("Debe indicar los medida vendibles de la parte completa.");
        }

        if (costoPorMedidaVendible == null || costoPorMedidaVendible.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El costo por medida vendible debe ser mayor a 0.");
        }

        BigDecimal medidaVendibleDecimal = BigDecimal.valueOf(medidaVendible);

        BigDecimal mermaTecnica = medidaReal.subtract(medidaVendibleDecimal);

        if (mermaTecnica.compareTo(BigDecimal.ZERO) < 0) {
            mermaTecnica = BigDecimal.ZERO;
        }

        BigDecimal equivalente = medidaVendibleDecimal.divide(
                BigDecimal.valueOf(medidaVendibleBase),
                6,
                RoundingMode.HALF_UP
        );

        BigDecimal costoDisponible = medidaVendibleDecimal.multiply(costoPorMedidaVendible)
                .setScale(2, RoundingMode.HALF_UP);

        InventarioParte parte = new InventarioParte();
        parte.setIdProducto(idProducto);
        parte.setIdSede(idSede);
        parte.setTipoParte("RETAZO");
        parte.setDescripcion("Retazo disponible");
        parte.setTipoMedida("AREA");
        parte.setUnidadMedida("PIE2");


        parte.setMedidaDisponible(medidaVendible);

        parte.setEquivalenteUnidadFisica(equivalente);

        parte.setCostoTotalOriginal(costoDisponible);
        parte.setCostoTotalDisponible(costoDisponible);
        parte.setCostoPorMedida(costoPorMedidaVendible);

        parte.setEstado("DISPONIBLE");

        parteDao.registrar(parte);

        stockDao.recalcularStockDesdePartes(idProducto, idSede);
        stockDao.recalcularValorDesdePartes(idProducto, idSede);
    }

    private Integer obtenerMedidaBaseDesdePartes(Integer idProducto, Integer idSede) {

        InventarioParte loteCompleto = parteDao.buscarLoteCompletoDisponible(idProducto, idSede)
                .orElse(null);

        if (loteCompleto != null
                && loteCompleto.getMedidaPorParte() != null
                && loteCompleto.getMedidaPorParte() > 0) {
            return loteCompleto.getMedidaPorParte();
        }

        List<InventarioParte> partes = parteDao.listarDisponiblesPorProductoYSede(idProducto, idSede);

        return partes.stream()
                .filter(p -> p.getMedidaPorParte() != null && p.getMedidaPorParte() > 0)
                .map(InventarioParte::getMedidaPorParte)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró la medida vendible base de la parte para este producto."
                ));
    }

    private String obtenerTipoMedidaDesdePartes(Integer idProducto, Integer idSede) {

        List<InventarioParte> partes = parteDao.listarDisponiblesPorProductoYSede(idProducto, idSede);

        return partes.stream()
                .map(InventarioParte::getTipoMedida)
                .filter(t -> t != null && !t.trim().isEmpty())
                .findFirst()
                .orElse("AREA");
    }

    @Transactional
    public void descontarParteCompletaPorPartes(Integer idProducto,
                                                Integer idSede,
                                                BigDecimal cantidadPartes,
                                                String documentoReferencia,
                                                Integer idUsuario) {

        if (idProducto == null || idProducto <= 0) {
            throw new RuntimeException("Producto inválido.");
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("Sede inválida.");
        }

        if (cantidadPartes == null || cantidadPartes.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad de partes a vender debe ser mayor a 0.");
        }

        BigDecimal cantidadSinDecimales = cantidadPartes.stripTrailingZeros();

        if (cantidadSinDecimales.scale() > 0) {
            throw new RuntimeException("La venta de parte completa debe ser en cantidad entera.");
        }

        int cantidad = cantidadPartes.intValueExact();

        InventarioParte lote = parteDao.buscarLoteCompletoDisponible(idProducto, idSede)
                .orElseThrow(() -> new RuntimeException("No hay lote de partes completas disponible."));

        int cantidadDisponible = lote.getCantidadPartes() == null ? 0 : lote.getCantidadPartes();

        if (cantidadDisponible < cantidad) {
            throw new RuntimeException("Stock insuficiente de partes completas disponibles.");
        }

        BigDecimal costoUnitario = lote.getCostoUnitarioParte() != null
                ? lote.getCostoUnitarioParte()
                : lote.getCostoTotalDisponible().divide(BigDecimal.valueOf(cantidadDisponible), 2, RoundingMode.HALF_UP);

        int nuevaCantidad = cantidadDisponible - cantidad;

        int medidaPorParte = lote.getMedidaPorParte() != null && lote.getMedidaPorParte() > 0
                ? lote.getMedidaPorParte()
                : obtenerMedidaBaseDesdePartes(idProducto, idSede);

        int nuevaMedidaDisponible = nuevaCantidad * medidaPorParte;

        BigDecimal nuevoCostoTotal = costoUnitario
                .multiply(BigDecimal.valueOf(nuevaCantidad))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal costoTotalSalida = costoUnitario
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(2, RoundingMode.HALF_UP);

        String nuevoEstado = nuevaCantidad > 0 ? "DISPONIBLE" : "AGOTADA";

        parteDao.actualizarLoteParte(
                lote.getIdParte(),
                nuevaCantidad,
                nuevaMedidaDisponible,
                BigDecimal.valueOf(nuevaCantidad).setScale(6, RoundingMode.HALF_UP),
                nuevoCostoTotal,
                costoUnitario,
                nuevoEstado
        );

        stockDao.recalcularStockDesdePartes(idProducto, idSede);
        stockDao.recalcularValorDesdePartes(idProducto, idSede);

        InventarioStock stockActualizado = stockDao.buscarPorProductoYSede(idProducto, idSede)
                .orElse(null);

        BigDecimal stockResultante = stockActualizado != null && stockActualizado.getStockActual() != null
                ? stockActualizado.getStockActual()
                : BigDecimal.ZERO;

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("SALIDA");
        mov.setOrigenMovimiento("VENTA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(BigDecimal.ZERO);
        mov.setSalida(BigDecimal.valueOf(cantidad));
        mov.setStockResultante(stockResultante);
        mov.setCostoUnitario(costoUnitario);
        mov.setObservacion("Salida automática de parte completa desde lote");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    @Transactional
    public void descontarMediaPartePorPartes(Integer idProducto,
                                             Integer idSede,
                                             BigDecimal cantidadMedias,
                                             String documentoReferencia,
                                             Integer idUsuario) {

        if (idProducto == null || idProducto <= 0) {
            throw new RuntimeException("Producto inválido.");
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("Sede inválida.");
        }

        if (cantidadMedias == null || cantidadMedias.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad de medias partes debe ser mayor a 0.");
        }

        BigDecimal cantidadSinDecimales = cantidadMedias.stripTrailingZeros();

        if (cantidadSinDecimales.scale() > 0) {
            throw new RuntimeException("La venta de media parte debe ser en cantidad entera. Ejemplo: 1, 2, 3 medias.");
        }

        int cantidad = cantidadMedias.intValueExact();

        BigDecimal costoTotalSalida = BigDecimal.ZERO;

        for (int i = 1; i <= cantidad; i++) {

            List<InventarioParte> partesDisponibles =
                    parteDao.listarDisponiblesPorProductoYSede(idProducto, idSede);

            InventarioParte mediaDisponible = partesDisponibles.stream()
                    .filter(p -> "MEDIA".equalsIgnoreCase(p.getTipoParte()))
                    .findFirst()
                    .orElse(null);

            if (mediaDisponible != null) {

                BigDecimal costoDisponible = mediaDisponible.getCostoTotalDisponible() != null
                        ? mediaDisponible.getCostoTotalDisponible()
                        : BigDecimal.ZERO;

                costoTotalSalida = costoTotalSalida.add(costoDisponible);

                parteDao.actualizarDisponibilidadParte(
                        mediaDisponible.getIdParte(),
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "AGOTADA"
                );

                continue;
            }

            InventarioParte completaDisponible = parteDao.buscarLoteCompletoDisponible(idProducto, idSede)
                    .orElse(null);

            if (completaDisponible == null) {
                throw new RuntimeException("Stock insuficiente de medias partes o partes completas disponibles.");
            }

            int cantidadLote = completaDisponible.getCantidadPartes() == null ? 0 : completaDisponible.getCantidadPartes();

            if (cantidadLote <= 0) {
                throw new RuntimeException("No hay partes completas disponibles en el lote.");
            }

            BigDecimal costoCompleta = completaDisponible.getCostoUnitarioParte() != null
                    ? completaDisponible.getCostoUnitarioParte()
                    : completaDisponible.getCostoTotalDisponible()
                    .divide(BigDecimal.valueOf(cantidadLote), 2, RoundingMode.HALF_UP);

            BigDecimal costoMedia = costoCompleta
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

            BigDecimal costoPorMedida = completaDisponible.getCostoPorMedida() != null
                    ? completaDisponible.getCostoPorMedida()
                    : BigDecimal.ZERO;

            Integer medidaCompleta = completaDisponible.getMedidaPorParte() != null
                    ? completaDisponible.getMedidaPorParte()
                    : obtenerMedidaBaseDesdePartes(idProducto, idSede);

            int medidaMedia = medidaCompleta / 2;

            costoTotalSalida = costoTotalSalida.add(costoMedia);

            int nuevaCantidadLote = cantidadLote - 1;
            int nuevaMedidaDisponibleLote = nuevaCantidadLote * medidaCompleta;

            BigDecimal nuevoCostoTotalLote = costoCompleta
                    .multiply(BigDecimal.valueOf(nuevaCantidadLote))
                    .setScale(2, RoundingMode.HALF_UP);

            parteDao.actualizarLoteParte(
                    completaDisponible.getIdParte(),
                    nuevaCantidadLote,
                    nuevaMedidaDisponibleLote,
                    BigDecimal.valueOf(nuevaCantidadLote).setScale(6, RoundingMode.HALF_UP),
                    nuevoCostoTotalLote,
                    costoCompleta,
                    nuevaCantidadLote > 0 ? "DISPONIBLE" : "AGOTADA"
            );
            InventarioParte mediaRestante = new InventarioParte();
            mediaRestante.setIdProducto(idProducto);
            mediaRestante.setIdSede(idSede);

            boolean esLongitud = "LONGITUD".equalsIgnoreCase(completaDisponible.getTipoMedida());

            mediaRestante.setTipoParte("MEDIA");
            mediaRestante.setCantidadPartes(1);

            mediaRestante.setDescripcion(
                    esLongitud
                            ? "Media unidad de longitud disponible"
                            : "Media parte disponible"
            );

            mediaRestante.setTipoMedida(completaDisponible.getTipoMedida());
            mediaRestante.setUnidadMedida(completaDisponible.getUnidadMedida());

            mediaRestante.setMedidaDisponible(medidaMedia);
            mediaRestante.setMedidaPorParte(medidaMedia);

            mediaRestante.setEquivalenteUnidadFisica(new BigDecimal("0.500000"));

            mediaRestante.setCostoTotalOriginal(costoMedia);
            mediaRestante.setCostoTotalDisponible(costoMedia);
            mediaRestante.setCostoUnitarioParte(costoMedia);
            mediaRestante.setCostoPorMedida(costoPorMedida);

            mediaRestante.setEstado("DISPONIBLE");

            parteDao.registrar(mediaRestante);
        }

        stockDao.recalcularStockDesdePartes(idProducto, idSede);
        stockDao.recalcularValorDesdePartes(idProducto, idSede);

        InventarioStock stockActualizado = stockDao.buscarPorProductoYSede(idProducto, idSede)
                .orElse(null);

        BigDecimal stockResultante = stockActualizado != null && stockActualizado.getStockActual() != null
                ? stockActualizado.getStockActual()
                : BigDecimal.ZERO;

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("SALIDA");
        mov.setOrigenMovimiento("VENTA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(BigDecimal.ZERO);
        mov.setSalida(cantidadMedias.multiply(new BigDecimal("0.5")));
        mov.setStockResultante(stockResultante);
        mov.setCostoUnitario(
                cantidad > 0
                        ? costoTotalSalida.divide(BigDecimal.valueOf(cantidad), 6, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO
        );
        mov.setObservacion("Salida automática de media parte por partes");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    @Transactional
    public void descontarMedidaPorPartes(Integer idProducto,
                                         Integer idSede,
                                         BigDecimal cantidadMedida,
                                         String documentoReferencia,
                                         Integer idUsuario) {

        if (idProducto == null || idProducto <= 0) {
            throw new RuntimeException("Producto inválido.");
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("Sede inválida.");
        }

        if (cantidadMedida == null || cantidadMedida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad de medida debe ser mayor a 0.");
        }

        BigDecimal cantidadSinDecimales = cantidadMedida.stripTrailingZeros();

        if (cantidadSinDecimales.scale() > 0) {
            throw new RuntimeException("La venta por medida debe ser en cantidad entera. Ejemplo: 1, 5, 10 o 25 medida.");
        }

        int medidaSolicitada = cantidadMedida.intValueExact();

        List<InventarioParte> partesDisponibles =
                parteDao.listarDisponiblesPorProductoYSede(idProducto, idSede);

        int medidaDisponibleTotal = partesDisponibles.stream()
                .map(InventarioParte::getMedidaDisponible)
                .filter(v -> v != null && v > 0)
                .reduce(0, Integer::sum);

        if (medidaDisponibleTotal < medidaSolicitada) {
            throw new RuntimeException(
                    "Stock insuficiente por partes. Disponible: "
                            + medidaDisponibleTotal + " medida, requerido: "
                            + medidaSolicitada + " medida."
            );
        }

        int restantePorDescontar = medidaSolicitada;
        BigDecimal costoTotalSalida = BigDecimal.ZERO;

        for (InventarioParte parte : partesDisponibles) {

            if (restantePorDescontar <= 0) {
                break;
            }

            Integer medidaParte = parte.getMedidaDisponible();

            if (medidaParte == null || medidaParte <= 0) {
                continue;
            }

            boolean esLoteCompleta = "COMPLETA".equalsIgnoreCase(parte.getTipoParte())
                    && parte.getCantidadPartes() != null
                    && parte.getCantidadPartes() > 1;

            BigDecimal costoDisponible = parte.getCostoTotalDisponible() != null
                    ? parte.getCostoTotalDisponible()
                    : BigDecimal.ZERO;

            BigDecimal costoPorMedida = parte.getCostoPorMedida();

            if (costoPorMedida == null || costoPorMedida.compareTo(BigDecimal.ZERO) <= 0) {
                costoPorMedida = costoDisponible.divide(
                        BigDecimal.valueOf(medidaParte),
                        6,
                        RoundingMode.HALF_UP
                );
            }

            if (medidaParte <= restantePorDescontar) {

                costoTotalSalida = costoTotalSalida.add(costoDisponible);

                parteDao.actualizarDisponibilidadParte(
                        parte.getIdParte(),
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "AGOTADA"
                );

                restantePorDescontar -= medidaParte;

            } else {

                int medidaVendida = restantePorDescontar;

                if (esLoteCompleta) {

                    int cantidadLote = parte.getCantidadPartes() == null ? 0 : parte.getCantidadPartes();

                    int medidaPorParte = parte.getMedidaPorParte() != null && parte.getMedidaPorParte() > 0
                            ? parte.getMedidaPorParte()
                            : obtenerMedidaBaseDesdePartes(idProducto, idSede);

                    BigDecimal costoUnitarioParte = parte.getCostoUnitarioParte() != null
                            ? parte.getCostoUnitarioParte()
                            : parte.getCostoTotalDisponible()
                            .divide(BigDecimal.valueOf(cantidadLote), 2, RoundingMode.HALF_UP);

                    BigDecimal costoSalida = costoPorMedida
                            .multiply(BigDecimal.valueOf(medidaVendida))
                            .setScale(2, RoundingMode.HALF_UP);

                    costoTotalSalida = costoTotalSalida.add(costoSalida);

                    int nuevaCantidadLote = cantidadLote - 1;
                    int nuevaMedidaDisponibleLote = nuevaCantidadLote * medidaPorParte;

                    BigDecimal nuevoCostoTotalLote = costoUnitarioParte
                            .multiply(BigDecimal.valueOf(nuevaCantidadLote))
                            .setScale(2, RoundingMode.HALF_UP);

                    parteDao.actualizarLoteParte(
                            parte.getIdParte(),
                            nuevaCantidadLote,
                            nuevaMedidaDisponibleLote,
                            BigDecimal.valueOf(nuevaCantidadLote).setScale(6, RoundingMode.HALF_UP),
                            nuevoCostoTotalLote,
                            costoUnitarioParte,
                            nuevaCantidadLote > 0 ? "DISPONIBLE" : "AGOTADA"
                    );

                    int medidaRestante = medidaPorParte - medidaVendida;

                    if (medidaRestante > 0) {

                        BigDecimal costoRestante = costoPorMedida
                                .multiply(BigDecimal.valueOf(medidaRestante))
                                .setScale(2, RoundingMode.HALF_UP);

                        BigDecimal equivalenteRestante = BigDecimal.valueOf(medidaRestante)
                                .divide(BigDecimal.valueOf(medidaPorParte), 6, RoundingMode.HALF_UP);

                        InventarioParte retazoRestante = new InventarioParte();
                        retazoRestante.setIdProducto(idProducto);
                        retazoRestante.setIdSede(idSede);
                        retazoRestante.setTipoParte("LONGITUD".equalsIgnoreCase(parte.getTipoMedida()) ? "CORTE" : "RETAZO");
                        retazoRestante.setCantidadPartes(1);
                        retazoRestante.setDescripcion("Retazo restante por venta de medida desde lote");
                        retazoRestante.setTipoMedida(parte.getTipoMedida());
                        retazoRestante.setUnidadMedida(parte.getUnidadMedida());


                        retazoRestante.setMedidaDisponible(medidaRestante);
                        retazoRestante.setMedidaPorParte(medidaRestante);

                        retazoRestante.setEquivalenteUnidadFisica(equivalenteRestante);

                        retazoRestante.setCostoTotalOriginal(costoRestante);
                        retazoRestante.setCostoTotalDisponible(costoRestante);
                        retazoRestante.setCostoUnitarioParte(costoRestante);
                        retazoRestante.setCostoPorMedida(costoPorMedida);

                        retazoRestante.setEstado("DISPONIBLE");

                        parteDao.registrar(retazoRestante);
                    }

                } else {

                    int medidaRestante = medidaParte - medidaVendida;

                    BigDecimal costoSalida = costoPorMedida
                            .multiply(BigDecimal.valueOf(medidaVendida))
                            .setScale(2, RoundingMode.HALF_UP);

                    BigDecimal costoRestante = costoPorMedida
                            .multiply(BigDecimal.valueOf(medidaRestante))
                            .setScale(2, RoundingMode.HALF_UP);

                    costoTotalSalida = costoTotalSalida.add(costoSalida);

                    int medidaBaseParte = parte.getMedidaPorParte() != null
                            && parte.getMedidaPorParte() > 0
                            ? parte.getMedidaPorParte()
                            : obtenerMedidaBaseDesdePartes(idProducto, idSede);

                    BigDecimal equivalenteRestante = BigDecimal.valueOf(medidaRestante)
                            .divide(BigDecimal.valueOf(medidaBaseParte), 6, RoundingMode.HALF_UP);

                    String tipoParteRestante = "LONGITUD".equalsIgnoreCase(parte.getTipoMedida()) ? "CORTE" : "RETAZO";

                    parteDao.convertirParteEnCorte(
                            parte.getIdParte(),
                            tipoParteRestante,
                            medidaRestante,
                            medidaRestante,
                            null,
                            null,
                            parte.getIdParteOrigen(),
                            parte.getIdDetalleVentaOrigen(),
                            parte.getNumeroPlanoOrigen(),
                            equivalenteRestante,
                            costoRestante,
                            costoRestante,
                            costoPorMedida,
                            "Parte convertida a " + tipoParteRestante.toLowerCase() + " por venta de medida"
                    );
                }

                restantePorDescontar = 0;
            }
        }

        stockDao.recalcularStockDesdePartes(idProducto, idSede);
        stockDao.recalcularValorDesdePartes(idProducto, idSede);

        InventarioStock stockActualizado = stockDao.buscarPorProductoYSede(idProducto, idSede)
                .orElse(null);

        BigDecimal stockResultante = stockActualizado != null && stockActualizado.getStockActual() != null
                ? stockActualizado.getStockActual()
                : BigDecimal.ZERO;

        Integer medidaBaseParte = obtenerMedidaBaseDesdePartes(idProducto, idSede);

        BigDecimal salidaEquivalenteParte = cantidadMedida
                .divide(BigDecimal.valueOf(medidaBaseParte), 6, RoundingMode.HALF_UP);

        InventarioMovimiento mov = new InventarioMovimiento();
        mov.setIdProducto(idProducto);
        mov.setIdSede(idSede);
        mov.setTipoMovimiento("SALIDA");
        mov.setOrigenMovimiento("VENTA");
        mov.setDocumentoReferencia(documentoReferencia);
        mov.setEntrada(BigDecimal.ZERO);
        mov.setSalida(salidaEquivalenteParte);
        mov.setStockResultante(stockResultante);
        mov.setCostoUnitario(
                costoTotalSalida.divide(cantidadMedida, 6, RoundingMode.HALF_UP)
        );
        mov.setObservacion("Salida automática por venta de medida desde partes reales");
        mov.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(mov);
    }

    /**
     * Consume exactamente las partes físicas indicadas en el plano validado
     * del detalle principal de una venta por medidas.
     *
     * Este método no debe combinarse con descontarMedidaPorPartes(...),
     * descontarMediaPartePorPartes(...) o descontarParteCompletaPorPartes(...)
     * para el mismo grupo, porque produciría un descuento doble.
     */
    @Transactional
    public void consumirPlanoCorteValidado(
            DetalleVenta detalle,
            Integer idSede,
            String documentoReferencia,
            Integer idUsuario
    ) {
        if (detalle == null || detalle.getIdDetalleVenta() <= 0) {
            throw new RuntimeException(
                    "No existe un detalle de venta válido para consumir el plano."
            );
        }

        if (detalle.getIdProducto() <= 0) {
            throw new RuntimeException(
                    "El detalle no tiene un producto válido."
            );
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException(
                    "No existe una sede válida para consumir el plano."
            );
        }

        if (detalle.getPlanoCorteJson() == null
                || detalle.getPlanoCorteJson().isBlank()) {
            throw new RuntimeException(
                    "El detalle de venta no conserva un plano de corte validado."
            );
        }

        CalculoMedidasRespuestaDto.PlanoCorteDto plano =
                leerPlanoCorteValidado(
                        detalle.getPlanoCorteJson()
                );

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> piezasFisicas =
                plano.piezas().stream()
                        .filter(java.util.Objects::nonNull)
                        .filter(pieza ->
                                pieza.idParteInventario() != null
                                        && pieza.idParteInventario() > 0
                        )
                        .toList();

        if (piezasFisicas.isEmpty()) {
            throw new RuntimeException(
                    "El plano validado no tiene partes físicas de inventario asignadas."
            );
        }

        /*
         * El valor previo debe salir de las partes reales y no de
         * inventario_stock. Ese resumen puede estar desfasado respecto a
         * inventario_partes y producir una salida cero aunque el plano sí
         * haya consumido material.
         */
        BigDecimal stockAnterior =
                nvl(stockDao.obtenerStockEquivalenteDesdePartes(
                        detalle.getIdProducto(),
                        idSede
                ));

        Integer medidaBaseCompleta =
                obtenerMedidaBaseDesdePartes(
                        detalle.getIdProducto(),
                        idSede
                );

        BigDecimal costoTotalConsumido =
                BigDecimal.ZERO;

        for (CalculoMedidasRespuestaDto.PiezaFisicaDto pieza
                : piezasFisicas) {

            InventarioParte parte =
                    parteDao.buscarPorIdParaActualizar(
                                    pieza.idParteInventario()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "La pieza física #"
                                                    + pieza.idParteInventario()
                                                    + " ya no existe."
                                    )
                            );

            validarParteContraPlano(
                    parte,
                    pieza,
                    detalle.getIdProducto(),
                    idSede
            );

            BigDecimal costoPieza =
                    obtenerCostoDeUnaPieza(parte);

            BigDecimal costoPorMedida =
                    obtenerCostoPorMedida(
                            parte,
                            costoPieza,
                            medidaBaseCompleta
                    );

            consumirParteOrigenExacta(
                    parte,
                    medidaBaseCompleta
            );

            costoTotalConsumido =
                    costoTotalConsumido.add(costoPieza);

            if ("AREA".equalsIgnoreCase(plano.tipoMedida())) {
                registrarSobrantesAreaDelPlano(
                        detalle,
                        idSede,
                        pieza,
                        parte,
                        medidaBaseCompleta,
                        costoPorMedida
                );
            }
        }

        stockDao.recalcularStockDesdePartes(
                detalle.getIdProducto(),
                idSede
        );

        stockDao.recalcularValorDesdePartes(
                detalle.getIdProducto(),
                idSede
        );

        InventarioStock stockDespues =
                stockDao.buscarPorProductoYSede(
                                detalle.getIdProducto(),
                                idSede
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No se pudo recalcular el stock después de consumir el plano."
                                )
                        );

        BigDecimal stockNuevo =
                nvl(stockDespues.getStockActual());

        BigDecimal salida =
                stockAnterior.subtract(stockNuevo)
                        .max(BigDecimal.ZERO)
                        .setScale(
                                6,
                                RoundingMode.HALF_UP
                        );

        if (salida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "El consumo del plano no produjo una salida válida de inventario."
            );
        }

        InventarioMovimiento movimiento =
                new InventarioMovimiento();

        movimiento.setIdProducto(
                detalle.getIdProducto()
        );

        movimiento.setIdSede(idSede);
        movimiento.setTipoMovimiento("SALIDA");
        movimiento.setOrigenMovimiento("VENTA");
        movimiento.setDocumentoReferencia(
                documentoReferencia
        );
        movimiento.setEntrada(BigDecimal.ZERO);
        movimiento.setSalida(salida);
        movimiento.setStockResultante(stockNuevo);

        movimiento.setCostoUnitario(
                salida.signum() == 0
                        ? BigDecimal.ZERO
                        : costoTotalConsumido.divide(
                        salida,
                        6,
                        RoundingMode.HALF_UP
                )
        );

        movimiento.setObservacion(
                "Salida por plano de corte validado usando partes físicas exactas"
        );

        movimiento.setIdUsuario(idUsuario);

        movimientoDao.registrarMovimiento(movimiento);
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto leerPlanoCorteValidado(
            String planoCorteJson
    ) {
        try {
            CalculoMedidasRespuestaDto.PlanoCorteDto plano =
                    PLANO_OBJECT_MAPPER.readValue(
                            planoCorteJson,
                            CalculoMedidasRespuestaDto.PlanoCorteDto.class
                    );

            if (plano == null
                    || plano.piezas() == null
                    || plano.piezas().isEmpty()) {
                throw new RuntimeException(
                        "El plano de corte guardado no contiene piezas."
                );
            }

            return plano;

        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(
                    "El plano de corte guardado no tiene un formato válido.",
                    ex
            );
        }
    }

    private void validarParteContraPlano(
            InventarioParte parte,
            CalculoMedidasRespuestaDto.PiezaFisicaDto pieza,
            Integer idProducto,
            Integer idSede
    ) {
        if (!idProducto.equals(parte.getIdProducto())
                || !idSede.equals(parte.getIdSede())) {
            throw new RuntimeException(
                    "La pieza física #"
                            + parte.getIdParte()
                            + " no pertenece al producto o sede de la venta."
            );
        }

        if (parte.getEstado() == null
                || (!"DISPONIBLE".equalsIgnoreCase(parte.getEstado())
                && !"PARCIAL".equalsIgnoreCase(parte.getEstado()))) {
            throw new RuntimeException(
                    "La pieza física #"
                            + parte.getIdParte()
                            + " ya no está disponible."
            );
        }

        int cantidadPartes =
                parte.getCantidadPartes() == null
                        ? 0
                        : parte.getCantidadPartes();

        int medidaDisponible =
                parte.getMedidaDisponible() == null
                        ? 0
                        : parte.getMedidaDisponible();

        if (cantidadPartes <= 0
                && medidaDisponible <= 0) {
            throw new RuntimeException(
                    "La pieza física #"
                            + parte.getIdParte()
                            + " ya no tiene material disponible."
            );
        }

        String tipoEsperado =
                pieza.tipoParteInventario() == null
                        ? ""
                        : pieza.tipoParteInventario()
                        .trim()
                        .toUpperCase();

        String tipoReal =
                parte.getTipoParte() == null
                        ? ""
                        : parte.getTipoParte()
                        .trim()
                        .toUpperCase();

        boolean coincideTipo =
                tipoEsperado.isBlank()
                        || tipoEsperado.equals(tipoReal)
                        || (("RETAZO".equals(tipoEsperado)
                        || "CORTE".equals(tipoEsperado))
                        && ("RETAZO".equals(tipoReal)
                        || "CORTE".equals(tipoReal)));

        if (!coincideTipo) {
            throw new RuntimeException(
                    "La pieza física #"
                            + parte.getIdParte()
                            + " cambió de tipo. Esperado: "
                            + tipoEsperado
                            + ", actual: "
                            + tipoReal
                            + "."
            );
        }

        boolean esRetazo =
                "RETAZO".equals(tipoReal)
                        || "CORTE".equals(tipoReal);

        if (esRetazo) {
            if (parte.getLargoDisponibleMm() == null
                    || parte.getAnchoDisponibleMm() == null
                    || parte.getLargoDisponibleMm().signum() <= 0
                    || parte.getAnchoDisponibleMm().signum() <= 0) {
                throw new RuntimeException(
                        "El retazo #"
                                + parte.getIdParte()
                                + " no tiene dimensiones físicas válidas."
                );
            }

            if (!rectanguloContiene(
                    parte.getLargoDisponibleMm(),
                    parte.getAnchoDisponibleMm(),
                    pieza.ancho(),
                    pieza.alto()
            )) {
                throw new RuntimeException(
                        "El plano ya no cabe dentro del retazo #"
                                + parte.getIdParte()
                                + "."
                );
            }
        }
    }

    private boolean rectanguloContiene(
            BigDecimal largoDisponible,
            BigDecimal anchoDisponible,
            BigDecimal anchoRequerido,
            BigDecimal altoRequerido
    ) {
        if (largoDisponible == null
                || anchoDisponible == null
                || anchoRequerido == null
                || altoRequerido == null) {
            return false;
        }

        boolean normal =
                anchoRequerido.compareTo(largoDisponible) <= 0
                        && altoRequerido.compareTo(anchoDisponible) <= 0;

        boolean rotado =
                altoRequerido.compareTo(largoDisponible) <= 0
                        && anchoRequerido.compareTo(anchoDisponible) <= 0;

        return normal || rotado;
    }

    private BigDecimal obtenerCostoDeUnaPieza(
            InventarioParte parte
    ) {
        int cantidad =
                parte.getCantidadPartes() == null
                        ? 1
                        : Math.max(1, parte.getCantidadPartes());

        if ("COMPLETA".equalsIgnoreCase(parte.getTipoParte())
                && cantidad > 1) {
            if (parte.getCostoUnitarioParte() != null
                    && parte.getCostoUnitarioParte().signum() >= 0) {
                return parte.getCostoUnitarioParte();
            }

            return nvl(parte.getCostoTotalDisponible())
                    .divide(
                            BigDecimal.valueOf(cantidad),
                            6,
                            RoundingMode.HALF_UP
                    );
        }

        return nvl(parte.getCostoTotalDisponible());
    }

    private BigDecimal obtenerCostoPorMedida(
            InventarioParte parte,
            BigDecimal costoPieza,
            Integer medidaBaseCompleta
    ) {
        int medidaReferencia;

        if ("RETAZO".equalsIgnoreCase(parte.getTipoParte())
                || "CORTE".equalsIgnoreCase(parte.getTipoParte())) {
            medidaReferencia =
                    parte.getMedidaDisponible() != null
                            && parte.getMedidaDisponible() > 0
                            ? parte.getMedidaDisponible()
                            : 0;
        } else {
            medidaReferencia =
                    parte.getMedidaPorParte() != null
                            && parte.getMedidaPorParte() > 0
                            ? parte.getMedidaPorParte()
                            : (parte.getMedidaDisponible() != null
                            && parte.getMedidaDisponible() > 0
                            ? parte.getMedidaDisponible()
                            : medidaBaseCompleta);
        }

        if (medidaReferencia <= 0) {
            throw new RuntimeException(
                    "No se pudo calcular el costo por medida de la pieza #"
                            + parte.getIdParte()
                            + "."
            );
        }

        /*
         * Se recalcula desde el costo real de la pieza que será consumida.
         * No se reutiliza costoPorMedida porque los registros históricos
         * pueden conservar un valor perteneciente a otra presentación.
         */
        return costoPieza.divide(
                BigDecimal.valueOf(medidaReferencia),
                6,
                RoundingMode.HALF_UP
        );
    }

    private void consumirParteOrigenExacta(
            InventarioParte parte,
            Integer medidaBaseCompleta
    ) {
        boolean esLoteCompleto =
                "COMPLETA".equalsIgnoreCase(parte.getTipoParte())
                        && parte.getCantidadPartes() != null
                        && parte.getCantidadPartes() > 1;

        if (esLoteCompleto) {
            int cantidadActual =
                    parte.getCantidadPartes();

            int nuevaCantidad =
                    cantidadActual - 1;

            int medidaPorParte =
                    parte.getMedidaPorParte() != null
                            && parte.getMedidaPorParte() > 0
                            ? parte.getMedidaPorParte()
                            : medidaBaseCompleta;

            BigDecimal costoUnitario =
                    obtenerCostoDeUnaPieza(parte);

            int nuevaMedida =
                    nuevaCantidad * medidaPorParte;

            BigDecimal nuevoCostoTotal =
                    costoUnitario.multiply(
                                    BigDecimal.valueOf(nuevaCantidad)
                            )
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            parteDao.actualizarLoteParte(
                    parte.getIdParte(),
                    nuevaCantidad,
                    nuevaMedida,
                    BigDecimal.valueOf(nuevaCantidad)
                            .setScale(
                                    6,
                                    RoundingMode.HALF_UP
                            ),
                    nuevoCostoTotal,
                    costoUnitario,
                    nuevaCantidad > 0
                            ? "DISPONIBLE"
                            : "AGOTADA"
            );

            return;
        }

        parteDao.actualizarDisponibilidadParte(
                parte.getIdParte(),
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "AGOTADA"
        );
    }


    private boolean esSobrantePagadoPorCliente(
            CalculoMedidasRespuestaDto.SobranteDto sobrante
    ) {
        if (sobrante == null || sobrante.clasificacion() == null) {
            return false;
        }

        String clasificacion = sobrante.clasificacion()
                .trim()
                .toUpperCase(Locale.ROOT);

        return clasificacion.startsWith("CLIENTE_")
                || clasificacion.equals("ENTREGAR_CLIENTE")
                || clasificacion.equals("CLIENTE_PAGADO");
    }


    private boolean esSobranteParaInventarioDentIA(
            CalculoMedidasRespuestaDto.SobranteDto sobrante
    ) {
        if (sobrante == null || sobrante.clasificacion() == null) {
            return false;
        }
        String clasificacion = sobrante.clasificacion().trim().toUpperCase(Locale.ROOT);
        return clasificacion.equals("DentIA_INVENTARIO")
                || clasificacion.equals("DentIA_NO_VENDIDO");
    }

    private void registrarSobrantesAreaDelPlano(
            DetalleVenta detalle,
            Integer idSede,
            CalculoMedidasRespuestaDto.PiezaFisicaDto pieza,
            InventarioParte parteOrigen,
            Integer medidaBaseCompleta,
            BigDecimal costoPorMedida
    ) {
        if (pieza.sobrantes() == null
                || pieza.sobrantes().isEmpty()) {
            return;
        }

        for (CalculoMedidasRespuestaDto.SobranteDto sobrante
                : pieza.sobrantes()) {

            if (sobrante == null
                    || sobrante.entregarCliente()
                    || esSobrantePagadoPorCliente(sobrante)
                    || !esSobranteParaInventarioDentIA(sobrante)
                    || sobrante.ancho() == null
                    || sobrante.alto() == null
                    || sobrante.ancho().signum() <= 0
                    || sobrante.alto().signum() <= 0
                    || sobrante.medida() == null
                    || sobrante.medida().signum() <= 0) {
                continue;
            }

            BigDecimal medidaFisica = convertirAreaM2AMedidaFisica(
                    sobrante.medida(),
                    parteOrigen.getUnidadMedida()
            );

            // Regla DentIA: los sobrantes menores de 0.50 Pie² son merma.
            if (medidaFisica.compareTo(new BigDecimal("0.500000")) < 0) {
                continue;
            }

            int medidaVendible = medidaFisica.setScale(0, RoundingMode.FLOOR).intValue();

            BigDecimal equivalente =
                    BigDecimal.valueOf(medidaVendible)
                            .divide(
                                    BigDecimal.valueOf(
                                            medidaBaseCompleta
                                    ),
                                    6,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal costoDisponible =
                    costoPorMedida.multiply(
                                    BigDecimal.valueOf(
                                            medidaVendible
                                    )
                            )
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            InventarioParte retazo =
                    new InventarioParte();

            retazo.setIdProducto(
                    detalle.getIdProducto()
            );

            retazo.setIdSede(idSede);
            retazo.setTipoParte("RETAZO");
            retazo.setCantidadPartes(1);

            retazo.setDescripcion(
                    "Retazo generado por venta "
                            + detalle.getIdVenta()
                            + ", plano "
                            + pieza.numero()
                            + ", sobrante "
                            + sobrante.numero()
            );

            retazo.setTipoMedida("AREA");

            retazo.setUnidadMedida(
                    parteOrigen.getUnidadMedida() == null
                            || parteOrigen.getUnidadMedida().isBlank()
                            ? "PIE2"
                            : parteOrigen.getUnidadMedida()
            );

            retazo.setMedidaDisponible(
                    medidaVendible
            );

            /*
             * Cada retazo es una pieza física individual; por ello su medida
             * por parte debe coincidir con su propia medida disponible.
             * La equivalencia contra la plancha completa se conserva aparte.
             */
            retazo.setMedidaPorParte(
                    medidaVendible
            );
            retazo.setMedidaFisicaDisponible(medidaFisica);

            retazo.setLargoDisponibleMm(
                    sobrante.ancho()
            );

            retazo.setAnchoDisponibleMm(
                    sobrante.alto()
            );

            retazo.setIdParteOrigen(
                    parteOrigen.getIdParte()
            );

            retazo.setIdDetalleVentaOrigen(
                    detalle.getIdDetalleVenta()
            );

            retazo.setNumeroPlanoOrigen(
                    pieza.numero()
            );

            retazo.setEquivalenteUnidadFisica(
                    equivalente
            );

            retazo.setCostoTotalOriginal(
                    costoDisponible
            );

            retazo.setCostoTotalDisponible(
                    costoDisponible
            );

            retazo.setCostoUnitarioParte(
                    costoDisponible
            );

            retazo.setCostoPorMedida(
                    costoPorMedida
            );

            retazo.setEstado("DISPONIBLE");

            parteDao.registrar(retazo);
        }
    }

    private BigDecimal convertirAreaM2AMedidaFisica(
            BigDecimal areaM2,
            String unidadMedida
    ) {
        String unidad = unidadMedida == null
                ? "PIE2"
                : unidadMedida.trim().toUpperCase(Locale.ROOT);

        BigDecimal medida;
        if ("PIE2".equals(unidad) || "PIE_2".equals(unidad) || "PIE²".equals(unidad)) {
            medida = areaM2.multiply(METROS_CUADRADOS_A_PIE2);
        } else if ("M2".equals(unidad) || "METRO_CUADRADO".equals(unidad) || "AREA".equals(unidad)) {
            medida = areaM2;
        } else {
            throw new RuntimeException("La unidad de medida " + unidad
                    + " no es compatible con retazos de área.");
        }
        return medida.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal obtenerMedidaFisicaMostrable(InventarioParte parte) {
        if (parte == null) {
            return BigDecimal.ZERO;
        }
        if (parte.getMedidaFisicaDisponible() != null
                && parte.getMedidaFisicaDisponible().signum() >= 0) {
            return parte.getMedidaFisicaDisponible();
        }
        return parte.getMedidaDisponible() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(Math.max(0, parte.getMedidaDisponible()));
    }

    /**
     * Revierte el inventario de una venta anulada antes de que exista
     * preparación física. Para planos validados restaura las partes origen
     * y deja sin disponibilidad los retazos creados por esa venta.
     */
    @Transactional
    public void revertirDetalleVentaPorAnulacion(
            DetalleVenta detalle,
            Integer idSede,
            String documentoReferencia,
            Integer idUsuario
    ) {
        if (detalle == null || detalle.getIdProducto() <= 0) {
            throw new RuntimeException("Detalle de venta inválido para devolver inventario.");
        }
        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("Sede inválida para devolver inventario.");
        }

        BigDecimal stockAnterior = nvl(stockDao.buscarPorProductoYSede(
                detalle.getIdProducto(), idSede
        ).map(InventarioStock::getStockActual).orElse(BigDecimal.ZERO));

        boolean inventarioPorPartes;
        if (detalle.getPlanoCorteJson() != null
                && !detalle.getPlanoCorteJson().isBlank()) {
            inventarioPorPartes = true;
            revertirPlanoCortePorAnulacion(detalle, idSede);
        } else if (tienePartesDisponibles(detalle.getIdProducto(), idSede)) {
            inventarioPorPartes = true;
            devolverParteCompletaPorAnulacion(
                    detalle.getIdProducto(), idSede,
                    cantidadStockDetalle(detalle)
            );
        } else {
            inventarioPorPartes = false;
            devolverStockSimplePorAnulacion(
                    detalle.getIdProducto(), idSede,
                    cantidadStockDetalle(detalle)
            );
        }

        if (inventarioPorPartes) {
            stockDao.recalcularStockDesdePartes(detalle.getIdProducto(), idSede);
            stockDao.recalcularValorDesdePartes(detalle.getIdProducto(), idSede);
        }

        BigDecimal stockNuevo = nvl(stockDao.buscarPorProductoYSede(
                detalle.getIdProducto(), idSede
        ).map(InventarioStock::getStockActual).orElse(BigDecimal.ZERO));

        BigDecimal entrada = stockNuevo.subtract(stockAnterior)
                .max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);

        if (entrada.signum() > 0) {
            InventarioMovimiento movimiento = new InventarioMovimiento();
            movimiento.setIdProducto(detalle.getIdProducto());
            movimiento.setIdSede(idSede);
            movimiento.setTipoMovimiento("ENTRADA");
            movimiento.setOrigenMovimiento("ANULACION_VENTA");
            movimiento.setDocumentoReferencia(documentoReferencia);
            movimiento.setEntrada(entrada);
            movimiento.setSalida(BigDecimal.ZERO);
            movimiento.setStockResultante(stockNuevo);
            movimiento.setCostoUnitario(null);
            movimiento.setObservacion("Devolución automática por anulación de venta");
            movimiento.setIdUsuario(idUsuario);
            movimientoDao.registrarMovimiento(movimiento);
        }
    }

    private BigDecimal cantidadStockDetalle(DetalleVenta detalle) {
        if (detalle.getCantidadDescontadaStock() != null
                && detalle.getCantidadDescontadaStock().signum() > 0) {
            return detalle.getCantidadDescontadaStock();
        }
        BigDecimal cantidad = detalle.getCantidad() == null
                ? BigDecimal.ZERO : detalle.getCantidad();
        BigDecimal factor = detalle.getFactorConversionStock() == null
                ? BigDecimal.ONE : detalle.getFactorConversionStock();
        return cantidad.multiply(factor).setScale(6, RoundingMode.HALF_UP);
    }

    private void devolverStockSimplePorAnulacion(
            Integer idProducto, Integer idSede, BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new RuntimeException("La cantidad a devolver debe ser mayor que cero.");
        }
        InventarioStock stock = stockDao.buscarPorProductoYSede(idProducto, idSede)
                .orElseThrow(() -> new RuntimeException(
                        "No existe stock del producto para devolver la anulación."));
        BigDecimal actual = nvl(stock.getStockActual());
        stockDao.actualizarStock(idProducto, idSede, actual.add(cantidad));
    }

    private void devolverParteCompletaPorAnulacion(
            Integer idProducto, Integer idSede, BigDecimal cantidadPartes) {
        BigDecimal limpia = cantidadPartes == null
                ? BigDecimal.ZERO : cantidadPartes.stripTrailingZeros();
        if (limpia.signum() <= 0 || limpia.scale() > 0) {
            throw new RuntimeException(
                    "La devolución de una presentación completa requiere una cantidad entera.");
        }
        int cantidad = limpia.intValueExact();
        InventarioParte lote = parteDao.buscarLoteCompletoPorProductoYSede(idProducto, idSede)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró el lote físico original para devolver la venta."));
        int medidaPorParte = lote.getMedidaPorParte() == null
                ? obtenerMedidaBaseDesdePartes(idProducto, idSede)
                : lote.getMedidaPorParte();
        BigDecimal costoUnitario = lote.getCostoUnitarioParte() != null
                ? lote.getCostoUnitarioParte()
                : nvl(lote.getCostoTotalOriginal());
        BigDecimal equivalente = BigDecimal.valueOf(cantidad).setScale(6, RoundingMode.HALF_UP);
        parteDao.incrementarLoteParte(
                lote.getIdParte(), cantidad, cantidad * medidaPorParte,
                equivalente, costoUnitario.multiply(BigDecimal.valueOf(cantidad)),
                "DISPONIBLE"
        );
    }

    private void revertirPlanoCortePorAnulacion(
            DetalleVenta detalle, Integer idSede) {
        CalculoMedidasRespuestaDto.PlanoCorteDto plano =
                leerPlanoCorteValidado(detalle.getPlanoCorteJson());

        List<InventarioParte> generadas =
                parteDao.listarGeneradasPorDetalleVenta(detalle.getIdDetalleVenta());
        for (InventarioParte generada : generadas) {
            parteDao.actualizarDisponibilidadParte(
                    generada.getIdParte(), 0, BigDecimal.ZERO,
                    BigDecimal.ZERO, "ANULADA"
            );
        }

        java.util.Set<Integer> restauradas = new java.util.HashSet<>();
        for (CalculoMedidasRespuestaDto.PiezaFisicaDto pieza : plano.piezas()) {
            if (pieza == null || pieza.idParteInventario() == null
                    || pieza.idParteInventario() <= 0) {
                continue;
            }

            InventarioParte origen = parteDao.buscarPorIdParaActualizar(
                    pieza.idParteInventario()
            ).orElseThrow(() -> new RuntimeException(
                    "No existe la pieza física original #" + pieza.idParteInventario() + "."));

            if (detalle.getIdProducto() != origen.getIdProducto()
                    || !idSede.equals(origen.getIdSede())) {
                throw new RuntimeException(
                        "La pieza física original no pertenece a la venta anulada.");
            }

            String tipo = origen.getTipoParte() == null
                    ? "" : origen.getTipoParte().trim().toUpperCase(Locale.ROOT);

            if ("COMPLETA".equals(tipo)) {
                int medida = origen.getMedidaPorParte() == null
                        ? obtenerMedidaBaseDesdePartes(detalle.getIdProducto(), idSede)
                        : origen.getMedidaPorParte();
                BigDecimal costo = origen.getCostoUnitarioParte() != null
                        ? origen.getCostoUnitarioParte()
                        : nvl(origen.getCostoTotalOriginal());
                parteDao.incrementarLoteParte(
                        origen.getIdParte(), 1, medida,
                        BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP),
                        costo, "DISPONIBLE"
                );
            } else {
                if (!restauradas.add(origen.getIdParte())) {
                    continue;
                }
                BigDecimal medidaFisicaOriginal =
                        origen.getMedidaFisicaDisponible() != null
                                && origen.getMedidaFisicaDisponible().signum() > 0
                                ? origen.getMedidaFisicaDisponible()
                                : BigDecimal.valueOf(
                                origen.getMedidaPorParte() != null
                                        && origen.getMedidaPorParte() > 0
                                        ? origen.getMedidaPorParte()
                                        : Math.max(0, origen.getMedidaDisponible() == null
                                        ? 0 : origen.getMedidaDisponible())
                        );

                int medidaVendible = origen.getMedidaPorParte() != null
                        && origen.getMedidaPorParte() > 0
                        ? origen.getMedidaPorParte()
                        : medidaFisicaOriginal
                        .setScale(0, RoundingMode.FLOOR)
                        .intValue();

                Integer base = obtenerMedidaBaseDesdePartes(
                        detalle.getIdProducto(), idSede);

                BigDecimal equivalente = base <= 0
                        ? BigDecimal.ZERO
                        : medidaFisicaOriginal.divide(
                        BigDecimal.valueOf(base),
                        6,
                        RoundingMode.HALF_UP
                );

                BigDecimal costo = nvl(origen.getCostoTotalOriginal());

                parteDao.restaurarParteIndividual(
                        origen.getIdParte(),
                        medidaVendible,
                        medidaFisicaOriginal,
                        equivalente,
                        costo,
                        "DISPONIBLE"
                );
            }
        }
    }

    public String generarInstruccionMaterialParaDetalle(DetalleVenta detalle, Integer idSede) {

        if (detalle == null) {
            return "No se pudo calcular el material sugerido.";
        }

        if (detalle.getIdProducto() <= 0 || idSede == null || idSede <= 0) {
            return "No se pudo calcular el material sugerido porque falta producto o sede.";
        }

        BigDecimal cantidadVendida = detalle.getCantidad() == null
                ? BigDecimal.ZERO
                : detalle.getCantidad();

        if (cantidadVendida.compareTo(BigDecimal.ZERO) <= 0) {
            return "Cantidad inválida para calcular material.";
        }

        int medidaNecesaria = cantidadVendida
                .setScale(0, RoundingMode.CEILING)
                .intValue();

        List<InventarioParte> partes = parteDao.listarDisponiblesPorProductoYSede(
                detalle.getIdProducto(),
                idSede
        );

        if (partes == null || partes.isEmpty()) {
            return "No hay piezas disponibles registradas para este producto en la sede activa.";
        }

        List<InventarioParte> candidatas = partes.stream()
                .filter(p -> p.getEstado() != null && "DISPONIBLE".equalsIgnoreCase(p.getEstado()))
                .filter(p -> p.getMedidaDisponible() != null && p.getMedidaDisponible() > 0)
                .filter(p -> p.getMedidaDisponible() >= medidaNecesaria)
                .sorted((a, b) -> {

                    int prioridadA = prioridadTipoParte(a.getTipoParte());
                    int prioridadB = prioridadTipoParte(b.getTipoParte());

                    if (prioridadA != prioridadB) {
                        return Integer.compare(prioridadA, prioridadB);
                    }

                    return Integer.compare(a.getMedidaDisponible(), b.getMedidaDisponible());
                })
                .toList();

        if (candidatas.isEmpty()) {

            int totalDisponible = partes.stream()
                    .filter(p -> p.getEstado() != null && "DISPONIBLE".equalsIgnoreCase(p.getEstado()))
                    .map(InventarioParte::getMedidaDisponible)
                    .filter(v -> v != null && v > 0)
                    .reduce(0, Integer::sum);

            String unidadTotal = partes.stream()
                    .map(InventarioParte::getUnidadMedida)
                    .filter(u -> u != null && !u.isBlank())
                    .findFirst()
                    .orElse("medida");

            return "No hay una sola pieza suficiente. Disponible total: "
                    + totalDisponible
                    + " "
                    + unidadTotal
                    + ". Requiere: "
                    + medidaNecesaria
                    + " "
                    + unidadTotal
                    + ". Revisar combinación manual.";
        }

        InventarioParte parteSugerida = candidatas.get(0);

        String tipoParte = normalizarNombreTipoParte(parteSugerida.getTipoParte());

        String unidad = parteSugerida.getUnidadMedida() == null
                ? "medida"
                : parteSugerida.getUnidadMedida();

        String descripcion = parteSugerida.getDescripcion() == null || parteSugerida.getDescripcion().isBlank()
                ? ""
                : " - " + parteSugerida.getDescripcion();

        return "Coger "
                + tipoParte
                + " #"
                + parteSugerida.getIdParte()
                + descripcion
                + ". Disponible: "
                + parteSugerida.getMedidaDisponible()
                + " "
                + unidad
                + ". Requiere: "
                + medidaNecesaria
                + " "
                + unidad
                + ".";
    }
    private int prioridadTipoParte(String tipoParte) {

        if (tipoParte == null) {
            return 99;
        }

        String tipo = tipoParte.trim().toUpperCase();

        return switch (tipo) {
            case "RETAZO", "CORTE" -> 1;
            case "MEDIA" -> 2;
            case "COMPLETA" -> 3;
            default -> 99;
        };
    }

    private String normalizarNombreTipoParte(String tipoParte) {

        if (tipoParte == null || tipoParte.isBlank()) {
            return "PIEZA";
        }

        String tipo = tipoParte.trim().toUpperCase();

        return switch (tipo) {
            case "RETAZO" -> "RETAZO";
            case "CORTE" -> "CORTE";
            case "MEDIA" -> "MEDIA PLANCHA";
            case "COMPLETA" -> "PLANCHA COMPLETA";
            default -> tipo;
        };
    }

    public record DisponibilidadFisicaProducto(
            int completasDisponibles,
            int mediasDisponibles,
            int retazosDisponibles,
            int medidaVendibleTotal,
            int medidaEnCompletas,
            int medidaEnMedias,
            int medidaEnRetazos,
            BigDecimal stockEquivalenteDisponible,
            String tipoMedida,
            String unidadMedida,
            List<InventarioParte> partesDisponibles
    ) {
        public boolean tieneCompletas(int cantidad) {
            return cantidad > 0 && completasDisponibles >= cantidad;
        }

        public boolean puedeObtenerMedias(int cantidad) {
            if (cantidad <= 0) {
                return false;
            }

            /*
             * Cada completa puede originar dos medias.
             * Las medias existentes se usan primero.
             */
            int mediasObtenibles =
                    mediasDisponibles + (completasDisponibles * 2);

            return mediasObtenibles >= cantidad;
        }

        public boolean tieneMedidaVendible(int cantidad) {
            return cantidad > 0 && medidaVendibleTotal >= cantidad;
        }

        public boolean tienePartes() {
            return completasDisponibles > 0
                    || mediasDisponibles > 0
                    || retazosDisponibles > 0;
        }
    }
}