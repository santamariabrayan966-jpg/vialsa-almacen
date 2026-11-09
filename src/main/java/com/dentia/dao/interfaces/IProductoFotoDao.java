package com.dentia.dao.interfaces;

import com.dentia.model.interno.ProductoFoto;

import java.util.List;
import java.util.Optional;

public interface IProductoFotoDao {

    List<ProductoFoto> listarActivasPorProducto(Integer idProducto);

    int contarActivasPorProducto(Integer idProducto);

    int guardar(ProductoFoto foto);

    int desactivar(Integer idProductoFoto);

    int marcarPortada(Integer idProductoFoto);

    Optional<ProductoFoto> buscarPorId(Integer idProductoFoto);

    Optional<ProductoFoto> obtenerPortadaPorProducto(Integer idProducto);

    Optional<ProductoFoto> obtenerPrimeraActivaPorProducto(Integer idProducto);
}
