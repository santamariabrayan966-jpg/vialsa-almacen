package com.dentia.dao.interfaces;

import com.dentia.model.interno.ProductoPresentacionOferta;

import java.util.Optional;

public interface IProductoPresentacionOfertaDao {

    Optional<ProductoPresentacionOferta> buscarActivaPorPresentacion(Integer idPrecioPresentacion);

    int crear(ProductoPresentacionOferta oferta);

    int actualizar(ProductoPresentacionOferta oferta);

    int desactivarActivasPorPresentacion(Integer idPrecioPresentacion);
}
