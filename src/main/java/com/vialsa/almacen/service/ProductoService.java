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

    // 💾 Guardar producto (crear o actualizar)
    public boolean guardar(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo.");
        }

        // Si el producto ya tiene ID, actualiza. Si no, crea uno nuevo.
        if (producto.getIdProducto() != null && producto.getIdProducto() > 0) {
            return productoDao.actualizar(producto) > 0;
        } else {
            return productoDao.crear(producto) > 0;
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

    // 📦 Actualizar stock (disminuir)
    public void descontarStock(int idProducto, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        productoDao.descontarStock(idProducto, cantidad);
    }

    // 📈 Actualizar stock (aumentar)
    public void aumentarStock(int idProducto, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        productoDao.aumentarStock(idProducto, cantidad);
    }
    public List<Producto> listarActivos() {
        return productoDao.listarActivos();
    }

}
