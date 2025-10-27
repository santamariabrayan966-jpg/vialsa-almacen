package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Producto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IProductoDao {
    List<Producto> listar();
    Optional<Producto> buscarPorId(Integer idProducto);
    int crear(Producto p);
    int actualizar(Producto p);
    int eliminar(Integer idProducto);
    void descontarStock(int idProducto, BigDecimal cantidad);
    void aumentarStock(int idProducto, BigDecimal cantidad);
}
