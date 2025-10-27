package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IProductoDao;
import com.vialsa.almacen.model.Producto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    private final IProductoDao productoDao;

    public ProductoService(IProductoDao productoDao) {
        this.productoDao = productoDao;
    }

    public List<Producto> listar() { return productoDao.listar(); }

    public Producto obtener(int id) {
        return productoDao.buscarPorId(id).orElse(null);
    }

    public boolean guardar(Producto p) {
        if (p.getIdProducto() == null) {
            return productoDao.crear(p) > 0;
        }
        return productoDao.actualizar(p) > 0;
    }

    public boolean eliminar(int id) {
        return productoDao.eliminar(id) > 0;
    }
}
