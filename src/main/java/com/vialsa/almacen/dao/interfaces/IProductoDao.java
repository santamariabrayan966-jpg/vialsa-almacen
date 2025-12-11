package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Producto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IProductoDao {

    List<Producto> listar();

    List<Producto> listarActivos();

    Optional<Producto> buscarPorId(Integer idProducto);

    int crear(Producto p);

    int actualizar(Producto p);

    int eliminar(Integer idProducto);

    void descontarStock(int idProducto, BigDecimal cantidad);

    void aumentarStock(int idProducto, BigDecimal cantidad);

    List<Producto> buscar(String filtro);

    String generarCodigoInterno(int idTipoProducto);

    int cambiarEstado(int idProducto, int nuevoEstado);

    // 👇 NECESARIO PARA VALIDAR EL PRECIO DE VENTA PROFESIONALMENTE
    BigDecimal obtenerUltimoPrecioCompra(int idProducto);
    List<Producto> buscarPorTexto(String texto);

}
