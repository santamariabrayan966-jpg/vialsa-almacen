package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.DetalleCompra;
import java.util.List;

public interface IDetalleCompraDao {
    int registrar(DetalleCompra detalle);
    List<DetalleCompra> listarPorCompra(int idCompra);
    boolean eliminarPorCompra(int idCompra);
}
