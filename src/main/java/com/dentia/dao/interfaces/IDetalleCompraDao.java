package com.dentia.dao.interfaces;

import com.dentia.model.interno.DetalleCompra;
import java.util.List;

public interface IDetalleCompraDao {
    int registrar(DetalleCompra detalle);
    List<DetalleCompra> listarPorCompra(int idCompra);
    boolean eliminarPorCompra(int idCompra);
}
