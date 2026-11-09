package com.dentia.dao.interfaces;

import com.dentia.model.interno.ProductoPresentacionSede;

import java.util.List;
import java.util.Set;

public interface IProductoPresentacionSedeDao {

    List<ProductoPresentacionSede> listarPorPresentacion(Integer idPrecioPresentacion);

    List<ProductoPresentacionSede> listarActivasPorPresentacion(Integer idPrecioPresentacion);

    List<ProductoPresentacionSede> listarActivasPorProducto(Integer idProducto);

    boolean estaActiva(Integer idPrecioPresentacion, Integer idSede);

    int habilitar(Integer idPrecioPresentacion, Integer idSede);

    int deshabilitar(Integer idPrecioPresentacion, Integer idSede);

    void sincronizarSedes(Integer idPrecioPresentacion, Set<Integer> idsSede);
}
