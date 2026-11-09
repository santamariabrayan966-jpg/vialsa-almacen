package com.dentia.service.interno;

import com.dentia.dao.interfaces.IProductoDao;
import com.dentia.model.interno.Producto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
public class ProductoService {

    private final IProductoDao productoDao;

    public ProductoService(IProductoDao productoDao) {
        this.productoDao = productoDao;
    }

    // ============================================================
    // LISTADOS
    // ============================================================

    public List<Producto> listar() {
        return productoDao.listar();
    }

    public List<Producto> listarActivos() {
        return productoDao.listarActivos();
    }

    public List<Producto> listarActivosConStockPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return productoDao.listarActivos();
        }

        return productoDao.listarActivosConStockPorSede(idSede);
    }
    public List<Producto> listarActivosConStockEnSedes() {
        return productoDao.listarActivosConStockEnSedes();
    }

    public List<Map<String, Object>> obtenerDisponibilidadPorProducto(Integer idProducto) {
        if (idProducto == null || idProducto <= 0) {
            return List.of();
        }

        return productoDao.obtenerDisponibilidadPorProducto(idProducto);
    }

    public List<Producto> listarEliminados() {
        return productoDao.listarEliminados();
    }

    public List<Producto> buscar(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return productoDao.listar();
        }

        return productoDao.buscar(filtro.trim());
    }

    public List<Producto> buscarVendiblesPorSede(String filtro, Integer idSede) {

        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        String texto = filtro == null ? "" : filtro.trim();

        return productoDao.buscarVendiblesPorSede(texto, idSede);
    }

    // ============================================================
    // GUARDAR PRODUCTO
    // ============================================================

    public boolean guardar(Producto producto) {

        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo.");
        }

        validarDatosBase(producto);
        normalizarDatos(producto);
        validarPrecioVenta(producto);

        if (producto.getIdProducto() == null || producto.getIdProducto() == 0) {
            prepararProductoNuevo(producto);
            return productoDao.crear(producto) > 0;
        }

        prepararProductoExistente(producto);
        return productoDao.actualizar(producto) > 0;
    }

    public Integer guardarYRetornarId(Producto producto) {

        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo.");
        }

        validarDatosBase(producto);
        normalizarDatos(producto);
        validarPrecioVenta(producto);

        if (producto.getIdProducto() == null || producto.getIdProducto() == 0) {
            prepararProductoNuevo(producto);
            return productoDao.crearYRetornarId(producto);
        }

        prepararProductoExistente(producto);
        productoDao.actualizar(producto);

        return producto.getIdProducto();
    }

    // ============================================================
    // VALIDACIONES BASE
    // ============================================================

    private void validarDatosBase(Producto producto) {

        if (producto.getNombreProducto() == null || producto.getNombreProducto().isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        }

        if (producto.getPrecioUnitario() == null) {
            throw new IllegalArgumentException("El precio principal es obligatorio.");
        }

        if (producto.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio principal no puede ser negativo.");
        }

        if (producto.getIdCategoria() == null) {
            throw new IllegalArgumentException("Debe seleccionar una categoría.");
        }
    }

    // ============================================================
    // NORMALIZACIÓN GENERAL
    // ============================================================

    private void normalizarDatos(Producto producto) {

        producto.setNombreProducto(producto.getNombreProducto().trim());

        if (producto.getCodigoInterno() != null) {
            producto.setCodigoInterno(producto.getCodigoInterno().trim());
        }

        if (producto.getDimensiones() != null) {
            producto.setDimensiones(producto.getDimensiones().trim());
        }

        if (producto.getModeloProducto() != null) {
            producto.setModeloProducto(producto.getModeloProducto().trim());
        }

        if (producto.getDescripcionProducto() != null) {
            producto.setDescripcionProducto(producto.getDescripcionProducto().trim());
        }

        if (producto.getIdEstadoProducto() == null) {
            producto.setIdEstadoProducto(1);
        }

        if (producto.getDescuentoMaximo() == null) {
            producto.setDescuentoMaximo(BigDecimal.ZERO);
        }

        if (producto.getStockMinimo() == null) {
            producto.setStockMinimo(0);
        }

        if (producto.getStockActual() == null) {
            producto.setStockActual(0);
        }

        normalizarControlInventario(producto);
        normalizarFormaVenta(producto);
    }

    // ============================================================
    // NUEVO FLUJO DE INVENTARIO
    // ============================================================

    private void normalizarControlInventario(Producto producto) {

        /*
         * En DentIA todo producto ingresa al almacén como unidades físicas:
         * plancha, varilla, rollo, bote, caja, unidad, etc.
         * Por eso el control de inventario siempre será CANTIDAD.
         */
        producto.setControlInventario("CANTIDAD");

        if (producto.getNombreUnidadInventario() == null
                || producto.getNombreUnidadInventario().isBlank()) {

            producto.setNombreUnidadInventario(obtenerNombreUnidadInventarioPorDefecto(producto));

        } else {
            producto.setNombreUnidadInventario(producto.getNombreUnidadInventario().trim());
        }

        /*
         * idUnidad queda temporalmente por compatibilidad con vistas/módulos antiguos.
         * El nuevo flujo ya trabaja principalmente con nombreUnidadInventario.
         */
        if (producto.getIdUnidad() == null) {
            producto.setIdUnidad(obtenerUnidadTemporalPorNombre(producto.getNombreUnidadInventario()));
        }
    }

    private String obtenerNombreUnidadInventarioPorDefecto(Producto producto) {

        if (producto.getNombreCategoria() != null) {
            String categoria = normalizarTexto(producto.getNombreCategoria());

            if (categoria.contains("vidrio")) {
                return "Plancha";
            }

            if (categoria.contains("alumin")) {
                return "Varilla";
            }

            if (categoria.contains("accesorio")) {
                return "Unidad";
            }
        }

        return "Unidad";
    }

    private Integer obtenerUnidadTemporalPorNombre(String nombreUnidadInventario) {

        if (nombreUnidadInventario == null) {
            return null;
        }

        String unidad = normalizarTexto(nombreUnidadInventario);

        /*
         * Estos IDs son compatibilidad temporal con tu tabla unidades actual.
         * Si tus IDs son diferentes, luego lo ajustamos o lo eliminamos
         * cuando ya no dependamos de idUnidad.
         */
        if (unidad.contains("plancha")) {
            return 4;
        }

        if (unidad.contains("unidad")) {
            return 2;
        }

        return null;
    }

    // ============================================================
    // NUEVO FLUJO DE VENTA
    // ============================================================

    private void normalizarFormaVenta(Producto producto) {

        /*
         * NUEVO FLUJO:
         * La forma de venta ya NO se define en Nuevo producto.
         * Las presentaciones reales se configuran en el modal de precios.
         *
         * Estos campos quedan por compatibilidad con módulos antiguos.
         */
        producto.setFormaVenta("CANTIDAD");

        if (producto.getNombreUnidadVenta() == null || producto.getNombreUnidadVenta().isBlank()) {
            producto.setNombreUnidadVenta("Unidad");
        } else {
            producto.setNombreUnidadVenta(producto.getNombreUnidadVenta().trim());
        }

        if (producto.getAbreviaturaUnidadVenta() == null || producto.getAbreviaturaUnidadVenta().isBlank()) {
            producto.setAbreviaturaUnidadVenta("und");
        } else {
            producto.setAbreviaturaUnidadVenta(producto.getAbreviaturaUnidadVenta().trim());
        }

        normalizarMedidasFisicas(producto);
    }

    private void normalizarMedidasFisicas(Producto producto) {

        boolean tieneMedidas = Boolean.TRUE.equals(producto.getVentaPorMedida());

        if (!tieneMedidas) {
            producto.setVentaPorMedida(false);
            producto.setTipoMedidaBase(null);
            producto.setLargoBase(null);
            producto.setAnchoBase(null);
            producto.setMedidaTotalBase(null);
            producto.setMedidaVendibleBase(null);
            producto.setDescripcionMedidaBase(null);
            return;
        }

        String tipo = producto.getTipoMedidaBase();

        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de medida física.");
        }

        tipo = tipo.trim().toUpperCase();
        producto.setTipoMedidaBase(tipo);

        if (!tipo.equals("AREA") && !tipo.equals("LONGITUD")) {
            throw new IllegalArgumentException("El tipo de medida física no es válido.");
        }

        if (producto.getLargoBase() == null || producto.getLargoBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debe ingresar el largo real de la unidad física.");
        }

        if (tipo.equals("AREA")) {

            if (producto.getAnchoBase() == null || producto.getAnchoBase().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Debe ingresar el ancho real de la unidad física.");
            }

            BigDecimal medidaReal = producto.getLargoBase()
                    .multiply(producto.getAnchoBase())
                    .setScale(6, RoundingMode.HALF_UP);

            BigDecimal factorPie2 = new BigDecimal("10.7639");

            int medidaVendible = medidaReal
                    .multiply(factorPie2)
                    .setScale(0, RoundingMode.DOWN)
                    .intValue();

            if (medidaVendible <= 0) {
                throw new IllegalArgumentException("La medida vendible debe ser mayor a cero.");
            }

            producto.setMedidaTotalBase(medidaReal);
            producto.setMedidaVendibleBase(medidaVendible);

            producto.setDescripcionMedidaBase(
                    "1 " + producto.getNombreUnidadInventario().toLowerCase()
                            + " equivale a "
                            + medidaVendible
                            + " pie² vendibles."
            );

            return;
        }

        producto.setAnchoBase(null);

        BigDecimal medidaReal = producto.getLargoBase()
                .setScale(6, RoundingMode.HALF_UP);

        int medidaVendible = medidaReal
                .setScale(0, RoundingMode.DOWN)
                .intValue();

        if (medidaVendible <= 0) {
            throw new IllegalArgumentException("La medida vendible debe ser mayor a cero.");
        }

        producto.setMedidaTotalBase(medidaReal);
        producto.setMedidaVendibleBase(medidaVendible);

        producto.setDescripcionMedidaBase(
                "1 " + producto.getNombreUnidadInventario().toLowerCase()
                        + " equivale a "
                        + medidaVendible
                        + " ml vendibles."
        );
    }

    private void normalizarVentaPorCantidad(Producto producto) {

        producto.setVentaPorMedida(false);
        producto.setTipoMedidaBase(null);

        producto.setLargoBase(null);
        producto.setAnchoBase(null);
        producto.setMedidaTotalBase(null);
        producto.setMedidaVendibleBase(null);

        if (producto.getNombreUnidadVenta() == null || producto.getNombreUnidadVenta().isBlank()) {
            producto.setNombreUnidadVenta("Unidad");
        } else {
            producto.setNombreUnidadVenta(producto.getNombreUnidadVenta().trim());
        }

        if (producto.getAbreviaturaUnidadVenta() == null || producto.getAbreviaturaUnidadVenta().isBlank()) {
            producto.setAbreviaturaUnidadVenta("und");
        } else {
            producto.setAbreviaturaUnidadVenta(producto.getAbreviaturaUnidadVenta().trim());
        }

        producto.setDescripcionMedidaBase(
                "1 " + producto.getNombreUnidadInventario().toLowerCase()
                        + " equivale a 1 "
                        + producto.getNombreUnidadVenta().toLowerCase()
                        + " vendible."
        );
    }

    private void normalizarVentaPorArea(Producto producto) {

        producto.setVentaPorMedida(true);
        producto.setTipoMedidaBase("AREA");

        if (producto.getNombreUnidadVenta() == null || producto.getNombreUnidadVenta().isBlank()) {
            producto.setNombreUnidadVenta("Metro cuadrado");
        } else {
            producto.setNombreUnidadVenta(producto.getNombreUnidadVenta().trim());
        }

        if (producto.getAbreviaturaUnidadVenta() == null || producto.getAbreviaturaUnidadVenta().isBlank()) {
            producto.setAbreviaturaUnidadVenta("m²");
        } else {
            producto.setAbreviaturaUnidadVenta(producto.getAbreviaturaUnidadVenta().trim());
        }

        if (producto.getLargoBase() == null || producto.getLargoBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debe ingresar el largo real de la unidad de inventario.");
        }

        if (producto.getAnchoBase() == null || producto.getAnchoBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debe ingresar el ancho real de la unidad de inventario.");
        }

        BigDecimal medidaReal = producto.getLargoBase()
                .multiply(producto.getAnchoBase())
                .setScale(6, RoundingMode.HALF_UP);

        int medidaVendible = medidaReal.setScale(0, RoundingMode.DOWN).intValue();

        if (medidaVendible <= 0) {
            throw new IllegalArgumentException("La medida vendible debe ser mayor a cero.");
        }

        producto.setMedidaTotalBase(medidaReal);
        producto.setMedidaVendibleBase(medidaVendible);

        producto.setDescripcionMedidaBase(
                "1 " + producto.getNombreUnidadInventario().toLowerCase()
                        + " equivale a "
                        + medidaVendible
                        + " "
                        + producto.getAbreviaturaUnidadVenta()
                        + " vendibles."
        );
    }

    private void normalizarVentaPorLongitud(Producto producto) {

        producto.setVentaPorMedida(true);
        producto.setTipoMedidaBase("LONGITUD");

        if (producto.getNombreUnidadVenta() == null || producto.getNombreUnidadVenta().isBlank()) {
            producto.setNombreUnidadVenta("Metro lineal");
        } else {
            producto.setNombreUnidadVenta(producto.getNombreUnidadVenta().trim());
        }

        if (producto.getAbreviaturaUnidadVenta() == null || producto.getAbreviaturaUnidadVenta().isBlank()) {
            producto.setAbreviaturaUnidadVenta("ml");
        } else {
            producto.setAbreviaturaUnidadVenta(producto.getAbreviaturaUnidadVenta().trim());
        }

        if (producto.getLargoBase() == null || producto.getLargoBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debe ingresar el largo real de la unidad de inventario.");
        }

        producto.setAnchoBase(null);

        BigDecimal medidaReal = producto.getLargoBase()
                .setScale(6, RoundingMode.HALF_UP);

        int medidaVendible = medidaReal.setScale(0, RoundingMode.DOWN).intValue();

        if (medidaVendible <= 0) {
            throw new IllegalArgumentException("La medida vendible debe ser mayor a cero.");
        }

        producto.setMedidaTotalBase(medidaReal);
        producto.setMedidaVendibleBase(medidaVendible);

        producto.setDescripcionMedidaBase(
                "1 " + producto.getNombreUnidadInventario().toLowerCase()
                        + " equivale a "
                        + medidaVendible
                        + " "
                        + producto.getAbreviaturaUnidadVenta()
                        + " vendibles."
        );
    }

    private String normalizarTexto(String valor) {
        return valor == null
                ? ""
                : valor.trim().toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u");
    }

    // ============================================================
    // PRECIO
    // ============================================================

    private void validarPrecioVenta(Producto producto) {

        /*
         * NUEVO FLUJO:
         * Productos ya NO administra precios de venta reales.
         * Los precios se configuran en el modal "Presentaciones de venta y precios".
         * Por eso aquí ya no se valida contra el último precio de compra.
         */

        if (producto.getPrecioUnitario() == null) {
            producto.setPrecioUnitario(BigDecimal.ZERO);
        }

        if (producto.getDescuentoMaximo() == null) {
            producto.setDescuentoMaximo(BigDecimal.ZERO);
        }

        if (producto.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio principal no puede ser negativo.");
        }

        if (producto.getDescuentoMaximo().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento máximo no puede ser negativo.");
        }
    }

    // ============================================================
    // NUEVO / EXISTENTE
    // ============================================================

    private void prepararProductoNuevo(Producto producto) {

        producto.setStockActual(0);

        if (producto.getCodigoInterno() == null || producto.getCodigoInterno().isBlank()) {
            producto.setCodigoInterno(
                    productoDao.generarCodigoInterno(producto.getIdCategoria())
            );
        }
    }

    private void prepararProductoExistente(Producto producto) {

        Optional<Producto> optActual = productoDao.buscarPorId(producto.getIdProducto());

        if (optActual.isEmpty()) {
            throw new IllegalArgumentException("El producto a actualizar no existe.");
        }

        Producto actualBD = optActual.get();

        /*
         * Compatibilidad temporal:
         * stockActual todavía se conserva desde productos.
         * Luego debe vivir solo en inventario_stock.
         */
        producto.setStockActual(actualBD.getStockActual());

        if (producto.getFoto() == null || producto.getFoto().isBlank()) {
            producto.setFoto(actualBD.getFoto());
        }

        if (producto.getIdMarca() == null) {
            producto.setIdMarca(actualBD.getIdMarca());
        }

        if (producto.getIdCategoria() == null) {
            producto.setIdCategoria(actualBD.getIdCategoria());
        }

        if (producto.getIdSubcategoria() == null) {
            producto.setIdSubcategoria(actualBD.getIdSubcategoria());
        }

        if (producto.getIdUnidad() == null) {
            producto.setIdUnidad(actualBD.getIdUnidad());
        }
    }

    // ============================================================
    // OBTENER
    // ============================================================

    public Producto obtener(Integer idProducto) {

        if (idProducto == null || idProducto <= 0) {
            return null;
        }

        Optional<Producto> productoOpt = productoDao.buscarPorId(idProducto);
        return productoOpt.orElse(null);
    }

    // ============================================================
    // ELIMINAR / RESTAURAR
    // ============================================================

    public boolean eliminar(Integer idProducto) {

        if (idProducto == null || idProducto <= 0) {
            return false;
        }

        return productoDao.eliminar(idProducto) > 0;
    }

    public boolean restaurar(Integer idProducto) {

        if (idProducto == null || idProducto <= 0) {
            return false;
        }

        return productoDao.restaurar(idProducto) > 0;
    }

    // ============================================================
    // STOCK TEMPORAL
    // Luego esto debe pasar a InventarioService / inventario_stock.
    // ============================================================

    public void descontarStock(int idProducto, BigDecimal cantidad) {

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        productoDao.descontarStock(idProducto, cantidad);
    }

    public void aumentarStock(int idProducto, BigDecimal cantidad) {

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        productoDao.aumentarStock(idProducto, cantidad);
    }

    public void agregarStock(int idProducto, BigDecimal cantidad) {

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        productoDao.aumentarStock(idProducto, cantidad);
    }

    public BigDecimal obtenerStockActual(int idProducto) {

        Producto producto = obtener(idProducto);

        if (producto == null) {
            throw new IllegalArgumentException("No existe el producto con ID: " + idProducto);
        }

        if (producto.getStockActual() == null) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(producto.getStockActual().toString());
    }

    // ============================================================
    // CÓDIGO INTERNO
    // ============================================================

    public String generarCodigoInterno(Integer idCategoria) {
        return productoDao.generarCodigoInterno(idCategoria);
    }

    // ============================================================
    // ESTADO
    // ============================================================

    public boolean cambiarEstado(Integer idProducto, Integer nuevoEstado) {

        if (idProducto == null || idProducto <= 0 || nuevoEstado == null) {
            return false;
        }

        if (nuevoEstado != 1 && nuevoEstado != 2) {
            throw new IllegalArgumentException("Estado de producto no válido.");
        }

        return productoDao.cambiarEstado(idProducto, nuevoEstado) > 0;
    }

    // ============================================================
    // COMPRAS
    // ============================================================

    public BigDecimal obtenerUltimoPrecioCompra(int idProducto) {
        return productoDao.obtenerUltimoPrecioCompra(idProducto);
    }

    public BigDecimal obtenerCostoPromedioPorSede(Integer idProducto, Integer idSede) {
        if (idProducto == null || idProducto <= 0) {
            return BigDecimal.ZERO;
        }

        if (idSede == null || idSede <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal costo = productoDao.obtenerCostoPromedioPorSede(idProducto, idSede);

        return costo != null ? costo : BigDecimal.ZERO;
    }
}
