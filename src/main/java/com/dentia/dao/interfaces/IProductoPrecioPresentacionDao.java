package com.dentia.dao.interfaces;

import com.dentia.model.interno.ProductoPrecioPresentacion;

import java.util.List;
import java.util.Optional;

public interface IProductoPrecioPresentacionDao {

    List<ProductoPrecioPresentacion> listarPorProducto(Integer idProducto);

    List<ProductoPrecioPresentacion> listarPorProductoYSede(Integer idProducto, Integer idSede);

    List<ProductoPrecioPresentacion> listarActivasPorProducto(Integer idProducto);

    List<ProductoPrecioPresentacion> listarActivasPorProductoYSede(Integer idProducto, Integer idSede);

    Optional<ProductoPrecioPresentacion> buscarPorId(Integer idPrecioPresentacion);

    int crear(ProductoPrecioPresentacion precio);

    int actualizar(ProductoPrecioPresentacion precio);

    int cambiarEstado(Integer idPrecioPresentacion, Integer estado);

    int marcarPrecioPortada(Integer idPrecioPresentacion);

    boolean existePresentacionEnSede(
            Integer idProducto,
            Integer idSede,
            String nombrePresentacion,
            Integer idPrecioPresentacionExcluir
    );
}
