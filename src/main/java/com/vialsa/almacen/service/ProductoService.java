package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IProductoDao;
import com.vialsa.almacen.model.Producto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final IProductoDao productoDao;

    public ProductoService(IProductoDao productoDao) {
        this.productoDao = productoDao;
    }

    // 📋 Listar todos los productos
    public List<Producto> listar() {
        return productoDao.listar();
    }

    public List<Producto> listarActivos() {
        return productoDao.listarActivos();
    }

    // 🔍 Buscar por texto
    public List<Producto> buscar(String filtro) {
        return productoDao.buscar(filtro);
    }

    // 💾 Guardar producto (crear o actualizar)
    // Reglas:
    // - Tipo -> Unidad (Aluminio/Accesorio = Unidad, Vidrio = Plancha)
    // - Estado por defecto = Activo
    // - Stock mínimo por defecto = 20 si no se especifica
    // - StockActual NO se edita desde el formulario:
    //      * Nuevo producto: stockActual = 0
    //      * Existente: se mantiene el de BD
    // - Si ya tiene compras, precioUnitario (venta) NO puede ser < último precio de compra
    public boolean guardar(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo.");
        }

        // ==========================
        // Reglas de negocio básicas
        // ==========================

        // 1) Tipo -> Unidad
        // 1 = Aluminio  -> Unidad (2)
        // 2 = Vidrio    -> Plancha (4)
        // 3 = Accesorio -> Unidad (2)
        if (producto.getIdTipoProducto() == null) {
            throw new IllegalArgumentException("El tipo de producto es obligatorio.");
        } else {
            int tipo = producto.getIdTipoProducto();
            int idUnidad;
            if (tipo == 2) {                 // Vidrio
                idUnidad = 4;                // Plancha
            } else if (tipo == 1 || tipo == 3) { // Aluminio / Accesorio
                idUnidad = 2;                // Unidad
            } else {
                throw new IllegalArgumentException("Tipo de producto no soportado.");
            }
            producto.setIdUnidad(idUnidad);
        }

        // 2) Estado por defecto = Activo (1)
        if (producto.getIdEstadoProducto() == null) {
            producto.setIdEstadoProducto(1);
        }

        // 3) Stock mínimo por defecto (ejemplo: 20 unidades)
        if (producto.getStockMinimo() == null) {
            producto.setStockMinimo(20);
        }

        // ==========================
        // Control de precio de venta
        // ==========================
        // Si el producto ya existe y tiene compras, el precio de venta
        // no puede ser menor al último precio de compra.
        if (producto.getIdProducto() != null && producto.getIdProducto() > 0) {
            BigDecimal ultimoPrecioCompra = productoDao.obtenerUltimoPrecioCompra(producto.getIdProducto());
            if (ultimoPrecioCompra != null && producto.getPrecioUnitario() != null) {
                if (producto.getPrecioUnitario().compareTo(ultimoPrecioCompra) < 0) {
                    throw new IllegalArgumentException(
                            "El precio de venta no puede ser menor al último precio de compra (S/ "
                                    + ultimoPrecioCompra + ")."
                    );
                }
            }
        }

        // ==========================
        // Control de stock
        // ==========================
        // Regla profesional:
        // - Nuevo: stockActual = 0.
        // - Existente: mantener el stockActual que está en BD.

        if (producto.getIdProducto() == null || producto.getIdProducto() == 0) {
            // Producto nuevo
            producto.setStockActual(0);

            // Generar código interno si no viene
            if (producto.getCodigoInterno() == null || producto.getCodigoInterno().isBlank()) {
                producto.setCodigoInterno(
                        productoDao.generarCodigoInterno(producto.getIdTipoProducto())
                );
            }

            return productoDao.crear(producto) > 0;

        } else {
            // Producto existente -> mantener datos de BD
            Optional<Producto> optActual = productoDao.buscarPorId(producto.getIdProducto());
            if (optActual.isEmpty()) {
                throw new IllegalArgumentException("El producto a actualizar no existe.");
            }

            Producto actualBD = optActual.get();

            // Mantener stock actual de BD
            producto.setStockActual(actualBD.getStockActual());

            // ⬅️ MUY IMPORTANTE:
            // Si NO se subió una nueva imagen, conservar la foto actual
            if (producto.getFoto() == null || producto.getFoto().isBlank()) {
                producto.setFoto(actualBD.getFoto());
            }

            return productoDao.actualizar(producto) > 0;
        }

    }

    // 🔍 Buscar producto por ID
    public Producto obtener(Integer idProducto) {
        if (idProducto == null || idProducto <= 0) {
            return null;
        }
        Optional<Producto> productoOpt = productoDao.buscarPorId(idProducto);
        return productoOpt.orElse(null);
    }

    // ❌ Eliminar producto
    public boolean eliminar(Integer idProducto) {
        if (idProducto == null || idProducto <= 0) {
            return false;
        }
        return productoDao.eliminar(idProducto) > 0;
    }

    // 📦 Actualizar stock (disminuir) — se usará en VENTAS
    public void descontarStock(int idProducto, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        productoDao.descontarStock(idProducto, cantidad);
    }

    // 📈 Actualizar stock (aumentar) — se usará en COMPRAS
    public void aumentarStock(int idProducto, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        productoDao.aumentarStock(idProducto, cantidad);
    }

    // =============================================================
// ⭐ NUEVOS MÉTODOS QUE FALTABAN ⭐
// =============================================================

    // Obtener el stock actual de un producto
    public BigDecimal obtenerStockActual(int idProducto) {
        Producto p = obtener(idProducto);

        if (p == null) {
            throw new IllegalArgumentException("No existe el producto con ID: " + idProducto);
        }

        // Convertir StockActual a BigDecimal siempre
        Number stock = p.getStockActual(); // Puede ser Integer, Double, BigDecimal

        if (stock == null) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(stock.toString());
    }


    // Aumentar stock (alias agregarStock) — requerido por VentaService
    public void agregarStock(int idProducto, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        productoDao.aumentarStock(idProducto, cantidad);  // ya existe en tu DAO
    }

    public String generarCodigoInterno(int idTipoProducto) {
        return productoDao.generarCodigoInterno(idTipoProducto);
    }

    // 🔁 Cambiar estado (1 = Activo, 2 = Inactivo)
    public boolean cambiarEstado(Integer idProducto, Integer nuevoEstado) {
        if (idProducto == null || idProducto <= 0 || nuevoEstado == null) {
            return false;
        }
        return productoDao.cambiarEstado(idProducto, nuevoEstado) > 0;
    }
    public BigDecimal obtenerUltimoPrecioCompra(int idProducto) {
        return productoDao.obtenerUltimoPrecioCompra(idProducto);
    }



}
